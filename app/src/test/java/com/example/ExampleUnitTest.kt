package com.example

import com.example.tts.BengaliTextChunker
import com.example.tts.SherpaOfflineTtsEngine
import com.example.tts.WavFileWriter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `chunker keeps in-word marks whole and splits only after punctuation followed by space or end`() {
        val sample = "সে একজন চো'র এবং র'ক্তের দাম ১২,৫০০ টাকা বা ১২.৫ টাকা। পরের প্রশ্ন কি? দারুণ খবর! কমা, শেষ'"
        val chunks = BengaliTextChunker.chunkText(sample)

        assertEquals(5, chunks.size)
        // Piece 1 keeps চো'র, র'ক্তের, ১২,৫০০, ১২.৫ whole and ends at ।
        assertEquals("সে একজন চো'র এবং র'ক্তের দাম ১২,৫০০ টাকা বা ১২.৫ টাকা।", chunks[0].text)
        assertEquals(0.40f, chunks[0].pauseSeconds, 0.001f)
        assertEquals(8820, chunks[0].silenceSampleCount(22050))

        // Piece 2 ends at ?
        assertEquals("পরের প্রশ্ন কি?", chunks[1].text)
        assertEquals(0.30f, chunks[1].pauseSeconds, 0.001f)
        assertEquals(6615, chunks[1].silenceSampleCount(22050))

        // Piece 3 ends at !
        assertEquals("দারুণ খবর!", chunks[2].text)
        assertEquals(0.30f, chunks[2].pauseSeconds, 0.001f)

        // Piece 4 ends at ,
        assertEquals("কমা,", chunks[3].text)
        assertEquals(0.20f, chunks[3].pauseSeconds, 0.001f)
        assertEquals(4410, chunks[3].silenceSampleCount(22050))

        // Piece 5 ends at ' at end of text
        assertEquals("শেষ'", chunks[4].text)
        assertEquals(0.20f, chunks[4].pauseSeconds, 0.001f)
    }

    @Test
    fun `chunker splits pieces over 300 chars strictly at word boundaries`() {
        val word = "বাংলাদেশ"
        // 45 words * ~9 chars = ~405 chars with no punctuation until the end ।
        val longSentence = (1..45).joinToString(" ") { word } + "।"
        assertTrue(longSentence.length > 300)

        val chunks = BengaliTextChunker.chunkText(longSentence)
        assertEquals(2, chunks.size)
        assertTrue(chunks[0].text.length <= 300)
        assertTrue(chunks[1].text.length <= 300)
        assertTrue(chunks[1].text.endsWith("।"))
        assertEquals(0.0f, chunks[0].pauseSeconds, 0.001f)
        assertEquals(0.40f, chunks[1].pauseSeconds, 0.001f)
    }

    @Test
    fun `verify v1_9_4_fixed model constants and wav writer`() {
        assertEquals(114188118L, SherpaOfflineTtsEngine.EXPECTED_MODEL_BYTES)
        assertEquals(567L, SherpaOfflineTtsEngine.EXPECTED_TOKENS_BYTES)
        assertEquals("model_fullmeta.onnx", SherpaOfflineTtsEngine.MODEL_FILENAME)
        assertEquals("tokens_sherpa.txt", SherpaOfflineTtsEngine.TOKENS_FILENAME)
        assertEquals(22050, SherpaOfflineTtsEngine.VITS_SAMPLE_RATE)

        val tempWav = File.createTempFile("test_voice", ".wav")
        try {
            val dummySamples = FloatArray(2205) { 0.25f }
            WavFileWriter.writeMono16BitWav(dummySamples, 22050, tempWav)
            assertEquals(44L + 2205L * 2L, tempWav.length())
        } finally {
            tempWav.delete()
        }
    }

    @Test
    fun `preparePieceForJoining strips only exact leading zeros and applies 20ms raised cosine fade`() {
        // 100 exact 0.0f samples + 1000 non-zero samples (including quiet 0.001f samples)
        val raw = FloatArray(1100) { idx ->
            when {
                idx < 100 -> 0.0f
                idx < 150 -> 0.001f // quiet speech must NOT be stripped
                else -> 0.5f
            }
        }
        val processed = WavFileWriter.preparePieceForJoining(raw, 22050)
        // Exactly 100 leading 0.0f stripped -> 1000 samples remain
        assertEquals(1000, processed.size)
        // Starts at 0.0f (fade-in start) and ends at 0.0f (fade-out end)
        assertEquals(0.0f, processed.first(), 1e-6f)
        assertEquals(0.0f, processed.last(), 1e-6f)
        // Middle untouched (index 500 is outside the 441-sample 20ms fades)
        assertEquals(0.5f, processed[500], 1e-6f)
    }

    @Test
    fun `preparePieceForSynthesis normalizes Bengali numbers and strips trailing split marks`() {
        val script =
            "শুভ সন্ধ্যা, রিপোর্টার হাসান নিউজ ডেস্কে আপনাদের স্বাগতম। এই প্রযুক্তিতে ১২,৫০০ টাকার সাশ্রয়ী ডিভাইসেও মাত্র ১২.৫ সেকেন্ডে দীর্ঘ প্রতিবেদন তৈরি করা সম্ভব।"
        val rawChunks = BengaliTextChunker.chunkText(script)
        assertEquals(3, rawChunks.size)

        val mergedChunks = BengaliTextChunker.mergeShortSubClausesForSynthesis(rawChunks)
        assertEquals(2, mergedChunks.size)
        assertEquals(
            "শুভ সন্ধ্যা, রিপোর্টার হাসান নিউজ ডেস্কে আপনাদের স্বাগতম",
            BengaliTextChunker.preparePieceForSynthesis(mergedChunks[0].text)
        )
        assertEquals(
            "এই প্রযুক্তিতে ১২৫০০ টাকার সাশ্রয়ী ডিভাইসেও মাত্র ১২ দশমিক ৫ সেকেন্ডে দীর্ঘ প্রতিবেদন তৈরি করা সম্ভব",
            BengaliTextChunker.preparePieceForSynthesis(mergedChunks[1].text)
        )
    }

    @Test
    fun `preparePieceForJoining trims cold start murmur and isolated pre-onset pop`() {
        val sr = 22050
        val win = 331 // 15ms at 22050 Hz
        // 20 windows (300ms):
        // windows 0..1: isolated cold-start pop (0.14f)
        // windows 2..7: 90ms silence gap (0.01f)
        // windows 8..17: real speech syllable (0.45f)
        // windows 18..19: post-speech murmur (0.01f)
        val raw = FloatArray(20 * win) { idx ->
            val w = idx / win
            when (w) {
                0, 1 -> 0.14f
                in 2..7 -> 0.01f
                in 8..17 -> 0.45f
                else -> 0.01f
            }
        }
        val cleaned = WavFileWriter.preparePieceForJoining(raw, sr)
        // Should start at window 6 (2 windows = 30ms before window 8) and keep through end -> 14 windows
        assertEquals(14 * win, cleaned.size)
        assertEquals(0.0f, cleaned.first(), 1e-6f)
        assertEquals(0.0f, cleaned.last(), 1e-6f)
    }
}

