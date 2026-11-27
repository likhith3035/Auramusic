package com.music.bitchord.data.telemetry

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuraAnalyticsTest {
    private lateinit var server: MockWebServer
    private lateinit var tempDir: java.io.File

    private var originalStatsUrl: String = ""
    private var originalPresenceUrl: String = ""
    private var originalGithubUrl: String = ""

    @BeforeTest
    fun setUp() {
        originalStatsUrl = AuraTelemetryRepository.statsBaseUrl
        originalPresenceUrl = AuraTelemetryRepository.livePresenceUrl
        originalGithubUrl = AuraTelemetryRepository.githubReleasesUrl
        AuraAnalytics.resetForTesting()
        AuraTelemetryRepository.clearCacheForTesting()
        server = MockWebServer()
        server.start()
        tempDir = Files.createTempDirectory("aura-analytics-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        AuraTelemetryRepository.statsBaseUrl = originalStatsUrl
        AuraTelemetryRepository.livePresenceUrl = originalPresenceUrl
        AuraTelemetryRepository.githubReleasesUrl = originalGithubUrl
        AuraAnalytics.resetForTesting()
        AuraTelemetryRepository.clearCacheForTesting()
        server.shutdown()
        tempDir.deleteRecursively()
    }

    @Test
    fun unconfiguredAnalyticsDoesNotSendAnyFakeRequests() {
        AuraAnalytics.init(
            directory = tempDir,
            buildPosthogApiKey = "",
            buildPosthogHost = server.url("/").toString(),
            version = "3.3.0",
            platform = "Android",
            osVersion = "15",
            distinctIdProvider = { "install-uuid-1" },
        )

        assertFalse(AuraAnalytics.isConfigured())
        assertEquals(AnalyticsConfigSource.NONE, AuraAnalytics.status.value.source)

        AuraAnalytics.trackAppLaunch()
        AuraAnalytics.trackPlayCounted()
        AuraAnalytics.trackDownloadCompleted("flac")
        AuraAnalytics.trackSearchSubmitted("youtube")
        AuraAnalytics.trackFeatureUsed("equalizer")

        val flushed = runBlocking { AuraAnalytics.flushNow() }
        assertFalse(flushed)
        assertNull(server.takeRequest(300, TimeUnit.MILLISECONDS))
        assertEquals(0, AuraAnalytics.status.value.queuedEvents)
    }

    @Test
    fun posthogBatchFlushesAnonymousEventsAndStripsPii() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":1}"""))

        AuraAnalytics.init(
            directory = tempDir,
            buildPosthogApiKey = "phc_test_project_key_123456",
            buildPosthogHost = server.url("/").toString(),
            version = "3.3.0",
            platform = "Desktop",
            osVersion = "Windows 11",
            distinctIdProvider = { "anon-install-42" },
        )

        assertTrue(AuraAnalytics.isConfigured())
        assertEquals(AnalyticsConfigSource.BUILD_CONFIG, AuraAnalytics.status.value.source)

        AuraAnalytics.trackPlayCounted()
        AuraAnalytics.trackDownloadCompleted("mp3")
        AuraAnalytics.trackSearchSubmitted("desktop")
        AuraAnalytics.trackFeatureUsed("listen_together_create")

        assertTrue(AuraAnalytics.flushNow())

        val req = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("POST", req.method)
        assertEquals("/batch/", req.path)
        val payload = Json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals("phc_test_project_key_123456", payload.getValue("api_key").jsonPrimitive.content)

        val batch = payload.getValue("batch").jsonArray
        assertEquals(4, batch.size)
        val firstEvent = batch[0].jsonObject
        assertEquals("track_played", firstEvent.getValue("event").jsonPrimitive.content)
        assertEquals("anon-install-42", firstEvent.getValue("distinct_id").jsonPrimitive.content)
        val props = firstEvent.getValue("properties").jsonObject
        assertEquals("Desktop", props.getValue("platform").jsonPrimitive.content)
        assertEquals("3.3.0", props.getValue("app_version").jsonPrimitive.content)
        assertFalse(props.containsKey("email"))
        assertFalse(props.containsKey("query"))
        assertFalse(props.containsKey("song"))

        assertEquals(0, AuraAnalytics.status.value.queuedEvents)
        assertEquals(4, AuraAnalytics.status.value.sentThisSession)
        assertEquals(200, AuraAnalytics.status.value.lastHttpCode)
    }

    @Test
    fun saveAndVerifyRuntimeConfigRejectsInvalidHttpAndPersistsValidKey() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"type":"authentication_error"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":1}"""))

        AuraAnalytics.init(
            directory = tempDir,
            buildPosthogApiKey = "",
            buildPosthogHost = server.url("/").toString(),
            version = "3.3.0",
            platform = "Android",
            distinctIdProvider = { "anon-install-99" },
        )

        val badResult = AuraAnalytics.saveAndVerifyConfig("phc_wrong_key_123", server.url("/").toString())
        assertTrue(badResult.isFailure)
        assertFalse(AuraAnalytics.isConfigured())

        val goodResult = AuraAnalytics.saveAndVerifyConfig("phc_real_key_987654", server.url("/").toString())
        assertTrue(goodResult.isSuccess)
        assertEquals(200, goodResult.getOrThrow())
        assertTrue(AuraAnalytics.isConfigured())
        assertEquals(AnalyticsConfigSource.RUNTIME_OVERRIDE, AuraAnalytics.status.value.source)

        val verifyReq = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/capture/", verifyReq.path)
        val verifyReq2 = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/capture/", verifyReq2.path)
    }

    @Test
    fun githubReleasesAndLivePresenceParsersComputeExactTotals() {
        val releasesJson = """
            [
              {
                "tag_name": "v3.3.0",
                "name": "AuraMusic v3.3.0",
                "published_at": "2026-11-12T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [
                  { "name": "AuraMusic-3.3.0-arm64-v8a.apk", "download_count": 15 },
                  { "name": "AuraMusic-3.3.0-windows-x64-setup.exe", "download_count": 4 },
                  { "name": "AuraMusic-3.3.0-linux-x86_64.AppImage", "download_count": 2 }
                ]
              },
              {
                "tag_name": "v3.2.1",
                "name": "AuraMusic v3.2.1",
                "published_at": "2026-11-01T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [
                  { "name": "AuraMusic-3.2.1-universal.apk", "download_count": 16 }
                ]
              }
            ]
        """.trimIndent()

        val summary = AuraTelemetryRepository.parseGitHubReleasesJson(releasesJson, fetchedAtMs = 1000L)
        assertEquals(37L, summary.totalDownloads)
        assertEquals(31L, summary.androidDownloads)
        assertEquals(6L, summary.desktopDownloads)
        assertEquals(0L, summary.otherDownloads)
        assertEquals(2, summary.releaseCount)
        assertEquals("v3.3.0", summary.latestTag)
        assertEquals(21L, summary.latestReleaseDownloads)

        val presenceJson = """
            {"online": 14, "android": 11, "pc": 3, "activeParties": 2, "partyMembers": 5, "totalParties": 18, "totalChats": 42}
        """.trimIndent()
        val pulse = AuraTelemetryRepository.parseLivePresenceJson(presenceJson, latencyMs = 120L)
        assertEquals(14, pulse.onlineNow)
        assertEquals(11, pulse.androidOnline)
        assertEquals(3, pulse.pcOnline)
        assertEquals(2, pulse.activeParties)
        assertEquals(5, pulse.partyMembers)
        assertEquals(18L, pulse.totalPartiesCreated)
        assertEquals(42L, pulse.totalChatsSent)
    }

    @Test
    fun fetchDashboardSnapshotSurvivesPartialRenderFailureAndPreservesRealLiveData() = runBlocking {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                return when (request.path) {
                    "/stats?format=json" -> MockResponse().setResponseCode(200).setBody(
                        """{"online":7,"android":5,"pc":2,"activeParties":1,"partyMembers":3,"totalParties":9,"totalChats":12}""",
                    )
                    "/releases" -> MockResponse().setResponseCode(200).setBody(
                        """[{"tag_name":"v3.3.0","draft":false,"prerelease":false,"assets":[{"name":"app.apk","download_count":21},{"name":"setup.exe","download_count":9}]}]""",
                    )
                    "/api/stats" -> MockResponse().setResponseCode(503)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        AuraTelemetryRepository.livePresenceUrl = server.url("/stats?format=json").toString()
        AuraTelemetryRepository.githubReleasesUrl = server.url("/releases").toString()
        AuraTelemetryRepository.statsBaseUrl = server.url("").toString().trimEnd('/')

        val snapshot = AuraTelemetryRepository.fetchDashboardSnapshot(forceRefresh = true)
        assertTrue(snapshot.livePulseResult.isSuccess)
        assertEquals(7, snapshot.livePulseResult.getOrThrow().onlineNow)
        assertTrue(snapshot.githubDownloadsResult.isSuccess)
        assertEquals(30L, snapshot.githubDownloadsResult.getOrThrow().totalDownloads)
        assertEquals(21L, snapshot.githubDownloadsResult.getOrThrow().androidDownloads)
        assertEquals(9L, snapshot.githubDownloadsResult.getOrThrow().desktopDownloads)
        assertTrue(snapshot.dailyHeartbeatResult.isFailure)
    }
}
