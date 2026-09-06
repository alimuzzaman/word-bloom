package com.ritik.wordpuzzle.data.repository

import android.content.Context
import com.ritik.wordpuzzle.BuildConfig
import com.ritik.wordpuzzle.domain.model.Category
import com.ritik.wordpuzzle.domain.model.Level
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Result of one catalogue HTTP request. */
internal data class CatalogHttpResponse(
    val statusCode: Int,
    val body: String? = null,
    val etag: String? = null,
)

/** Small transport seam: tests can exercise fallback/version rules without a socket. */
internal interface CatalogHttpClient {
    suspend fun fetch(url: String, etag: String?): CatalogHttpResponse
}

/**
 * JDK transport used by the app.  Keeping this dependency-free avoids adding a
 * networking stack for one JSON request and works on the app's min SDK (24).
 */
internal class UrlConnectionCatalogHttpClient(
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    private val maxBodyBytes: Int = MAX_BODY_BYTES,
) : CatalogHttpClient {

    override suspend fun fetch(url: String, etag: String?): CatalogHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "identity")
                etag?.takeIf(String::isNotBlank)?.let { setRequestProperty("If-None-Match", it) }
            }
            try {
                val status = connection.responseCode
                val responseBody = if (status == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { it.readUtf8Bounded(maxBodyBytes) }
                } else {
                    null
                }
                CatalogHttpResponse(
                    statusCode = status,
                    body = responseBody,
                    etag = connection.getHeaderField("ETag")?.takeIf(String::isNotBlank),
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun java.io.InputStream.readUtf8Bounded(maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IOException("Catalogue response exceeds $maxBytes bytes")
            output.write(buffer, 0, count)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 2_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 3_000
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
        const val BUFFER_BYTES = 8 * 1024
    }
}

/** A validated raw catalogue saved for offline use. */
internal data class CachedCatalog(
    val raw: String,
    val catalogVersion: String,
    val etag: String?,
    val fetchedAtEpochMillis: Long,
)

/** Storage seam for the catalogue cache. */
internal interface CatalogCache {
    suspend fun read(): CachedCatalog?
    suspend fun write(catalog: CachedCatalog)
}

/**
 * Atomic app-private file cache.  The envelope keeps ETag/version metadata beside
 * the exact JSON body and is replaced with a rename, so a killed write cannot leave
 * a partially-written catalogue.
 */
internal class FileCatalogCache(
    private val directory: File,
    private val fileName: String = DEFAULT_FILE_NAME,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CatalogCache {

    constructor(context: Context, json: Json = Json { ignoreUnknownKeys = true }) : this(
        directory = context.filesDir,
        json = json,
    )

    override suspend fun read(): CachedCatalog? = withContext(Dispatchers.IO) {
        val file = File(directory, fileName)
        if (!file.isFile) return@withContext null
        runCatching {
            val envelope = json.decodeFromString<CacheEnvelope>(file.readText())
            require(envelope.cacheSchemaVersion == CACHE_SCHEMA_VERSION)
            require(envelope.catalogVersion.isNotBlank())
            require(envelope.body.isNotBlank())
            CachedCatalog(
                raw = envelope.body,
                catalogVersion = envelope.catalogVersion,
                etag = envelope.etag?.takeIf(String::isNotBlank),
                fetchedAtEpochMillis = envelope.fetchedAtEpochMillis,
            )
        }.getOrNull()
    }

    override suspend fun write(catalog: CachedCatalog) = withContext(Dispatchers.IO) {
        require(catalog.raw.isNotBlank())
        require(catalog.catalogVersion.isNotBlank())
        directory.mkdirs()
        val target = File(directory, fileName)
        val temporary = File(directory, "$fileName.tmp")
        val encoded = json.encodeToString(
            CacheEnvelope.serializer(),
            CacheEnvelope(
                cacheSchemaVersion = CACHE_SCHEMA_VERSION,
                catalogVersion = catalog.catalogVersion,
                etag = catalog.etag,
                fetchedAtEpochMillis = catalog.fetchedAtEpochMillis,
                body = catalog.raw,
            ),
        )
        try {
            temporary.writeText(encoded)
            if (!temporary.renameTo(target)) {
                throw IOException("Could not atomically replace catalogue cache")
            }
        } finally {
            // A successful rename removes the temporary path; on failure this is
            // safe cleanup of only the exact cache temp file.
            if (temporary.exists()) temporary.delete()
        }
    }

    @Serializable
    @PublishedApi
    internal data class CacheEnvelope(
        val cacheSchemaVersion: Int,
        val catalogVersion: String,
        val etag: String? = null,
        val fetchedAtEpochMillis: Long,
        val body: String,
    )

    private companion object {
        const val DEFAULT_FILE_NAME = "level_catalog_cache.json"
        const val CACHE_SCHEMA_VERSION = 1
    }
}

/**
 * Loads remote content when configured and falls back to a validated cache, then
 * the bundled asset.  A remote response is promoted only when it is structurally
 * valid, educator-approved (for schema v2), and not older than the cached version.
 */
internal class RemoteCatalogSource(
    private val endpoint: String,
    private val remoteParser: LevelCatalogParser,
    private val assetLoader: suspend () -> LevelCatalog,
    private val httpClient: CatalogHttpClient,
    private val cache: CatalogCache,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {

    private val mutex = Mutex()
    @Volatile private var memory: LevelCatalog? = null
    @Volatile private var cachedEntryRead = false
    private var cachedEntry: CachedCatalog? = null

    suspend fun getCatalog(): LevelCatalog {
        memory?.let { return it }
        return mutex.withLock {
            memory ?: loadInitial().also { memory = it }
        }
    }

    /**
     * Explicit refresh hook for a future pull-to-refresh/settings action.  It is
     * safe to call repeatedly and never replaces a valid catalogue with bad data.
     * Returns true only when an accepted response changes or confirms the catalog.
     */
    suspend fun refresh(): Boolean = mutex.withLock {
        val cached = readCachedEntry()
        val response = fetchRemote(cached?.etag) ?: return@withLock false
        when (response.statusCode) {
            HttpURLConnection.HTTP_NOT_MODIFIED -> {
                val existing = cached ?: return@withLock false
                val parsed = parseCached(existing) ?: return@withLock false
                val refreshed = existing.copy(
                    fetchedAtEpochMillis = nowEpochMillis(),
                    etag = response.etag ?: existing.etag,
                )
                runCatching { cache.write(refreshed) }
                if (memory == null) memory = parsed
                true
            }

            HttpURLConnection.HTTP_OK -> {
                val body = response.body ?: return@withLock false
                val accepted = parseRemote(body, cached) ?: return@withLock false
                val entry = CachedCatalog(
                    raw = body,
                    catalogVersion = accepted.catalogVersion,
                    etag = response.etag,
                    fetchedAtEpochMillis = nowEpochMillis(),
                )
                runCatching { cache.write(entry) }
                cachedEntry = entry
                cachedEntryRead = true
                memory = accepted
                true
            }

            else -> false
        }
    }

    private suspend fun loadInitial(): LevelCatalog {
        val cached = readCachedEntry()
        val response = fetchRemote(cached?.etag)
        when (response?.statusCode) {
            HttpURLConnection.HTTP_NOT_MODIFIED -> {
                cached?.let { parseCached(it)?.let { parsed -> return parsed } }
            }

            HttpURLConnection.HTTP_OK -> {
                val body = response.body
                if (body != null) {
                    val accepted = parseRemote(body, cached)
                    if (accepted != null) {
                        val entry = CachedCatalog(
                            raw = body,
                            catalogVersion = accepted.catalogVersion,
                            etag = response.etag,
                            fetchedAtEpochMillis = nowEpochMillis(),
                        )
                        runCatching { cache.write(entry) }
                        cachedEntry = entry
                        cachedEntryRead = true
                        return accepted
                    }
                }
            }
        }

        cached?.let { parseCached(it)?.let { parsed -> return parsed } }
        return assetLoader()
    }

    private fun parseRemote(raw: String, cached: CachedCatalog?): LevelCatalog? = runCatching {
        val parsed = remoteParser.parse(raw)
        if (cached != null && compareCatalogVersions(parsed.catalogVersion, cached.catalogVersion) < 0) {
            return@runCatching null
        }
        parsed
    }.getOrNull()

    private fun parseCached(entry: CachedCatalog): LevelCatalog? = runCatching {
        remoteParser.parse(entry.raw).takeIf { it.catalogVersion == entry.catalogVersion }
    }.getOrNull()

    private suspend fun readCachedEntry(): CachedCatalog? {
        if (cachedEntryRead) return cachedEntry
        // All callers reach this helper while holding [mutex]. Keeping the read
        // lock-free avoids trying to acquire the same non-reentrant Mutex twice.
        cachedEntry = runCatching {
            cache.read()?.takeIf { entry ->
                // Metadata is untrusted too.  A mismatched version must not be
                // allowed to make a stale body look newer than it is.
                val parsed = remoteParser.parse(entry.raw)
                parsed.catalogVersion == entry.catalogVersion
            }
        }.getOrNull()
        cachedEntryRead = true
        return cachedEntry
    }

    private suspend fun fetchRemote(etag: String?): CatalogHttpResponse? {
        if (!isValidEndpoint(endpoint)) return null
        return runCatching { httpClient.fetch(endpoint, etag) }.getOrNull()
    }

    private fun isValidEndpoint(value: String): Boolean = runCatching {
        val uri = URI(value)
        val secureScheme = uri.scheme.equals("https", ignoreCase = true)
        val debugHttp = BuildConfig.DEBUG && uri.scheme.equals("http", ignoreCase = true)
        (secureScheme || debugHttp) &&
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null
    }.getOrDefault(false)
}

/** Repository facade consumed by ViewModels. */
internal class RemoteLevelRepository(
    context: Context,
    endpoint: String = BuildConfig.LEVEL_CATALOG_URL,
    json: Json = Json { ignoreUnknownKeys = true },
    httpClient: CatalogHttpClient = UrlConnectionCatalogHttpClient(),
    cache: CatalogCache = FileCatalogCache(context, json),
) : LevelRepository {

    private val source = RemoteCatalogSource(
        endpoint = endpoint,
        remoteParser = LevelCatalogParser(json, requireApprovedContent = true),
        assetLoader = { AssetLevelRepository(context, json).loadFromAssets() },
        httpClient = httpClient,
        cache = cache,
    )

    override suspend fun getCategories(): List<Category> = source.getCatalog().categories

    override suspend fun getLevels(categoryId: String): List<Level> =
        source.getCatalog().levelsByCategory[categoryId].orEmpty()

    override suspend fun getLevel(categoryId: String, id: Int): Level? =
        getLevels(categoryId).firstOrNull { it.id == id }

    /** Manually fetch and promote a newer server catalogue. */
    suspend fun refresh(): Boolean = source.refresh()
}

/**
 * Compare the numeric/alphabetic components used by versions such as
 * `2026.09.02-1`.  Unknown trailing numeric zeroes compare equal, and malformed
 * values are never accepted by [LevelCatalogParser] in the first place.
 */
internal fun compareCatalogVersions(left: String, right: String): Int {
    val leftHash = left.startsWith("sha256-", ignoreCase = true)
    val rightHash = right.startsWith("sha256-", ignoreCase = true)
    if (leftHash || rightHash) {
        // A schema-1 server uses a content hash rather than an ordered release
        // number.  Treat two hashes as the same ordering so a freshly fetched
        // hash can replace an older cached hash; schema-2 numeric versions are
        // considered newer than a hash-derived migration version.
        return when {
            leftHash && rightHash -> 0
            leftHash -> -1
            else -> 1
        }
    }
    val tokenPattern = Regex("[0-9]+|[A-Za-z]+")
    val a = tokenPattern.findAll(left).map { it.value }.toList()
    val b = tokenPattern.findAll(right).map { it.value }.toList()
    val size = maxOf(a.size, b.size)
    for (index in 0 until size) {
        val leftToken = a.getOrNull(index) ?: "0"
        val rightToken = b.getOrNull(index) ?: "0"
        val leftNumeric = leftToken.all(Char::isDigit)
        val rightNumeric = rightToken.all(Char::isDigit)
        val comparison = when {
            leftNumeric && rightNumeric -> compareNumericTokens(leftToken, rightToken)
            leftNumeric -> 1
            rightNumeric -> -1
            else -> leftToken.compareTo(rightToken, ignoreCase = true)
        }
        if (comparison != 0) return comparison
    }
    return 0
}

private fun compareNumericTokens(left: String, right: String): Int {
    val normalizedLeft = left.trimStart('0').ifEmpty { "0" }
    val normalizedRight = right.trimStart('0').ifEmpty { "0" }
    return normalizedLeft.length.compareTo(normalizedRight.length).takeIf { it != 0 }
        ?: normalizedLeft.compareTo(normalizedRight)
}
