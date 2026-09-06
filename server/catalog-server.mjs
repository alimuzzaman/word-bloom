import { createHash } from "node:crypto";
import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { fileURLToPath, pathToFileURL } from "node:url";
import { dirname, resolve } from "node:path";

const SERVER_DIR = dirname(fileURLToPath(import.meta.url));
export const DEFAULT_CATALOG_PATH = resolve(
  SERVER_DIR,
  "../app/src/main/assets/categories.json",
);
// Schema 1 is kept as a read-only migration path while existing APKs still
// carry the original asset. New remote catalogs should publish schema 2.
export const SUPPORTED_SCHEMA_VERSIONS = new Set([1, 2]);
export const SUPPORTED_SCHEMA_VERSION = 2;
export const DEFAULT_HOST = "127.0.0.1";
export const DEFAULT_PORT = 8787;
export const MAX_CATALOG_BYTES = 5 * 1024 * 1024;
const CATALOG_PATH = "/v1/catalog";
const CATEGORIES_PATH = "/v1/categories";
const HEALTH_PATH = "/healthz";
const CACHE_CONTROL = "public, max-age=300, stale-while-revalidate=86400";

/**
 * Return a deterministic JSON representation for semantic catalog versioning.
 * Object keys are sorted, while arrays retain their authored order.
 */
