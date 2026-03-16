package chang.sllj.homeassetkeeper.camera

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

@Singleton
class BrandDictionary @Inject constructor() {

    fun findCandidates(rawText: String): List<BrandMatch> {
        val segments = buildSegments(rawText)
        if (segments.isEmpty()) return emptyList()

        return brands.asSequence()
            .mapNotNull { entry ->
                val bestAliasScore = entry.aliases.maxOfOrNull { alias ->
                    segments.maxOfOrNull { segment ->
                        scoreMatch(segment, alias)
                    } ?: Int.MIN_VALUE
                } ?: Int.MIN_VALUE

                if (bestAliasScore >= MIN_MATCH_SCORE) {
                    BrandMatch(entry.displayName, bestAliasScore)
                } else {
                    null
                }
            }
            .sortedByDescending { it.score }
            .distinctBy { it.brand.lowercase(Locale.ROOT) }
            .take(MAX_MATCHES)
            .toList()
    }

    private fun buildSegments(rawText: String): List<String> {
        val full = normalize(rawText)
        val tokens = rawText
            .split(tokenSplitRegex)
            .map(::normalize)
            .filter { it.length >= 2 }

        val segments = linkedSetOf<String>()
        if (full.length >= 2) segments += full

        tokens.forEach { segments += it }

        for (windowSize in 2..3) {
            tokens.windowed(windowSize, partialWindows = false).forEach { window ->
                val joined = window.joinToString("")
                if (joined.length >= 3) segments += joined
            }
        }

        return segments.toList()
    }

    private fun scoreMatch(candidate: String, alias: String): Int {
        if (candidate.isBlank() || alias.isBlank()) return Int.MIN_VALUE
        if (candidate == alias) return 100

        if (candidate.contains(alias) && alias.length >= 3) {
            return 95 - (candidate.length - alias.length)
        }
        if (alias.contains(candidate) && candidate.length >= 3) {
            return 91 - (alias.length - candidate.length)
        }

        val maxDistance = when (max(candidate.length, alias.length)) {
            in 0..4 -> 1
            in 5..7 -> 2
            else -> 3
        }
        if (abs(candidate.length - alias.length) > maxDistance) return Int.MIN_VALUE

        val distance = levenshtein(candidate, alias)
        if (distance > maxDistance) return Int.MIN_VALUE

        return 88 - (distance * 10) - abs(candidate.length - alias.length)
    }

    private fun normalize(text: String): String =
        text.uppercase(Locale.ROOT)
            .replace("&", "AND")
            .map(::normalizeChar)
            .filter { it.isLetterOrDigit() || isCjk(it) }
            .joinToString("")

    private fun normalizeChar(char: Char): Char = when (char) {
        '0' -> 'O'
        '1' -> 'I'
        '5' -> 'S'
        '8' -> 'B'
        else -> char
    }

    private fun isCjk(char: Char): Boolean =
        char.code in 0x3400..0x9FFF || char.code in 0xF900..0xFAFF

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)

        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitution = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + substitution
                )
            }
            for (j in previous.indices) {
                previous[j] = current[j]
            }
        }

        return previous[b.length]
    }

    data class BrandMatch(
        val brand: String,
        val score: Int
    )

    private data class BrandEntry(
        val displayName: String,
        val aliases: List<String>
    )

    companion object {
        private const val MIN_MATCH_SCORE = 72
        private const val MAX_MATCHES = 3
        private val tokenSplitRegex = Regex("""[\s\-_./\\|,:;()]+""")

        private val brands = listOf(
            entry("Acer", "宏碁"),
            entry("AOC"),
            entry("Apple"),
            entry("Ariston", "阿里斯頓"),
            entry("ASUS", "華碩"),
            entry("Beko", "倍科"),
            entry("BenQ", "明基"),
            entry("Black & Decker", "百得"),
            entry("Blaupunkt", "藍寶"),
            entry("Bosch", "博世"),
            entry("Bose", "博士"),
            entry("Brother"),
            entry("Canon", "佳能"),
            entry("Carrier", "開利"),
            entry("Casio", "卡西歐"),
            entry("Daikin", "大金"),
            entry("De'Longhi", "德龍"),
            entry("Dell"),
            entry("Dyson"),
            entry("Electrolux", "伊萊克斯"),
            entry("Epson"),
            entry("Frigidaire"),
            entry("Fujifilm", "富士"),
            entry("Fujitsu", "富士通"),
            entry("GE", "General Electric", "通用電氣"),
            entry("Google"),
            entry("Gree", "格力"),
            entry("Haier", "海爾"),
            entry("Hisense", "海信"),
            entry("Hitachi", "日立"),
            entry("Honeywell", "漢威聯合"),
            entry("HP", "Hewlett Packard"),
            entry("Huawei", "華為"),
            entry("Indesit", "意黛喜"),
            entry("JBL"),
            entry("JVC"),
            entry("KitchenAid"),
            entry("Lenovo", "聯想"),
            entry("LG", "樂金"),
            entry("Liebherr", "利勃海爾"),
            entry("Logitech", "羅技"),
            entry("Makita", "牧田"),
            entry("Maytag"),
            entry("Midea", "美的"),
            entry("Miele"),
            entry("Mitsubishi Electric", "三菱電機", "Mitsubishi"),
            entry("Nespresso"),
            entry("NEXTBASE"),
            entry("Nikon", "尼康"),
            entry("Nintendo", "任天堂"),
            entry("Nokia"),
            entry("Olympus", "奧林巴斯"),
            entry("OPPO"),
            entry("Panasonic", "國際牌", "松下"),
            entry("Philips", "飛利浦"),
            entry("Pioneer", "先鋒"),
            entry("Ricoh", "理光"),
            entry("Rinnai", "林內"),
            entry("Sakura", "櫻花"),
            entry("Samsung", "三星"),
            entry("Sanyo", "三洋"),
            entry("SANLUX", "台灣三洋"),
            entry("Sennheiser", "森海塞爾"),
            entry("Sharp", "夏普"),
            entry("Siemens", "西門子"),
            entry("Sony", "索尼"),
            entry("Sub-Zero"),
            entry("Tatung", "大同"),
            entry("TCL"),
            entry("Tefal", "特福"),
            entry("Tesla", "特斯拉"),
            entry("Toshiba", "東芝"),
            entry("TP-Link"),
            entry("UNIQLO"),
            entry("VIGORPLUS+"),
            entry("ViewSonic", "優派"),
            entry("vivo"),
            entry("Whirlpool"),
            entry("Xiaomi", "小米"),
            entry("Yamaha", "山葉"),
            entry("Zanussi", "金章")
        )

        private fun entry(displayName: String, vararg aliases: String): BrandEntry {
            val normalizedAliases = buildList {
                add(displayName)
                addAll(aliases)
            }.map {
                it.uppercase(Locale.ROOT)
                    .replace("&", "AND")
                    .map { char ->
                        when (char) {
                            '0' -> 'O'
                            '1' -> 'I'
                            '5' -> 'S'
                            '8' -> 'B'
                            else -> char
                        }
                    }
                    .filter { char -> char.isLetterOrDigit() || char.code in 0x3400..0x9FFF || char.code in 0xF900..0xFAFF }
                    .joinToString("")
            }.distinct()

            return BrandEntry(
                displayName = displayName,
                aliases = normalizedAliases
            )
        }
    }
}
