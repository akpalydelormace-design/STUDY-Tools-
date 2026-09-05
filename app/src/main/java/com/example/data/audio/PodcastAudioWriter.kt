package com.example.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

object PodcastAudioWriter {

    /**
     * Writes base64 encoded audio parts (WAV/MP3/PCM) into a local file.
     * Guarantees a valid WAV structure if PCM/WAV parts are provided.
     */
    fun saveBase64AudioChunksToFile(
        context: Context,
        pdfId: Long,
        audioBase64Chunks: List<ByteArray>
    ): Pair<File, Long> {
        val podcastDir = File(context.filesDir, "podcasts").apply { mkdirs() }
        val outputFile = File(podcastDir, "podcast_pdf_${pdfId}_${System.currentTimeMillis()}.wav")

        // First collect all raw PCM / audio bytes
        val allAudioBytes = mutableListOf<Byte>()
        for (rawChunk in audioBase64Chunks) {
            val pcmData = stripWavHeaderIfPresent(rawChunk)
            for (b in pcmData) {
                allAudioBytes.add(b)
            }
        }

        val rawPcm = allAudioBytes.toByteArray()
        writeWavFile(outputFile, rawPcm, sampleRate = 24000, channels = 1, bitsPerSample = 16)

        val durationMs = calculateDurationMs(outputFile, rawPcm.size, sampleRate = 24000, channels = 1, bitsPerSample = 16)
        return Pair(outputFile, durationMs)
    }

    private fun stripWavHeaderIfPresent(chunk: ByteArray): ByteArray {
        // Simple check if chunk starts with "RIFF" (0x52 0x49 0x46 0x46) and "WAVE" at offset 8
        if (chunk.size > 44 &&
            chunk[0] == 'R'.code.toByte() &&
            chunk[1] == 'I'.code.toByte() &&
            chunk[2] == 'F'.code.toByte() &&
            chunk[3] == 'F'.code.toByte()
        ) {
            // Strip the standard 44-byte WAV header so we can concatenate pure PCM frames
            return chunk.copyOfRange(44, chunk.size)
        }
        return chunk
    }

    private fun writeWavFile(
        file: File,
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ) {
        FileOutputStream(file).use { out ->
            val totalAudioLen = pcmData.size.toLong()
            val totalDataLen = totalAudioLen + 36
            val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

            val header = ByteArray(44)
            // RIFF/WAVE header
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = (totalDataLen shr 8 and 0xff).toByte()
            header[6] = (totalDataLen shr 16 and 0xff).toByte()
            header[7] = (totalDataLen shr 24 and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // 'fmt ' chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 16 for PCM
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // AudioFormat PCM = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = (sampleRate shr 8 and 0xff).toByte()
            header[26] = (sampleRate shr 16 and 0xff).toByte()
            header[27] = (sampleRate shr 24 and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte()
            header[31] = (byteRate shr 24 and 0xff).toByte()
            header[32] = (channels * bitsPerSample / 8).toByte() // block align
            header[33] = 0
            header[34] = bitsPerSample.toByte()
            header[35] = 0

            // 'data' chunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = (totalAudioLen shr 8 and 0xff).toByte()
            header[42] = (totalAudioLen shr 16 and 0xff).toByte()
            header[43] = (totalAudioLen shr 24 and 0xff).toByte()

            out.write(header, 0, 44)
            out.write(pcmData)
        }
    }

    private fun calculateDurationMs(
        file: File,
        pcmBytesLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            val parsedMs = durationStr?.toLongOrNull()
            if (parsedMs != null && parsedMs > 0) {
                return parsedMs
            }
        } catch (_: Exception) {}

        // Fallback calculation from PCM parameters
        val bytesPerSecond = sampleRate * channels * (bitsPerSample / 8)
        if (bytesPerSecond <= 0) return 0L
        return (pcmBytesLength.toLong() * 1000L) / bytesPerSecond
    }
}
