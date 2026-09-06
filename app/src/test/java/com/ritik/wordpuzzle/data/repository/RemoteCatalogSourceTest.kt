package com.ritik.wordpuzzle.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCatalogSourceTest {

    private val parser = LevelCatalogParser(
        Json { ignoreUnknownKeys = true },
        requireApprovedContent = true,
    )

    @Test
    fun `valid newer remote catalogue wins over bundled content and is cached`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val cache = InMemoryCatalogCache()
        val client = FakeCatalogHttpClient(
            CatalogHttpResponse(
                statusCode = 200,
                body = catalogJson(version = "2.0", categoryName = "Remote Fish"),
                etag = "remote-etag",
            ),
        )
        val source = source(client, cache) { bundled }

        val result = source.getCatalog()

        assertEquals("2.0", result.catalogVersion)
        assertEquals("Remote Fish", result.categories.single().nameEn)
        assertEquals("2.0", cache.value?.catalogVersion)
        assertEquals("remote-etag", cache.value?.etag)
        assertEquals(1, client.calls)
    }

    @Test
    fun `http failure falls back to a valid cached catalogue before bundled content`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val cache = InMemoryCatalogCache(
            CachedCatalog(
                raw = catalogJson(version = "2.0", categoryName = "Cached Fish"),
                catalogVersion = "2.0",
                etag = "cached-etag",
                fetchedAtEpochMillis = 10L,
            ),
        )
        val client = FakeCatalogHttpClient(CatalogHttpResponse(statusCode = 503))
        val source = source(client, cache) { bundled }

        val result = source.getCatalog()

        assertEquals("2.0", result.catalogVersion)
        assertEquals("Cached Fish", result.categories.single().nameEn)
        assertEquals("cached-etag", client.lastEtag)
    }

    @Test
    fun `invalid or stale remote content never replaces a newer cached catalogue`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val cache = InMemoryCatalogCache(
            CachedCatalog(
                raw = catalogJson(version = "3.0", categoryName = "Cached Fish"),
                catalogVersion = "3.0",
                etag = "cached-etag",
                fetchedAtEpochMillis = 10L,
            ),
        )
        val staleClient = FakeCatalogHttpClient(
            CatalogHttpResponse(
                statusCode = 200,
                body = catalogJson(version = "2.0", categoryName = "Stale Fish"),
                etag = "stale-etag",
            ),
        )
        val staleSource = source(staleClient, cache) { bundled }

        val staleResult = staleSource.getCatalog()
        assertEquals("3.0", staleResult.catalogVersion)
        assertEquals("Cached Fish", staleResult.categories.single().nameEn)
        assertEquals("3.0", cache.value?.catalogVersion)

        // A syntactically valid response with incomplete story coverage is
        // still malformed content. It must fall back all the way to the
        // bundled parser, rather than surfacing an exception to the UI.
        val malformedClient = FakeCatalogHttpClient(
            CatalogHttpResponse(
                statusCode = 200,
                body = catalogJson(version = "4.0", categoryName = "Malformed Fish")
                    .replace("\"storyWords\":[\"CAT\"]", "\"storyWords\":[]"),
            ),
        )
        val emptyCache = InMemoryCatalogCache()
        val malformedSource = source(malformedClient, emptyCache) { bundled }
        val malformedResult = malformedSource.getCatalog()
        assertEquals("1.0", malformedResult.catalogVersion)
        assertEquals("Bundled Fish", malformedResult.categories.single().nameEn)
        assertNull(emptyCache.value)
    }

    @Test
    fun `remote draft content falls back to the bundled approved catalogue`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val draft = catalogJson(version = "2.0", categoryName = "Draft Fish")
            .replace("\"contentReviewStatus\":\"approved\"", "\"contentReviewStatus\":\"draft\"")
        val client = FakeCatalogHttpClient(CatalogHttpResponse(statusCode = 200, body = draft))
        val cache = InMemoryCatalogCache()
        val source = source(client, cache) { bundled }

        val result = source.getCatalog()

        assertEquals("1.0", result.catalogVersion)
        assertEquals("Bundled Fish", result.categories.single().nameEn)
        assertNull(cache.value)
    }

    @Test
    fun `corrupt cache falls back to the bundled catalogue when the network is down`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val cache = InMemoryCatalogCache(
            CachedCatalog(
                raw = "{not-json}",
                catalogVersion = "2.0",
                etag = "corrupt-etag",
                fetchedAtEpochMillis = 10L,
            ),
        )
        val client = FakeCatalogHttpClient(CatalogHttpResponse(statusCode = 503))
        val source = source(client, cache) { bundled }

        val result = source.getCatalog()

        assertEquals("1.0", result.catalogVersion)
        assertEquals("Bundled Fish", result.categories.single().nameEn)
    }

    @Test
    fun `not modified response reuses cache and updates its freshness`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val cache = InMemoryCatalogCache(
            CachedCatalog(
                raw = catalogJson(version = "2.0", categoryName = "Cached Fish"),
                catalogVersion = "2.0",
                etag = "cached-etag",
                fetchedAtEpochMillis = 10L,
            ),
        )
        val client = FakeCatalogHttpClient(
            CatalogHttpResponse(statusCode = 304, etag = "cached-etag"),
        )
        val source = source(client, cache, now = { 42L }) { bundled }

        assertTrue(source.refresh())
        val result = source.getCatalog()

        assertEquals("2.0", result.catalogVersion)
        assertEquals("Cached Fish", result.categories.single().nameEn)
        assertEquals("cached-etag", client.lastEtag)
        assertEquals(42L, cache.value?.fetchedAtEpochMillis)
    }

    @Test
    fun `draft remote content is rejected and bundled content remains available`() = runTest {
        val bundled = parser.parse(catalogJson(version = "1.0", categoryName = "Bundled Fish"))
        val draft = catalogJson(version = "2.0", categoryName = "Draft Fish")
            .replace("\"contentReviewStatus\":\"approved\"", "\"contentReviewStatus\":\"draft\"")
        val cache = InMemoryCatalogCache()
        val client = FakeCatalogHttpClient(
            CatalogHttpResponse(statusCode = 200, body = draft, etag = "draft-etag"),
        )
        val source = source(client, cache) { bundled }

        val result = source.getCatalog()

        assertEquals("1.0", result.catalogVersion)
        assertEquals("Bundled Fish", result.categories.single().nameEn)
        assertNull(cache.value)
    }

    private fun source(
        client: CatalogHttpClient,
        cache: CatalogCache,
        now: () -> Long = { 1L },
        assetLoader: suspend () -> LevelCatalog,
    ): RemoteCatalogSource = RemoteCatalogSource(
        endpoint = "https://example.test/v1/catalog",
        remoteParser = parser,
        assetLoader = assetLoader,
        httpClient = client,
        cache = cache,
        nowEpochMillis = now,
    )

    private fun catalogJson(version: String, categoryName: String): String =
        """
        {"schemaVersion":2,"catalogVersion":"$version","contentReviewNotice":"Approved sample","categories":[{"id":"fish","nameEn":"$categoryName","nameBn":"মাছ","iconKey":"fish","order":1,"introductionEn":"Fish live in water.","introductionBn":"মাছ পানিতে বাস করে।","completionEn":"Fish are useful.","completionBn":"মাছ উপকারী।","completionWords":["CAT"],"storyEn":"The cat visits the fish.","storyBn":"বিড়ালটি মাছের কাছে যায়।","storyWords":["CAT"],"contentReviewStatus":"approved","levels":[{"id":1,"categoryId":"fish","order":1,"difficulty":1,"ageBand":"6-7","heroWord":{"word":"CAT","definitionEn":"A small animal","translationBn":"বিড়াল","meaningBn":"ছোট প্রাণী"},"wordMeanings":[{"word":"CAT","definitionEn":"A small animal","translationBn":"বিড়াল","meaningBn":"ছোট প্রাণী"}],"lesson":{"sentenceEn":"The cat sits.","sentenceBn":"বিড়ালটি বসে।","usedWords":["CAT"]},"letters":"CAT","words":["CAT"],"bonusWords":[],"rows":1,"cols":3,"placements":[{"word":"CAT","row":0,"col":0,"direction":"HORIZONTAL"}]}]}]}
        """.trimIndent()
}

private class FakeCatalogHttpClient(
    private val response: CatalogHttpResponse,
) : CatalogHttpClient {
    var calls: Int = 0
        private set
    var lastEtag: String? = null
        private set

    override suspend fun fetch(url: String, etag: String?): CatalogHttpResponse {
        calls += 1
        lastEtag = etag
        return response
    }
}

private class InMemoryCatalogCache(
    initial: CachedCatalog? = null,
) : CatalogCache {
    var value: CachedCatalog? = initial
        private set

    override suspend fun read(): CachedCatalog? = value

    override suspend fun write(catalog: CachedCatalog) {
        value = catalog
    }
}
