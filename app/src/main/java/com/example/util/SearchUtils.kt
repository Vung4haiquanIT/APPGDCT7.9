package com.example.util

import java.text.Normalizer

/**
 * Tiện ích hỗ trợ tìm kiếm nâng cao theo cơ chế từ khóa (tương tự Google Search)
 * và xếp hạng kết quả theo số lượng từ khóa khớp nhiều nhất hiển thị lên trước.
 *
 * Tính năng chính:
 * - Không phân biệt chữ hoa, chữ thường
 * - Không phân biệt dấu tiếng Việt (gõ không dấu tìm được có dấu và ngược lại)
 * - Tách truy vấn thành các từ khóa (keywords/tokens) độc lập
 * - Xếp hạng ưu tiên mục có SỐ LƯỢNG TỪ KHÓA KHỚP NHIỀU NHẤT lên đầu danh sách
 * - Thưởng điểm lớn cho việc khớp nguyên cụm từ (Exact phrase match)
 * - Khớp theo tiền tố từ, từ viết tắt (acronym: ví dụ "ctct", "gdct", "qdnd")
 * - Khớp liền không dấu cách (compact matching: ví dụ "dieulenh" -> "Điều lệnh")
 * - Tìm kiếm gần đúng (fuzzy matching) khi gõ sai sót nhẹ
 * - Trọng số theo trường: Tiêu đề > Phân loại/Chủ đề > Mô tả/Giải thích > Nội dung phụ
 */
object SearchUtils {

    data class Field(
        val text: String?,
        val weight: Double = 1.0
    )

    data class SearchRank(
        val matchedKeywordCount: Int,
        val score: Double,
        val totalKeywords: Int = 0
    ) : Comparable<SearchRank> {
        override fun compareTo(other: SearchRank): Int {
            // 1. Số từ khóa khớp nhiều nhất hiển thị lên trước
            val countCmp = other.matchedKeywordCount.compareTo(this.matchedKeywordCount)
            if (countCmp != 0) return countCmp
            // 2. Điểm liên quan tổng thể (khớp chính xác cụm từ, trọng số tiêu đề, độ chính xác từ)
            return other.score.compareTo(this.score)
        }
    }

    private data class PreparedField(
        val rawText: String,
        val normText: String,
        val targetWords: List<String>,
        val compactText: String,
        val weight: Double
    )

    // Bảng tra cứu chuyển đổi trực tiếp toàn bộ ký tự tiếng Việt sang ký tự ASCII không dấu
    private val VIETNAMESE_ACCENT_MAP: Map<Char, Char> = buildMap {
        val aChars = "àáảãạăằắẳẵặâầấẩẫậÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬ"
        for (c in aChars) put(c, 'a')

        val dChars = "đĐ"
        for (c in dChars) put(c, 'd')

        val eChars = "èéẻẽẹêềếểễệÈÉẺẼẸÊỀẾỂỄỆ"
        for (c in eChars) put(c, 'e')

        val iChars = "ìíỉĩịÌÍỈĨỊ"
        for (c in iChars) put(c, 'i')

        val oChars = "òóỏõọôồốổỗộơờớởỡợÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢ"
        for (c in oChars) put(c, 'o')

        val uChars = "ùúủũụưừứửữựÙÚỦŨỤƯỪỨỬỮỰ"
        for (c in uChars) put(c, 'u')

        val yChars = "ỳýỷỹỵỲÝỶỸỴ"
        for (c in yChars) put(c, 'y')
    }

    /**
     * Loại bỏ toàn bộ dấu tiếng Việt và chuẩn hóa về chữ cái ASCII không dấu
     */
    fun removeAccents(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        val sb = StringBuilder(input.length)
        for (char in input) {
            val mapped = VIETNAMESE_ACCENT_MAP[char]
            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.append(char)
            }
        }

        val normalized = Normalizer.normalize(sb.toString(), Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{M}+"), "")
            .replace("đ", "d")
            .replace("Đ", "d")
            .replace("ơ", "o")
            .replace("Ơ", "o")
            .replace("ư", "u")
            .replace("Ư", "u")
    }

