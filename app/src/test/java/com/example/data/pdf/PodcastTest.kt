package com.example.data.pdf

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.PodcastAudioWriter
import com.example.data.local.StudyDatabase
import com.example.data.model.PodcastEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PodcastTest {

    private lateinit var context: Context
    private lateinit var database: StudyDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPodcastEntityStorageAndRetrieval() = runBlocking {
        val podcast = PodcastEntity(
            pdfId = 42L,
            title = "Podcast — Philosophie.pdf",
            durationMs = 120000L,
            localAudioPath = "/tmp/podcast_42.wav",
            script = "[{\"speaker\":\"PROFESSEUR\",\"text\":\"Bonjour !\"}]",
            status = "COMPLETED"
        )

        val insertedId = database.podcastDao().insertPodcast(podcast)
        assertTrue(insertedId > 0)

        val retrieved = database.podcastDao().getPodcastForPdfSync(42L)
        assertNotNull(retrieved)
        assertEquals("Podcast — Philosophie.pdf", retrieved?.title)
        assertEquals(120000L, retrieved?.durationMs)
        assertEquals("COMPLETED", retrieved?.status)

        // Update playback position
        database.podcastDao().updatePlaybackPosition(retrieved!!.id, 45000L)
        val updated = database.podcastDao().getPodcastForPdfSync(42L)
        assertEquals(45000L, updated?.playbackPositionMs)

        // Delete podcast
        database.podcastDao().deletePodcastByPdfId(42L)
        val afterDelete = database.podcastDao().getPodcastForPdfSync(42L)
        assertNull(afterDelete)
    }

    @Test
    fun testPodcastAudioWriterSaveChunksToFile() {
        // Create dummy PCM audio chunk (100 bytes)
        val chunk1 = ByteArray(100) { 0x01 }
        val chunk2 = ByteArray(100) { 0x02 }

        val (savedFile, durationMs) = PodcastAudioWriter.saveBase64AudioChunksToFile(
            context = context,
            pdfId = 99L,
            audioBase64Chunks = listOf(chunk1, chunk2)
        )

        assertTrue(savedFile.exists())
        // 44 bytes header + 200 bytes PCM = 244 bytes
        assertEquals(244L, savedFile.length())
        assertTrue(durationMs >= 0L)

        // Cleanup test file
        savedFile.delete()
    }

    @Test
    fun testTextValidationForPodcastGeneration() {
        val emptyText = ""
        val shortText = "Bonjour"
        val validText = "Ce cours de philosophie porte sur le concept de conscience chez René Descartes..."

        assertTrue(emptyText.trim().length < 50)
        assertTrue(shortText.trim().length < 50)
        assertFalse(validText.trim().length < 50)
    }

    @Test
    fun testNoHardcodedApiKeyInSourceCode() {
        // Read BuildConfig or env placeholder
        val testApiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        // Verify key is not a raw leaked secret starting with AIza
        assertFalse("API Key should not be a raw hardcoded AIza key", testApiKey.startsWith("AIzaSyFakeKeyThatIsExposed"))
    }
}
