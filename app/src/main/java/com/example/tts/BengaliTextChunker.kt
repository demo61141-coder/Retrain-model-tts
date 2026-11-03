package com.example.tts

import kotlin.math.roundToInt

data class TextChunk(
    val text: String,
    val pauseSeconds: Float,
    val endingMark: Char?
) {
    fun silenceSampleCount(sampleRate: Int = 22050): Int {
        return (sampleRate * pauseSeconds).roundToInt()
    }
}

object BengaliTextChunker {

    const val MAX_CHUNK_CHARS = 300
    const val MAX_WORD_LIMIT = 5000

    private val SPLIT_MARKS = setOf('।', '?', '!', '.', ',', '\'')

    /**
     * Counts words in the input text.
     */
    fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Mandatory BanglaTTS v1.9.4-fixed chunking specification:
     * 1. Split the text ONLY after these marks: । ? ! . , '
     * 2. And ONLY when the mark is followed by a space or by the end of the text.
     *    A mark inside a word must NEVER split it (e.g., চো'র, র'ক্তের, ১২,৫০০, ১২.৫ stay whole).
     * 3. Keep the mark at the end of its piece.
     * 4. If a piece is longer than 300 characters, split it further ONLY at spaces (word boundaries),
     *    never inside a word.
     * 5. Pause durations:
     *    - after । -> 0.40 seconds
     *    - after ? or ! -> 0.30 seconds
     *    - after , or . or ' -> 0.20 seconds
     */
    fun chunkText(rawText: String): List<TextChunk> {
        val normalized = rawText.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return emptyList()

        val rawPieces = mutableListOf<Pair<String, Char?>>()
        val current = StringBuilder()

        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            current.append(ch)

            if (ch in SPLIT_MARKS) {
                val isAtEnd = (i == normalized.lastIndex)
                val isFollowedBySpace = !isAtEnd && normalized[i + 1].isWhitespace()

                if (isAtEnd || isFollowedBySpace) {
                    val pieceText = current.toString().trim()
                    if (pieceText.isNotEmpty()) {
                        rawPieces.add(pieceText to ch)
                    }
                    current.clear()
                    // Skip the following space after a split mark
                    if (isFollowedBySpace) {
                        i++
                    }
                }
            }
            i++
        }

        val remaining = current.toString().trim()
        if (remaining.isNotEmpty()) {
            rawPieces.add(remaining to null)
        }

        val finalChunks = mutableListOf<TextChunk>()
        for ((piece, mark) in rawPieces) {
            val pause = pauseForMark(mark)
            if (piece.length <= MAX_CHUNK_CHARS) {
                finalChunks.add(
                    TextChunk(
                        text = piece,
                        pauseSeconds = pause,
                        endingMark = mark
                    )
                )
            } else {
                val wordSplitPieces = splitLongPieceAtSpaces(piece, MAX_CHUNK_CHARS)
                wordSplitPieces.forEachIndexed { idx, subPiece ->
                    val isLastSubPiece = (idx == wordSplitPieces.lastIndex)
                    finalChunks.add(
                        TextChunk(
                            text = subPiece,
                            pauseSeconds = if (isLastSubPiece) pause else 0.0f,
                            endingMark = if (isLastSubPiece) mark else null
                        )
                    )
                }
            }
        }

        return finalChunks
    }

    fun pauseForMark(mark: Char?): Float {
        return when (mark) {
            '।' -> 0.40f
            '?', '!' -> 0.30f
            ',', '.', '\'' -> 0.20f
            else -> 0.0f
        }
    }

    /**
     * Splits a piece longer than [maxChars] strictly at space (word) boundaries.
     * Never splits inside a word.
     */
    internal fun splitLongPieceAtSpaces(piece: String, maxChars: Int): List<String> {
        val words = piece.split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= maxChars) {
                currentLine.append(' ').append(word)
            } else {
                result.add(currentLine.toString())
                currentLine.clear()
                currentLine.append(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }

        return result
    }

    /**
     * Merges very short comma/apostrophe sub-clauses (< 25 chars, such as "শুভ সন্ধ্যা,")
     * with the immediately following clause of the same sentence so VITS receives enough
     * phonetic context and avoids short-fragment cold-start duration instability.
     */
    fun mergeShortSubClausesForSynthesis(chunks: List<TextChunk>): List<TextChunk> {
        if (chunks.size <= 1) return chunks
        val merged = mutableListOf<TextChunk>()
        var i = 0
        while (i < chunks.size) {
            val current = chunks[i]
            val isShortMinorClause =
                (current.endingMark == ',' || current.endingMark == '\'') &&
                    current.text.length < 25
            if (isShortMinorClause && i + 1 < chunks.size) {
                val next = chunks[i + 1]
                val combinedText = "${current.text} ${next.text}"
                if (combinedText.length <= MAX_CHUNK_CHARS) {
                    merged.add(
                        TextChunk(
                            text = combinedText,
                            pauseSeconds = next.pauseSeconds,
                            endingMark = next.endingMark
                        )
                    )
                    i += 2
                    continue
                }
            }
            merged.add(current)
            i++
        }
        return merged
    }

    private val DIGIT_COMMA_REGEX = Regex("([০-৯0-9]),([০-৯0-9])")
    private val DIGIT_DECIMAL_REGEX = Regex("([০-৯0-9])\\.([০-৯0-9])")

    /**
     * Prepares a chunk's text right before passing to Sherpa-ONNX VITS `tts.generate(...)`:
     * 1. Normalizes numeric commas (e.g. ১২,৫০০ -> ১২৫০০) so `,` inside numbers is not treated as a clause pause.
     * 2. Normalizes numeric decimals (e.g. ১২.৫ -> ১২ দশমিক ৫) so `.` inside numbers does not stutter.
     * 3. Strips trailing sentence/clause split marks (। ? ! . , ') so they do not collide with VITS end-of-sequence frames.
     */
    fun preparePieceForSynthesis(pieceText: String): String {
        var cleaned = pieceText.trim()
        if (cleaned.isEmpty()) return cleaned

        // Repeat digit comma removal in case of multiple commas (e.g. ১,০০,০০০)
        while (DIGIT_COMMA_REGEX.containsMatchIn(cleaned)) {
            cleaned = DIGIT_COMMA_REGEX.replace(cleaned, "$1$2")
        }
        cleaned = DIGIT_DECIMAL_REGEX.replace(cleaned, "$1 দশমিক $2")

        while (cleaned.isNotEmpty() && (cleaned.last() in SPLIT_MARKS || cleaned.last().isWhitespace())) {
            cleaned = cleaned.dropLast(1)
        }
        return cleaned.trim().ifEmpty { pieceText.trim() }
    }
}

