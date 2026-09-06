import assert from "node:assert/strict";
import { once } from "node:events";
import test from "node:test";

import {
  DEFAULT_CATALOG_PATH,
  createCatalogServer,
  loadCatalog,
  validateCatalog,
} from "./catalog-server.mjs";

async function withServer(options, callback) {
  const catalog = await loadCatalog(DEFAULT_CATALOG_PATH);
  const { server, ...metadata } = createCatalogServer(catalog, options);
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const address = server.address();
  const baseUrl = `http://127.0.0.1:${address.port}`;
  try {
    return await callback(baseUrl, metadata);
  } finally {
    server.close();
    await once(server, "close");
  }
}

test("GET /v1/catalog returns a versioned catalog with an ETag", async () => {
  await withServer({}, async (baseUrl, metadata) => {
    const response = await fetch(`${baseUrl}/v1/catalog`);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get("content-type"), "application/json; charset=utf-8");
    assert.equal(response.headers.get("etag"), metadata.etag);
    assert.equal(response.headers.get("x-catalog-version"), metadata.catalogVersion);
    assert.match(response.headers.get("cache-control"), /max-age=300/);
    const payload = await response.json();
    assert.equal(payload.schemaVersion, metadata.catalog.schemaVersion);
    assert.equal(payload.catalogVersion, metadata.catalogVersion);
    assert.equal(payload.categories.length, 16);
  });
});

test("If-None-Match returns 304 without a response body", async () => {
  await withServer({}, async (baseUrl, metadata) => {
    const response = await fetch(`${baseUrl}/v1/catalog`, {
      headers: { "If-None-Match": metadata.etag },
    });
    assert.equal(response.status, 304);
    assert.equal(await response.text(), "");
    assert.equal(response.headers.get("etag"), metadata.etag);
  });
});

test("category and health routes are read-only and bounded", async () => {
  await withServer({}, async (baseUrl, metadata) => {
    const category = await fetch(`${baseUrl}/v1/categories/fish`);
    assert.equal(category.status, 200);
    const categoryPayload = await category.json();
    assert.equal(categoryPayload.category.id, "fish");
    assert.equal(categoryPayload.catalogVersion, metadata.catalogVersion);
    const categoryEtag = category.headers.get("etag");
    assert.ok(categoryEtag);
    const categoryNotModified = await fetch(`${baseUrl}/v1/categories/fish`, {
      headers: { "If-None-Match": categoryEtag },
    });
    assert.equal(categoryNotModified.status, 304);

    const health = await fetch(`${baseUrl}/healthz`);
    assert.equal(health.status, 200);
    assert.deepEqual(await health.json(), {
      status: "ok",
      schemaVersion: metadata.catalog.schemaVersion,
      catalogVersion: metadata.catalogVersion,
    });

    const missing = await fetch(`${baseUrl}/v1/categories/does-not-exist`);
    assert.equal(missing.status, 404);
    assert.deepEqual(await missing.json(), { error: "category_not_found" });

    const mutation = await fetch(`${baseUrl}/v1/catalog`, { method: "POST" });
    assert.equal(mutation.status, 405);
    assert.equal(mutation.headers.get("allow"), "GET, HEAD, OPTIONS");
  });
});

test("OPTIONS is available only for an explicitly configured CORS origin", async () => {
  await withServer({ corsOrigin: "https://example.test" }, async (baseUrl) => {
    const response = await fetch(`${baseUrl}/v1/catalog`, {
      method: "OPTIONS",
      headers: { Origin: "https://example.test" },
    });
    assert.equal(response.status, 204);
    assert.equal(response.headers.get("access-control-allow-origin"), "https://example.test");
    assert.equal(response.headers.get("access-control-allow-methods"), "GET, HEAD, OPTIONS");
  });
});

test("catalog validation rejects incompatible schemas and malformed grids", () => {
  assert.throws(
    () => validateCatalog({ schemaVersion: 3, categories: [] }),
    /unsupported schemaVersion/,
  );
  assert.throws(
    () => validateCatalog({
      schemaVersion: 1,
      categories: [{
        id: "bad",
        nameEn: "Bad",
        nameBn: "খারাপ",
        iconKey: "x",
        order: 1,
        levels: [{
          id: 1,
          categoryId: "bad",
          order: 1,
          difficulty: 1,
          letters: "CAT",
          words: ["CAR"],
          bonusWords: [],
          rows: 2,
          cols: 2,
          placements: [{ word: "CAR", row: 0, col: 0, direction: "HORIZONTAL" }],
        }],
      }],
    }),
    /cannot be spelled from letters/,
  );
});