export function canonicalJson(value) {
  if (Array.isArray(value)) {
    return `[${value.map(canonicalJson).join(",")}]`;
  }
  if (value !== null && typeof value === "object") {
    return `{${Object.keys(value)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${canonicalJson(value[key])}`)
      .join(",")}}`;
  }
  return JSON.stringify(value);
}

export function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function requireString(value, path, { max = 4_000 } = {}) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`${path} must be a non-empty string`);
  }
  if (value.length > max) {
    throw new Error(`${path} exceeds ${max} characters`);
  }
  if (value.includes("\u0000")) {
    throw new Error(`${path} must not contain NUL`);
  }
}

function requirePositiveInteger(value, path) {
  if (!Number.isInteger(value) || value < 1) {
    throw new Error(`${path} must be a positive integer`);
  }
}

function requireOptionalTextFields(value, path) {
  if (!isPlainObject(value)) {
    throw new Error(`${path} must be an object`);
  }
  for (const [key, field] of Object.entries(value)) {
    if (/^(?:introduction|context|completion|story|sentence|definition)/i.test(key)) {
      requireString(field, `${path}.${key}`);
    }
  }
}

function containsWholeWord(text, word) {
  // Target words are ASCII A-Z. Restricting the lookarounds to ASCII letters
  // prevents a short target (for example CAT) from matching CATCH while still
  // allowing punctuation, whitespace, and sentence boundaries around it.
  const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`(?<![A-Za-z])${escaped}(?![A-Za-z])`, "i").test(text);
}

function validateOptionalArray(value, path, { maxItems = 128, itemMax = 64 } = {}) {
  if (!Array.isArray(value) || value.length > maxItems) {
    throw new Error(`${path} must be an array with at most ${maxItems} items`);
  }
  for (const [index, item] of value.entries()) {
    requireString(item, `${path}[${index}]`, { max: itemMax });
  }
}

function canSpell(word, letters) {
  const available = new Map();
  for (const letter of letters) {
    available.set(letter, (available.get(letter) ?? 0) + 1);
  }
  for (const letter of word) {
    const count = available.get(letter) ?? 0;
    if (count === 0) return false;
    available.set(letter, count - 1);
  }
  return true;
}

function placementCells(placement) {
  const vertical = placement.direction === "VERTICAL";
  return [...placement.word].map((letter, index) => ({
    row: placement.row + (vertical ? index : 0),
    col: placement.col + (vertical ? 0 : index),
    letter,
  }));
}

function validateLevel(level, category, schemaVersion, index = "?") {
  const path = `categories[${category.id}].levels[${index}]`;
  if (!isPlainObject(level)) throw new Error(`${path} must be an object`);
  requirePositiveInteger(level.id, `${path}.id`);
  requirePositiveInteger(level.order, `${path}.order`);
  requirePositiveInteger(level.difficulty, `${path}.difficulty`);
  if (level.difficulty > 5) throw new Error(`${path}.difficulty must be 1..5`);
  if (level.categoryId !== category.id) {
    throw new Error(`${path}.categoryId must match ${category.id}`);
  }
  requireString(level.letters, `${path}.letters`, { max: 32 });
  if (!/^[A-Z]+$/.test(level.letters)) {
    throw new Error(`${path}.letters must contain uppercase A-Z only`);
  }
  if (!Array.isArray(level.words) || level.words.length === 0) {
    throw new Error(`${path}.words must be a non-empty array`);
  }
  const words = new Set();
  for (const [index, word] of level.words.entries()) {
    requireString(word, `${path}.words[${index}]`, { max: 32 });
    if (!/^[A-Z]+$/.test(word)) {
      throw new Error(`${path}.words[${index}] must contain uppercase A-Z only`);
    }
    if (words.has(word)) throw new Error(`${path}.words contains duplicate ${word}`);
    if (!canSpell(word, level.letters)) {
      throw new Error(`${path}.words[${index}] cannot be spelled from letters`);
    }
    words.add(word);
  }
  if (!Array.isArray(level.bonusWords)) {
    throw new Error(`${path}.bonusWords must be an array`);
  }
  for (const [index, word] of level.bonusWords.entries()) {
    requireString(word, `${path}.bonusWords[${index}]`, { max: 32 });
    if (!/^[A-Z]+$/.test(word) || !canSpell(word, level.letters)) {
      throw new Error(`${path}.bonusWords[${index}] cannot be spelled from letters`);
    }
  }
  requirePositiveInteger(level.rows, `${path}.rows`);
  requirePositiveInteger(level.cols, `${path}.cols`);
  if (level.rows > 64 || level.cols > 64) {
    throw new Error(`${path} grid is larger than 64x64`);
  }
  if (!Array.isArray(level.placements) || level.placements.length !== words.size) {
    throw new Error(`${path}.placements must contain one placement per target word`);
  }

  const occupied = new Map();
  const placedWords = new Set();
  for (const [index, placement] of level.placements.entries()) {
    const placementPath = `${path}.placements[${index}]`;
    if (!isPlainObject(placement)) throw new Error(`${placementPath} must be an object`);
    requireString(placement.word, `${placementPath}.word`, { max: 32 });
    if (!words.has(placement.word) || placedWords.has(placement.word)) {
      throw new Error(`${placementPath}.word must name each target exactly once`);
    }
    if (!Number.isInteger(placement.row) || placement.row < 0) {
      throw new Error(`${placementPath}.row must be a zero-based coordinate`);
    }
    if (!Number.isInteger(placement.col) || placement.col < 0) {
      throw new Error(`${placementPath}.col must be a zero-based coordinate`);
    }
    if (placement.direction !== "HORIZONTAL" && placement.direction !== "VERTICAL") {
      throw new Error(`${placementPath}.direction must be HORIZONTAL or VERTICAL`);
    }
    for (const cell of placementCells(placement)) {
      if (cell.row >= level.rows || cell.col >= level.cols) {
        throw new Error(`${placementPath} extends outside its grid`);
      }
      const key = `${cell.row}:${cell.col}`;
      const prior = occupied.get(key);
      if (prior && prior !== cell.letter) {
        throw new Error(`${placementPath} conflicts at ${key}`);
      }
      occupied.set(key, cell.letter);
    }
    placedWords.add(placement.word);
  }
  if (placedWords.size !== words.size) throw new Error(`${path} is missing a placement`);

  if (level.heroWord !== undefined && level.heroWord !== null) {
    if (!isPlainObject(level.heroWord)) throw new Error(`${path}.heroWord must be an object`);
    for (const field of ["word", "definitionEn", "translationBn", "meaningBn"]) {
      requireString(level.heroWord[field], `${path}.heroWord.${field}`);
    }
    if (!words.has(level.heroWord.word)) {
      throw new Error(`${path}.heroWord.word must be one of the target words`);
    }
  } else {
    throw new Error(`${path}.heroWord must have bilingual metadata`);
  }
  if (level.wordMeanings !== undefined) {
    if (!Array.isArray(level.wordMeanings)) {
      throw new Error(`${path}.wordMeanings must be an array`);
    }
    const meaningWords = new Set();
    for (const [index, meaning] of level.wordMeanings.entries()) {
      const meaningPath = `${path}.wordMeanings[${index}]`;
      if (!isPlainObject(meaning)) throw new Error(`${meaningPath} must be an object`);
      requireString(meaning.word, `${meaningPath}.word`, { max: 32 });
      if (!words.has(meaning.word) && !level.bonusWords.includes(meaning.word)) {
        throw new Error(`${meaningPath}.word is not a target or bonus word`);
      }
      if (meaningWords.has(meaning.word)) {
        throw new Error(`${path}.wordMeanings contains duplicate ${meaning.word}`);
      }
      meaningWords.add(meaning.word);
      for (const field of ["definitionEn", "translationBn", "meaningBn"]) {
        requireString(meaning[field], `${meaningPath}.${field}`);
      }
    }
    if (schemaVersion >= 2 &&
        (meaningWords.size !== words.size || [...words].some((word) => !meaningWords.has(word)))) {
      throw new Error(`${path}.wordMeanings must cover target words exactly`);
    }
  } else if (schemaVersion >= 2) {
    throw new Error(`${path}.wordMeanings must be present for schema 2`);
  }
  if (level.lesson !== undefined) {
    if (!isPlainObject(level.lesson)) throw new Error(`${path}.lesson must be an object`);
    requireString(level.lesson.sentenceEn, `${path}.lesson.sentenceEn`);
    requireString(level.lesson.sentenceBn, `${path}.lesson.sentenceBn`);
    if (level.lesson.usedWords !== undefined) {
      validateOptionalArray(level.lesson.usedWords, `${path}.lesson.usedWords`);
      for (const word of level.lesson.usedWords) {
        if (!words.has(word)) throw new Error(`${path}.lesson.usedWords contains unknown word ${word}`);
      }
    }
    if (schemaVersion >= 2 &&
        (!Array.isArray(level.lesson.usedWords) || level.lesson.usedWords.length === 0)) {
      throw new Error(`${path}.lesson.usedWords must contain every target word`);
    }
    if (schemaVersion >= 2) {
      const usedWords = new Set(level.lesson.usedWords);
      if (usedWords.size !== words.size || [...words].some((word) => !usedWords.has(word))) {
        throw new Error(`${path}.lesson.usedWords must cover every target word exactly once`);
      }
      for (const word of words) {
        if (!containsWholeWord(level.lesson.sentenceEn, word)) {
          throw new Error(`${path}.lesson.sentenceEn must contain target word ${word} as a whole word`);
        }
      }
    }
  } else if (schemaVersion >= 2) {
    throw new Error(`${path}.lesson must be present for schema 2`);
  }
  if (level.ageBand !== undefined) requireString(level.ageBand, `${path}.ageBand`, { max: 32 });
  if (schemaVersion >= 2 && !level.ageBand) {
    throw new Error(`${path}.ageBand must be present for schema 2`);
  }
}

/**
 * Validate the public catalog shape before it is made reachable over HTTP.
 * This is strict about executable/grid fields and the schema-2 learning contract;
 * unrelated additional metadata remains forward-compatible.
 */
export function validateCatalog(catalog) {
  if (!isPlainObject(catalog)) throw new Error("catalog must be an object");
  const schemaVersion = catalog.schemaVersion;
  if (!SUPPORTED_SCHEMA_VERSIONS.has(schemaVersion)) {
    throw new Error(`unsupported schemaVersion ${schemaVersion}`);
  }
  if (!Array.isArray(catalog.categories) || catalog.categories.length === 0) {
    throw new Error("catalog.categories must be a non-empty array");
  }
  const categoryIds = new Set();
  const categoryOrders = new Set();
  for (const [index, category] of catalog.categories.entries()) {
    const path = `categories[${index}]`;
    if (!isPlainObject(category)) throw new Error(`${path} must be an object`);
    requireString(category.id, `${path}.id`, { max: 64 });
    if (!/^[a-z0-9]+(?:[-_][a-z0-9]+)*$/.test(category.id)) {
      throw new Error(`${path}.id must be a lowercase slug`);
    }
    if (categoryIds.has(category.id)) throw new Error(`duplicate category id ${category.id}`);
    categoryIds.add(category.id);
    requireString(category.nameEn, `${path}.nameEn`, { max: 120 });
    requireString(category.nameBn, `${path}.nameBn`, { max: 120 });
    requireString(category.iconKey, `${path}.iconKey`, { max: 64 });
    requirePositiveInteger(category.order, `${path}.order`);
    if (categoryOrders.has(category.order)) throw new Error(`duplicate category order ${category.order}`);
    categoryOrders.add(category.order);
    if (!Array.isArray(category.levels) || category.levels.length === 0) {
      throw new Error(`${path}.levels must be a non-empty array`);
    }
    const levelIds = new Set();
    const levelOrders = new Set();
    for (const [levelIndex, level] of category.levels.entries()) {
      validateLevel(level, category, schemaVersion, levelIndex);
      if (levelIds.has(level.id)) throw new Error(`duplicate level id in ${category.id}`);
      if (levelOrders.has(level.order)) throw new Error(`duplicate level order in ${category.id}`);
      levelIds.add(level.id);
      levelOrders.add(level.order);
    }
    const sortedOrders = [...levelOrders].sort((a, b) => a - b);
    for (let i = 0; i < sortedOrders.length; i += 1) {
      if (sortedOrders[i] !== i + 1) {
        throw new Error(`level order in ${category.id} must be contiguous from 1`);
      }
    }
    for (const [key, value] of Object.entries(category)) {
      if (/^(?:introduction|context|completion)/i.test(key) && typeof value === "string") {
        requireString(value, `category ${category.id}.${key}`);
      }
    }
    if (schemaVersion >= 2) {
      for (const field of [
        "introductionEn", "introductionBn", "completionEn", "completionBn", "storyEn", "storyBn",
      ]) {
        const max = field.startsWith("story") ? 16_000 : 4_000;
        requireString(category[field], `category ${category.id}.${field}`, { max });
      }
      requireString(category.contentReviewStatus, `category ${category.id}.contentReviewStatus`, { max: 32 });
      validateOptionalArray(category.completionWords, `category ${category.id}.completionWords`);
      const heroWords = new Set(category.levels.map((level) => level.heroWord?.word?.toUpperCase()));
      const completionWords = new Set(category.completionWords.map((word) => word.toUpperCase()));
      if (completionWords.size === 0 || completionWords.size !== category.completionWords.length ||
          [...completionWords].some((word) => !heroWords.has(word))) {
        throw new Error(`category ${category.id}.completionWords must be a unique non-empty subset of hero words`);
      }

      // A category story is the long-form reinforcement for the complete
      // category. It must explicitly declare, and mention as whole English
      // words, every unique target answer from every level.
      validateOptionalArray(category.storyWords, `category ${category.id}.storyWords`, {
        maxItems: 1_024,
        itemMax: 64,
      });
      const storyWords = new Set();
      for (const [storyIndex, word] of category.storyWords.entries()) {
        if (!/^[A-Z]+$/.test(word)) {
          throw new Error(`category ${category.id}.storyWords[${storyIndex}] must contain uppercase A-Z only`);
        }
        if (storyWords.has(word)) {
          throw new Error(`category ${category.id}.storyWords contains duplicate ${word}`);
        }
        storyWords.add(word);
      }
      const targetWords = new Set(category.levels.flatMap((level) => level.words));
      if (storyWords.size !== targetWords.size || [...targetWords].some((word) => !storyWords.has(word))) {
        throw new Error(`category ${category.id}.storyWords must cover every target word exactly once`);
      }
      for (const word of targetWords) {
        if (!containsWholeWord(category.storyEn, word)) {
          throw new Error(`category ${category.id}.storyEn must contain target word ${word} as a whole word`);
        }
      }
    }
  }
  if (catalog.catalogVersion !== undefined) {
    requireString(catalog.catalogVersion, "catalog.catalogVersion", { max: 128 });
  }
  if (catalog.schemaVersion === 2 && catalog.catalogVersion === undefined) {
    throw new Error("schemaVersion 2 catalogs must include catalogVersion");
  }
  if (catalog.contentReviewNotice !== undefined) {
    requireString(catalog.contentReviewNotice, "catalog.contentReviewNotice");
  }
  return catalog;
}

export async function loadCatalog(catalogPath = DEFAULT_CATALOG_PATH) {
  const absolutePath = resolve(catalogPath);
  const metadata = await stat(absolutePath);
  if (!metadata.isFile()) throw new Error(`catalog path is not a regular file: ${absolutePath}`);
  if (metadata.size > MAX_CATALOG_BYTES) {
    throw new Error(`catalog exceeds ${MAX_CATALOG_BYTES} bytes: ${absolutePath}`);
  }
  let parsed;
  try {
    parsed = JSON.parse(await readFile(absolutePath, "utf8"));
  } catch (error) {
    throw new Error(`invalid catalog JSON at ${absolutePath}: ${error.message}`);
  }
  return validateCatalog(parsed);
}

function makeSnapshot(catalog) {
  // A server-provided version must not hash itself. Ignore an authored value so
  // the same catalog content always gets the same version.
  const authoredVersion = catalog.catalogVersion;
  const source = { ...catalog };
  delete source.catalogVersion;
  const catalogVersion = authoredVersion || `sha256-${sha256(canonicalJson(source))}`;
  const responseCatalog = { ...source, catalogVersion };
  const body = Buffer.from(`${JSON.stringify(responseCatalog)}\n`, "utf8");
  const etag = `"${sha256(body)}"`;
  return { catalog: responseCatalog, body, catalogVersion, etag };
}

function etagMatches(header, etag) {
  if (!header) return false;
  return header
    .split(",")
    .map((token) => token.trim())
    .some((token) => token === "*" || token.replace(/^W\//, "") === etag);
}

function writeJson(response, status, payload, headers = {}) {
  const body = Buffer.from(`${JSON.stringify(payload)}\n`, "utf8");
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": body.byteLength,
    ...headers,
  });
  response.end(body);
}

function applyCors(response, corsOrigin) {
  if (!corsOrigin) return;
  response.setHeader("Access-Control-Allow-Origin", corsOrigin);
  response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
  response.setHeader("Access-Control-Allow-Headers", "Accept, If-None-Match");
  response.setHeader("Vary", "Origin");
}

function safeCorsOrigin(value) {
  if (!value) return "";
  if (value === "*") return "*";
  if (!/^https?:\/\/[^\s/]+(?::\d+)?$/.test(value)) {
    throw new Error("CORS_ORIGIN must be * or one http(s) origin");
  }
  return value;
}

/**
 * Build a server around an already validated catalog. Keeping construction pure
 * makes contract tests cheap and prevents a request from changing the catalog.
 */
export function createCatalogServer(catalog, { corsOrigin = "" } = {}) {
  validateCatalog(catalog);
  const snapshot = makeSnapshot(catalog);
  const categorySnapshots = new Map(
    snapshot.catalog.categories.map((category) => {
      const body = Buffer.from(`${JSON.stringify({
        schemaVersion: snapshot.catalog.schemaVersion,
        catalogVersion: snapshot.catalogVersion,
        category,
      })}\n`, "utf8");
      return [category.id, { body, etag: `"${sha256(body)}"` }];
    }),
  );
  const allowedCorsOrigin = safeCorsOrigin(corsOrigin);

  const server = createServer((request, response) => {
    applyCors(response, allowedCorsOrigin);
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Cache-Control", CACHE_CONTROL);
    response.setHeader("ETag", snapshot.etag);
    response.setHeader("X-Catalog-Version", snapshot.catalogVersion);

    if (request.method === "OPTIONS") {
      response.writeHead(204, { Allow: "GET, HEAD, OPTIONS" });
      response.end();
      return;
    }
    if (request.method !== "GET" && request.method !== "HEAD") {
      writeJson(response, 405, { error: "method_not_allowed" }, { Allow: "GET, HEAD, OPTIONS" });
      return;
    }

    let pathname;
    try {
      pathname = new URL(request.url ?? "/", "http://localhost").pathname;
    } catch {
      writeJson(response, 400, { error: "invalid_url" });
      return;
    }

    if (pathname === HEALTH_PATH) {
      const payload = {
        status: "ok",
        schemaVersion: snapshot.catalog.schemaVersion,
        catalogVersion: snapshot.catalogVersion,
      };
      if (request.method === "HEAD") {
        response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
        response.end();
      } else {
        writeJson(response, 200, payload);
      }
      return;
    }

    if (pathname === CATALOG_PATH) {
      if (etagMatches(request.headers["if-none-match"], snapshot.etag)) {
        response.writeHead(304);
        response.end();
      } else if (request.method === "HEAD") {
        response.writeHead(200, {
          "Content-Type": "application/json; charset=utf-8",
          "Content-Length": snapshot.body.byteLength,
        });
        response.end();
      } else {
        response.writeHead(200, {
          "Content-Type": "application/json; charset=utf-8",
          "Content-Length": snapshot.body.byteLength,
        });
        response.end(snapshot.body);
      }
      return;
    }

    if (pathname === CATEGORIES_PATH || pathname.startsWith(`${CATEGORIES_PATH}/`)) {
      const categoryId = pathname.slice(`${CATEGORIES_PATH}/`.length);
      if (!categoryId) {
        writeJson(response, 400, { error: "category_id_required" });
        return;
      }
      let decodedId;
      try {
        decodedId = decodeURIComponent(categoryId);
      } catch {
        writeJson(response, 400, { error: "invalid_category_id" });
        return;
      }
      const category = snapshot.catalog.categories.find((item) => item.id === decodedId);
      if (!category) {
        writeJson(response, 404, { error: "category_not_found" });
        return;
      }
      const categorySnapshot = categorySnapshots.get(category.id);
      response.setHeader("ETag", categorySnapshot.etag);
      if (etagMatches(request.headers["if-none-match"], categorySnapshot.etag)) {
        response.writeHead(304);
      } else {
        response.writeHead(200, {
          "Content-Type": "application/json; charset=utf-8",
          "Content-Length": categorySnapshot.body.byteLength,
          "ETag": categorySnapshot.etag,
        });
        if (request.method === "GET") response.end(categorySnapshot.body);
        else response.end();
        return;
      }
      response.end();
      return;
    }

    writeJson(response, 404, { error: "not_found" });
  });

  return {
    server,
    catalog: snapshot.catalog,
    catalogVersion: snapshot.catalogVersion,
    etag: snapshot.etag,
  };
}

function envPort(value) {
  if (value === undefined || value === "") return DEFAULT_PORT;
  const port = Number(value);
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("PORT must be an integer from 1 to 65535");
  }
  return port;
}

async function main() {
  const catalog = await loadCatalog(process.env.CATALOG_PATH || DEFAULT_CATALOG_PATH);
  const { server, catalogVersion } = createCatalogServer(catalog, {
    corsOrigin: process.env.CORS_ORIGIN || "",
  });
  const host = process.env.HOST || DEFAULT_HOST;
  const port = envPort(process.env.PORT);
  server.listen(port, host, () => {
    const address = server.address();
    const actualPort = typeof address === "object" && address ? address.port : port;
    console.log(`Word Bloom catalog API listening on http://${host}:${actualPort}`);
    console.log(`catalogVersion=${catalogVersion}`);
  });
  const shutdown = () => server.close(() => process.exit(0));
  process.once("SIGINT", shutdown);
  process.once("SIGTERM", shutdown);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch((error) => {
    console.error(`catalog server failed: ${error.message}`);
    process.exitCode = 1;
  });
}