    /**
     * Làm sạch và chuẩn hóa chuỗi phục vụ tìm kiếm:
     * Chuyển thường, bỏ dấu, thay các ký tự đặc biệt/dấu câu thành khoảng trắng đơn.
     */
    fun normalizeForSearch(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val withoutAccents = removeAccents(input.lowercase())
        return withoutAccents
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Tính khoảng cách Levenshtein giữa 2 chuỗi để hỗ trợ tìm kiếm gần đúng (fuzzy)
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Kiểm tra một từ khóa (token) có khớp gần đúng với bất kỳ từ nào trong văn bản mục tiêu không
     */
    fun matchesTokenFuzzy(token: String, targetWords: List<String>): Boolean {
        if (token.isEmpty()) return true
        if (targetWords.isEmpty()) return false

        for (word in targetWords) {
            if (word == token || (token.length >= 3 && word.startsWith(token))) {
                return true
            }
        }

        // Chỉ áp dụng khoảng cách Levenshtein cho từ có độ dài từ 5 ký tự trở lên để tránh nhầm từ tiếng Việt ngắn
        val maxAllowedDistance = when {
            token.length >= 8 -> 2
            token.length >= 5 -> 1
            else -> 0
        }

        if (maxAllowedDistance > 0) {
            for (word in targetWords) {
                if (Math.abs(word.length - token.length) <= maxAllowedDistance) {
                    if (levenshteinDistance(token, word) <= maxAllowedDistance) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Kiểm tra khớp từ viết tắt (acronym). Ví dụ: "qdnd" -> "Quân đội nhân dân", "ctct" -> "Công tác chính trị"
     */
    fun matchesAcronym(queryNorm: String, targetWords: List<String>): Boolean {
        if (queryNorm.length < 2 || targetWords.isEmpty()) return false
        val acronym = targetWords.mapNotNull { it.firstOrNull() }.joinToString("")
        return acronym.contains(queryNorm)
    }

    /**
     * Tính điểm xếp hạng tìm kiếm tương tự thuật toán Google Search:
     * - Tách truy vấn thành các từ khóa (keywords/tokens)
     * - Đếm số từ khóa riêng biệt khớp được trong văn bản mục tiêu (matchedKeywordCount)
     * - Tính điểm liên quan (score) dựa trên:
     *   + Khớp nguyên cụm từ (Exact phrase match)
     *   + Khớp chính xác từ (Exact word match)
     *   + Khớp tiền tố (Prefix match)
     *   + Khớp từ viết tắt (Acronym match)
     *   + Khớp liền không cách (Compact match)
     *   + Trọng số của từng trường (Tiêu đề > Mô tả > Chi tiết)
     *   + Khớp có dấu gốc (Accent match bonus)
     *   + Độ bao phủ từ khóa (Toàn bộ từ khóa khớp -> cộng điểm lớn)
     */
    fun calculateRank(query: String?, fields: List<Field>): SearchRank {
        if (query.isNullOrBlank()) {
            return SearchRank(0, 0.0, 0)
        }

        val cleanQuery = query.trim()
        val normQuery = normalizeForSearch(cleanQuery)
        val compactQuery = normQuery.replace(" ", "")

        val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }.distinct()
        if (queryTokens.isEmpty()) {
            return SearchRank(0, 0.0, 0)
        }

        val rawTokens = cleanQuery.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.isNotBlank() }.distinct()

        val preparedFields = fields.mapNotNull { f ->
            val text = f.text
            if (text.isNullOrBlank()) null
            else {
                val norm = normalizeForSearch(text)
                PreparedField(
                    rawText = text.lowercase(),
                    normText = norm,
                    targetWords = norm.split(" ").filter { it.isNotBlank() },
                    compactText = norm.replace(" ", ""),
                    weight = f.weight
                )
            }
        }

        if (preparedFields.isEmpty()) {
            return SearchRank(0, 0.0, queryTokens.size)
        }

        val matchedTokens = mutableSetOf<String>()
        var totalScore = 0.0

        // 1. Kiểm tra khớp cụm từ đầy đủ (Exact phrase match)
        for (field in preparedFields) {
            if (field.normText.contains(normQuery)) {
                totalScore += 120.0 * field.weight
                if (field.rawText.contains(cleanQuery, ignoreCase = true)) {
                    totalScore += 60.0 * field.weight
                }
                matchedTokens.addAll(queryTokens)
            } else if (compactQuery.length >= 4 && field.compactText.contains(compactQuery)) {
                totalScore += 70.0 * field.weight
                matchedTokens.addAll(queryTokens)
            }

            // Kiểm tra viết tắt toàn truy vấn (vd "qdnd" khớp "quan doi nhan dan")
            if (compactQuery.length >= 2 && matchesAcronym(compactQuery, field.targetWords)) {
                totalScore += 90.0 * field.weight
                matchedTokens.addAll(queryTokens)
            }
        }

        // 2. Chấm điểm từng từ khóa trong truy vấn
        for (token in queryTokens) {
            var tokenMatchedInAnyField = false
            var tokenScoreSum = 0.0

            for (field in preparedFields) {
                var fieldScore = 0.0

                // (a) Khớp chính xác từ nguyên vẹn
                val exactWordCount = field.targetWords.count { it == token }
                if (exactWordCount > 0) {
                    fieldScore = maxOf(fieldScore, (20.0 + (exactWordCount - 1) * 3.0) * field.weight)
                    tokenMatchedInAnyField = true
                }

                // (b) Khớp tiền tố (từ trong văn bản bắt đầu bằng token, yêu cầu độ dài >= 3)
                val prefixCount = if (token.length >= 3) field.targetWords.count { it.startsWith(token) && it != token } else 0
                if (prefixCount > 0) {
                    fieldScore = maxOf(fieldScore, (12.0 + (prefixCount - 1) * 2.0) * field.weight)
                    tokenMatchedInAnyField = true
                }

                // (c) Khớp liền không dấu cách (yêu cầu token >= 4 ký tự)
                if (token.length >= 4 && field.compactText.contains(token) && fieldScore == 0.0) {
                    fieldScore = maxOf(fieldScore, 8.0 * field.weight)
                    tokenMatchedInAnyField = true
                }

                // (d) Khớp gần đúng (fuzzy cho từ dài >= 5 ký tự)
                if (token.length >= 5 && fieldScore == 0.0 && matchesTokenFuzzy(token, field.targetWords)) {
                    fieldScore = maxOf(fieldScore, 5.0 * field.weight)
                    tokenMatchedInAnyField = true
                }

                // Thưởng thêm điểm nếu trùng khớp cả dấu tiếng Việt nguyên bản
                if (fieldScore > 0.0 && rawTokens.any { rawT -> field.rawText.contains(rawT) }) {
                    fieldScore += 3.0 * field.weight
                }

                tokenScoreSum += fieldScore
            }

            if (tokenMatchedInAnyField) {
                matchedTokens.add(token)
                totalScore += tokenScoreSum
            }
        }

        // 3. Thưởng tỷ lệ bao phủ từ khóa (Keyword coverage bonus)
        val matchedCount = matchedTokens.size
        if (matchedCount == queryTokens.size && queryTokens.isNotEmpty()) {
            // Khớp 100% tất cả các từ khóa trong truy vấn -> cộng điểm ưu tiên hàng đầu
            totalScore += 150.0
        } else if (queryTokens.size >= 3 && matchedCount >= (queryTokens.size * 0.67)) {
            // Khớp phần lớn từ khóa (>= 2/3)
            totalScore += 50.0
        }

        return SearchRank(
            matchedKeywordCount = matchedCount,
            score = totalScore,
            totalKeywords = queryTokens.size
        )
    }

    /**
     * Lọc và xếp hạng danh sách theo thuật toán Google:
     * - Chỉ hiển thị những bài khớp ít nhất 2 từ khóa tìm kiếm (nếu truy vấn có từ 2 từ khóa trở lên)
     * - Xếp hạng số từ khóa nhiều nhất hiển thị lên trước
     * - Các phần tử cùng số từ khóa sẽ xếp theo điểm liên quan (độ khớp chính xác, cụm từ, trọng số tiêu đề)
     * - Loại bỏ các phần tử không đạt ngưỡng số từ khóa
     * - Trả về danh sách gốc nếu query trống
     */
    fun <T> filterAndRank(
        items: List<T>,
        query: String?,
        minKeywords: Int? = null,
        extractFields: (T) -> List<Field>
    ): List<T> {
        if (query.isNullOrBlank()) return items

        val cleanQuery = query.trim()
        val normQuery = normalizeForSearch(cleanQuery)
        val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }.distinct()
        if (queryTokens.isEmpty()) return items

        // Quy tắc: Khi truy vấn có từ 2 từ khóa trở lên, bài phải khớp ít nhất 2 từ khóa tìm kiếm thì mới hiện lên
        // Khi truy vấn chỉ có 1 từ khóa, khớp ít nhất 1 từ khóa
        val threshold = minKeywords ?: if (queryTokens.size >= 2) 2 else 1

        val scoredList = mutableListOf<Pair<T, SearchRank>>()

        for (item in items) {
            val fields = extractFields(item)
            val rank = calculateRank(query, fields)
            if (rank.matchedKeywordCount >= threshold && rank.score > 0.0) {
                scoredList.add(item to rank)
            }
        }

        // Sắp xếp:
        // 1. Số từ khóa khớp nhiều nhất (matchedKeywordCount DESC)
        // 2. Điểm số liên quan tổng thể (score DESC)
        scoredList.sortWith(
            compareByDescending<Pair<T, SearchRank>> { it.second.matchedKeywordCount }
                .thenByDescending { it.second.score }
        )

        return scoredList.map { it.first }
    }

    /**
     * Kiểm tra khớp đơn giản giữa target và query (tương thích ngược)
     */
    fun matches(target: String?, query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        if (target.isNullOrBlank()) return false
        val cleanQuery = query.trim()
        val normQuery = normalizeForSearch(cleanQuery)
        val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }.distinct()
        if (queryTokens.isEmpty()) return true

        val threshold = if (queryTokens.size >= 2) 2 else 1
        val rank = calculateRank(query, listOf(Field(target)))
        return rank.matchedKeywordCount >= threshold && rank.score > 0.0
    }

    /**
     * Kiểm tra khớp với bất kỳ chuỗi nào trong targets (tương thích ngược)
     */
    fun matchesAny(query: String?, vararg targets: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val fields = targets.mapNotNull { it }.map { Field(it) }
        if (fields.isEmpty()) return false
        val cleanQuery = query.trim()
        val normQuery = normalizeForSearch(cleanQuery)
        val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }.distinct()
        if (queryTokens.isEmpty()) return true

        val threshold = if (queryTokens.size >= 2) 2 else 1
        val rank = calculateRank(query, fields)
        return rank.matchedKeywordCount >= threshold && rank.score > 0.0
    }

    /**
     * Kiểm tra khớp với bất kỳ chuỗi nào trong danh sách targets (tương thích ngược)
     */
    fun matchesAnyInList(query: String?, targets: List<String?>): Boolean {
        if (query.isNullOrBlank()) return true
        val fields = targets.mapNotNull { it }.map { Field(it) }
        if (fields.isEmpty()) return false
        val cleanQuery = query.trim()
        val normQuery = normalizeForSearch(cleanQuery)
        val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }.distinct()
        if (queryTokens.isEmpty()) return true

        val threshold = if (queryTokens.size >= 2) 2 else 1
        val rank = calculateRank(query, fields)
        return rank.matchedKeywordCount >= threshold && rank.score > 0.0
    }
}