test("schema 2 accepts the reviewed wordMeanings list and authored version", () => {
  const catalog = {
    schemaVersion: 2,
    catalogVersion: "2026.09.02-1",
    contentReviewNotice: "Draft copy requires educator review.",
    categories: [{
      id: "fish",
      nameEn: "Fish",
      nameBn: "মাছ",
      iconKey: "fish",
      order: 1,
      introductionEn: "Fish live in water.",
      introductionBn: "মাছ পানিতে বাস করে।",
      completionEn: "Fish are useful.",
      completionBn: "মাছ উপকারী।",
      completionWords: ["COD"],
      storyEn: "The cod swims near the fish.",
      storyBn: "কড মাছটির কাছে সাঁতার কাটে।",
      storyWords: ["COD"],
      contentReviewStatus: "approved",
      levels: [{
        id: 1,
        categoryId: "fish",
        order: 1,
        difficulty: 1,
        ageBand: "6-7",
        heroWord: {
          word: "COD",
          definitionEn: "A sea fish.",
          translationBn: "কড মাছ",
          meaningBn: "এক ধরনের সামুদ্রিক মাছ।",
        },
        wordMeanings: [{
          word: "COD",
          definitionEn: "A sea fish.",
          translationBn: "কড মাছ",
          meaningBn: "এক ধরনের সামুদ্রিক মাছ।",
        }],
        lesson: {
          sentenceEn: "The cod swims in the sea.",
          sentenceBn: "কড মাছ সাগরে সাঁতার কাটে।",
          usedWords: ["COD"],
        },
        letters: "COD",
        words: ["COD"],
        bonusWords: [],
        rows: 1,
        cols: 3,
        placements: [{ word: "COD", row: 0, col: 0, direction: "HORIZONTAL" }],
      }],
    }],
  };

  const result = createCatalogServer(catalog);
  assert.equal(result.catalog.catalogVersion, "2026.09.02-1");
  assert.equal(result.catalog.categories[0].levels[0].wordMeanings[0].word, "COD");
});

test("schema 2 rejects stories that omit, substring, or misdeclare target words", () => {
  const catalog = {
    schemaVersion: 2,
    catalogVersion: "2026.09.02-1",
    contentReviewNotice: "Approved sample",
    categories: [{
      id: "fish",
      nameEn: "Fish",
      nameBn: "মাছ",
      iconKey: "fish",
      order: 1,
      introductionEn: "Fish live in water.",
      introductionBn: "মাছ পানিতে বাস করে।",
      completionEn: "Fish are useful.",
      completionBn: "মাছ উপকারী।",
      completionWords: ["COD"],
      storyEn: "The cod swims near the fish.",
      storyBn: "কড মাছটির কাছে সাঁতার কাটে।",
      storyWords: ["COD"],
      contentReviewStatus: "approved",
      levels: [{
        id: 1,
        categoryId: "fish",
        order: 1,
        difficulty: 1,
        ageBand: "6-7",
        heroWord: {
          word: "COD",
          definitionEn: "A sea fish.",
          translationBn: "কড মাছ",
          meaningBn: "এক ধরনের সামুদ্রিক মাছ।",
        },
        wordMeanings: [{
          word: "COD",
          definitionEn: "A sea fish.",
          translationBn: "কড মাছ",
          meaningBn: "এক ধরনের সামুদ্রিক মাছ।",
        }],
        lesson: {
          sentenceEn: "The cod swims.",
          sentenceBn: "কড মাছ সাঁতার কাটে।",
          usedWords: ["COD"],
        },
        letters: "COD",
        words: ["COD"],
        bonusWords: [],
        rows: 1,
        cols: 3,
        placements: [{ word: "COD", row: 0, col: 0, direction: "HORIZONTAL" }],
      }],
    }],
  };

  const missingStoryWord = structuredClone(catalog);
  missingStoryWord.categories[0].storyWords = [];
  assert.throws(
    () => validateCatalog(missingStoryWord),
    /storyWords must cover every target word exactly once/,
  );

  const unknownStoryWord = structuredClone(catalog);
  unknownStoryWord.categories[0].storyWords = ["FISH"];
  assert.throws(
    () => validateCatalog(unknownStoryWord),
    /storyWords must cover every target word exactly once/,
  );

  const missingBengaliStory = structuredClone(catalog);
  missingBengaliStory.categories[0].storyBn = "";
  assert.throws(
    () => validateCatalog(missingBengaliStory),
    /category fish.storyBn must be a non-empty string/,
  );

  const substringStory = structuredClone(catalog);
  substringStory.categories[0].storyEn = "The codex swims near the fish.";
  assert.throws(
    () => validateCatalog(substringStory),
    /storyEn must contain target word COD as a whole word/,
  );

  const missingLessonWord = structuredClone(catalog);
  missingLessonWord.categories[0].levels[0].lesson.usedWords = [];
  assert.throws(
    () => validateCatalog(missingLessonWord),
    /lesson.usedWords must contain every target word/,
  );

  const substringLesson = structuredClone(catalog);
  substringLesson.categories[0].levels[0].lesson.sentenceEn = "The codex swims.";
  assert.throws(
    () => validateCatalog(substringLesson),
    /lesson.sentenceEn must contain target word COD as a whole word/,
  );
});
