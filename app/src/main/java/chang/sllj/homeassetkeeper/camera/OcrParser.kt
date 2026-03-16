package chang.sllj.homeassetkeeper.camera

import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class OcrParser @Inject constructor(
    private val brandDictionary: BrandDictionary
) {

    fun parseBrandCandidates(recognitions: List<OcrRecognition>): List<String> {
        val aggregatedScores = linkedMapOf<String, BrandAggregate>()

        extractLineObservations(recognitions).forEach { observation ->
            val candidate = normalizeLine(observation.text)
            if (candidate.isBlank()) return@forEach

            if (isBrandCandidate(candidate)) {
                addBrandCandidate(
                    aggregatedScores = aggregatedScores,
                    key = candidate.lowercase(),
                    displayText = candidate,
                    score = scoreBrandCandidate(candidate, observation),
                    preferredDisplay = false
                )
            }

            brandDictionary.findCandidates(candidate).forEach { match ->
                addBrandCandidate(
                    aggregatedScores = aggregatedScores,
                    key = match.brand.lowercase(),
                    displayText = match.brand,
                    score = scoreBrandCandidate(match.brand, observation) + match.score + 18,
                    preferredDisplay = true
                )
            }
        }

        return aggregatedScores.values
            .sortedWith(
                compareByDescending<BrandAggregate> { it.totalScore }
                    .thenByDescending { it.voteCount }
                    .thenBy { it.displayText.length }
            )
            .map { it.displayText }
            .take(MAX_BRAND_CANDIDATES)
    }

    private fun addBrandCandidate(
        aggregatedScores: MutableMap<String, BrandAggregate>,
        key: String,
        displayText: String,
        score: Int,
        preferredDisplay: Boolean
    ) {
        val aggregate = aggregatedScores[key]
        if (aggregate == null) {
            aggregatedScores[key] = BrandAggregate(
                displayText = displayText,
                totalScore = score,
                voteCount = 1,
                preferredDisplay = preferredDisplay
            )
            return
        }

        aggregatedScores[key] = aggregate.copy(
            displayText = choosePreferredDisplay(
                existing = aggregate.displayText,
                candidate = displayText,
                existingPreferred = aggregate.preferredDisplay,
                candidatePreferred = preferredDisplay
            ),
            totalScore = aggregate.totalScore + score,
            voteCount = aggregate.voteCount + 1,
            preferredDisplay = aggregate.preferredDisplay || preferredDisplay
        )
    }

    fun parsePurchaseDateCandidates(recognitions: List<OcrRecognition>): List<Long> {
        val today = LocalDate.now(ZoneOffset.UTC)

        val aggregatedScores = linkedMapOf<LocalDate, DateAggregate>()
        extractLineObservations(recognitions).forEach { observation ->
            val line = normalizeLine(observation.text)
            parseDateFromLine(line)?.takeIf { !it.isAfter(today.plusDays(7)) }?.let { date ->
                val score = scoreDateCandidate(line, observation)
                val aggregate = aggregatedScores[date]
                if (aggregate == null) {
                    aggregatedScores[date] = DateAggregate(
                        date = date,
                        totalScore = score,
                        voteCount = 1,
                        bestLineIndex = observation.lineIndex
                    )
                } else {
                    aggregatedScores[date] = aggregate.copy(
                        totalScore = aggregate.totalScore + score,
                        voteCount = aggregate.voteCount + 1,
                        bestLineIndex = minOf(aggregate.bestLineIndex, observation.lineIndex)
                    )
                }
            }
        }

        return aggregatedScores.values
            .sortedWith(
                compareByDescending<DateAggregate> { it.totalScore }
                    .thenByDescending { it.voteCount }
                    .thenBy { it.bestLineIndex }
                    .thenByDescending { it.date }
            )
            .take(MAX_DATE_CANDIDATES)
            .map {
                it.date.atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
    }

    private fun parseDateFromLine(line: String): LocalDate? {
        yearFirstRegex.find(line)?.destructured?.let { (year, month, day) ->
            return parseDate(year.toInt(), month.toInt(), day.toInt())
        }

        yearFirstZhRegex.find(line)?.destructured?.let { (year, month, day) ->
            return parseDate(year.toInt(), month.toInt(), day.toInt())
        }

        monthDayYearRegex.find(line)?.destructured?.let { (first, second, year) ->
            val a = first.toInt()
            val b = second.toInt()
            if (a <= 12 && b > 12) return parseDate(year.toInt(), a, b)
            if (a > 12 && b <= 12) return parseDate(year.toInt(), b, a)
        }

        return null
    }

    private fun parseDate(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()

    private fun normalizeLine(raw: String): String =
        raw.replace("\\s+".toRegex(), " ")
            .replace("：", ":")
            .trim()

    private fun isBrandCandidate(candidate: String): Boolean {
        if (candidate.length !in 2..32) return false
        if (candidate.count(Char::isDigit) > candidate.length / 3) return false
        if (candidate.any { it == '$' || it == ':' }) return false
        if (yearFirstRegex.containsMatchIn(candidate) || yearFirstZhRegex.containsMatchIn(candidate)) return false

        val lower = candidate.lowercase()
        if (brandStopWords.any(lower::contains)) return false

        val letters = candidate.count(Char::isLetter)
        return letters >= 2 || containsCjk(candidate) || containsDevanagari(candidate)
    }

    private fun scoreBrandCandidate(candidate: String, observation: LineObservation): Int {
        val letters = candidate.count(Char::isLetter)
        val digits = candidate.count(Char::isDigit)
        val spaces = candidate.count { it == ' ' }
        val uppercaseBonus = if (candidate.any(Char::isUpperCase)) 6 else 0
        val compactBonus = if (spaces <= 2) 4 else 0
        val cleanBonus = if (brandShapeRegex.matches(candidate)) 10 else 0
        val scriptBonus = scriptBonus(candidate, observation.script)
        val confidenceBonus = (observation.confidence * 10f).roundToInt()
        val languageBonus = if (observation.languageTag != "und") 2 else 0
        return (letters * 2) - (digits * 3) - spaces +
            uppercaseBonus + compactBonus + cleanBonus +
            scriptBonus + confidenceBonus + languageBonus
    }

    private fun purchaseDateLabelScore(line: String): Int {
        val lower = line.lowercase()
        return when {
            purchaseDateKeywords.any(lower::contains) -> 3
            genericDateKeywords.any(lower::contains) -> 1
            else -> 0
        }
    }

    private fun scoreDateCandidate(line: String, observation: LineObservation): Int {
        val labelScore = purchaseDateLabelScore(line) * 4
        val confidenceBonus = (observation.confidence * 10f).roundToInt()
        val earlyLineBonus = maxOf(0, 8 - observation.lineIndex)
        return labelScore + confidenceBonus + earlyLineBonus
    }

    private fun extractLineObservations(recognitions: List<OcrRecognition>): List<LineObservation> {
        val observations = mutableListOf<LineObservation>()
        recognitions.forEach { recognition ->
            recognition.text.textBlocks.forEachIndexed { blockIndex, block ->
                block.lines.forEachIndexed { lineIndex, line ->
                    observations += LineObservation(
                        text = line.text,
                        script = recognition.script,
                        languageTag = line.recognizedLanguage,
                        confidence = line.confidence.takeIf { it > 0f } ?: 0f,
                        blockIndex = blockIndex,
                        lineIndex = lineIndex
                    )
                }
            }
        }
        return observations
    }

    private fun scriptBonus(candidate: String, script: OcrScript): Int = when {
        containsDevanagari(candidate) && script == OcrScript.DEVANAGARI -> 16
        containsJapaneseKana(candidate) && script == OcrScript.JAPANESE -> 16
        containsHangul(candidate) && script == OcrScript.KOREAN -> 16
        containsHan(candidate) && script == OcrScript.CHINESE -> 12
        containsLatin(candidate) && script == OcrScript.LATIN -> 8
        else -> 0
    }

    private fun containsLatin(text: String): Boolean =
        text.any { it in 'A'..'Z' || it in 'a'..'z' }

    private fun containsCjk(text: String): Boolean =
        containsHan(text) || containsJapaneseKana(text) || containsHangul(text)

    private fun containsHan(text: String): Boolean =
        text.any { it.code in 0x4E00..0x9FFF }

    private fun containsJapaneseKana(text: String): Boolean =
        text.any { it.code in 0x3040..0x30FF }

    private fun containsHangul(text: String): Boolean =
        text.any { it.code in 0xAC00..0xD7AF }

    private fun containsDevanagari(text: String): Boolean =
        text.any { it.code in 0x0900..0x097F }

    private fun choosePreferredDisplay(existing: String, candidate: String): String =
        if (candidate.length < existing.length) candidate else existing

    private fun choosePreferredDisplay(
        existing: String,
        candidate: String,
        existingPreferred: Boolean,
        candidatePreferred: Boolean
    ): String = when {
        candidatePreferred && !existingPreferred -> candidate
        !candidatePreferred && existingPreferred -> existing
        candidate.length < existing.length -> candidate
        else -> existing
    }

    private data class DateAggregate(
        val date: LocalDate,
        val totalScore: Int,
        val voteCount: Int,
        val bestLineIndex: Int
    )

    private data class BrandAggregate(
        val displayText: String,
        val totalScore: Int,
        val voteCount: Int,
        val preferredDisplay: Boolean
    )

    private data class LineObservation(
        val text: String,
        val script: OcrScript,
        val languageTag: String,
        val confidence: Float,
        val blockIndex: Int,
        val lineIndex: Int
    )

    companion object {
        private const val MAX_BRAND_CANDIDATES = 3
        private const val MAX_DATE_CANDIDATES = 3
        private val yearFirstRegex =
            Regex("""\b(19\d{2}|20\d{2})[./-](0?[1-9]|1[0-2])[./-](0?[1-9]|[12]\d|3[01])\b""")
        private val yearFirstZhRegex =
            Regex("""\b(19\d{2}|20\d{2})年(0?[1-9]|1[0-2])月(0?[1-9]|[12]\d|3[01])日?\b""")
        private val monthDayYearRegex =
            Regex("""\b(0?[1-9]|1[0-2]|[12]\d|3[01])[./-](0?[1-9]|1[0-2]|[12]\d|3[01])[./-](19\d{2}|20\d{2})\b""")
        private val brandShapeRegex =
            Regex("""^[A-Za-z][A-Za-z0-9&+\-.' ]{1,31}$""")

        private val purchaseDateKeywords = listOf(
            "purchase", "purchased", "invoice", "receipt", "transaction",
            "購買", "购买", "發票", "发票", "交易"
        )
        private val genericDateKeywords = listOf("date", "日期")
        private val brandStopWords = listOf(
            "receipt", "invoice", "purchase", "total", "subtotal", "tax",
            "qty", "quantity", "date", "time", "store", "cashier",
            "warranty", "serial", "model", "phone", "customer"
        )
    }
}
