package com.pransetu.app.core.ai

data class ParsedDistressTriage(
    val rawText: String,
    val detectedLanguage: String,
    val peopleCount: Int,
    val isMedicalUrgent: Boolean,
    val hazardType: String,
    val vulnerabilities: List<String>,
    val urgencyScore: Int, // 1 to 10 scale
    val structuredSummary: String
)

/**
 * Intelligent On-Device NLP & Voice Triage Engine.
 * Extracts critical disaster parameters (headcount, medical urgency, hazard type)
 * from spoken voice or typed text in Indian regional languages (Odia, Hindi, Bengali, Telugu, English).
 */
object VoiceDistressParser {

    private val odiaNumbers = mapOf(
        "୧" to 1, "୨" to 2, "୩" to 3, "୪" to 4, "୫" to 5, "୬" to 6, "୭" to 7, "୮" to 8, "୯" to 9, "୧୦" to 10,
        "ଗୋଟିଏ" to 1, "ଦୁଇ" to 2, "ଦୁଇଜଣ" to 2, "ତିନି" to 3, "ତିନିଜଣ" to 3, "ଚାରି" to 4, "ଚାରିଜଣ" to 4, "ପାଞ୍ଚ" to 5, "ପାଞ୍ଚଜଣ" to 5
    )

    private val hindiNumbers = mapOf(
        "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4, "पाँच" to 5, "पांच" to 5, "छह" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10
    )

    private val bengaliNumbers = mapOf(
        "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5, "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10
    )

    private val medicalKeywords = listOf(
        // English
        "bleed", "blood", "injury", "injured", "hurt", "fracture", "broken", "unconscious", "fainted", "choking",
        "heart attack", "pregnant", "breathing", "burn", "snake", "bite", "dying", "wound",
        // Odia
        "ରକ୍ତ", "ଆଘାତ", "ଭଙ୍ଗା", "ବେହୋସ", "ସାପ", "କାମୁଡ଼ି", "ଶ୍ୱାସ", "ଗର୍ଭବତୀ", "ଚିକିତ୍ସା",
        // Hindi
        "खून", "रक्त", "चोट", "घायल", "टूटा", "बेहोश", "साँप", "सांप", "काटा", "सांस", "गर्भवती", "इलाज",
        // Bengali
        "রক্ত", "আহত", "ভাঙা", "অজ্ঞান", "সাপ", "কামড়ে", "শ্বাস", "গর্ভবতী",
        // Telugu
        "రక్తం", "గాయం", "విరిగిన", "స్పృహ", "పాము", "కాటు", "శ్వాస", "గర్భిణీ"
    )

    private val hazardKeywords = mapOf(
        "FLOOD" to listOf("flood", "water", "drown", "submerged", "roof", "river", "surge", "ବନ୍ୟା", "ପାଣି", "ନଦୀ", "बाढ़", "पानी", "বন্যা", "వరద"),
        "CYCLONE" to listOf("cyclone", "wind", "storm", "gale", "tree", "roof blown", "ବାତ୍ୟା", "ଝଡ଼", "तूफान", "चक्रवात", "ঘূর্ণিঝড়", "తుఫాను"),
        "COLLAPSE" to listOf("collapse", "trapped", "debris", "rubble", "wall", "ଭୁଶୁଡ଼ି", "ଦବି", "दब", "मलबा", "দেওয়াল", "కూలి"),
        "FIRE" to listOf("fire", "smoke", "burn", "flame", "ନିଆଁ", "ଆଗ", "আগুন", "మంట"),
        "LANDSLIDE" to listOf("landslide", "mud", "hill", "ପାହାଡ଼", "भूस्खलन", "ধস", "కొండచరియలు")
    )

    private val vulnerabilityKeywords = mapOf(
        "INFANT/BABY" to listOf("baby", "infant", "newborn", "ଶିଶୁ", "ଛୋଟ ପିଲା", "बच्चा", "শিশু", "శిశువు"),
        "ELDERLY" to listOf("elderly", "old", "senior", "grandpa", "grandma", "ବୃଦ୍ଧ", "ଜେଜେ", "बुजुर्ग", "বৃদ্ধ", "వృద్ధులు"),
        "DISABLED" to listOf("disabled", "wheelchair", "blind", "handicap", "ଦିବ୍ୟାଙ୍ଗ", "दिव्यांग", "প্রতিবন্ধী", "దివ్యాంగులు"),
        "PREGNANT" to listOf("pregnant", "pregnancy", "labor", "ଗର୍ଭବତୀ", "गर्भवती", "গর্ভবতী", "గర్భిణీ")
    )

    fun parse(text: String): ParsedDistressTriage {
        val lower = text.lowercase().trim()
        val detectedLanguage = detectLanguage(lower)

        // Extract Headcount
        var peopleCount = 1
        val digitMatch = Regex("""\b(\d+)\s*(?:people|persons|members|people's|us|men|women|kids|children|heads|ଜଣ|ଲୋକ|लोग|জন|మంది)?""", RegexOption.IGNORE_CASE).find(lower)
        if (digitMatch != null) {
            peopleCount = digitMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: 1
        } else {
            // Check native words
            for ((word, count) in odiaNumbers + hindiNumbers + bengaliNumbers) {
                if (lower.contains(word)) {
                    peopleCount = count
                    break
                }
            }
        }

        // Extract Medical Urgency
        val isMedicalUrgent = medicalKeywords.any { lower.contains(it) }

        // Extract Hazard Type
        var detectedHazard = "GENERAL_EMERGENCY"
        for ((hazard, keywords) in hazardKeywords) {
            if (keywords.any { lower.contains(it) } ) {
                detectedHazard = hazard
                break
            }
        }

        // Extract Vulnerabilities
        val vulnerabilities = mutableListOf<String>()
        for ((vuln, keywords) in vulnerabilityKeywords) {
            if (keywords.any { lower.contains(it) } ) {
                vulnerabilities.add(vuln)
            }
        }

        // Compute AI Urgency Score (1 to 10)
        var score = 5
        if (isMedicalUrgent) score += 3
        if (vulnerabilities.isNotEmpty()) score += 2
        if (peopleCount >= 5) score += 1
        if (detectedHazard in listOf("FLOOD", "COLLAPSE", "FIRE")) score += 1
        val finalScore = score.coerceIn(1, 10)

        // Build Structured Summary
        val summaryParts = mutableListOf<String>()
        summaryParts.add("[$detectedHazard]")
        summaryParts.add("Headcount: $peopleCount")
        if (isMedicalUrgent) summaryParts.add("🚑 Medical Urgent")
        if (vulnerabilities.isNotEmpty()) summaryParts.add("⚠️ Vulnerable: ${vulnerabilities.joinToString(", ")}")
        summaryParts.add("Urgency: $finalScore/10")
        summaryParts.add("Transcript: \"$text\"")

        return ParsedDistressTriage(
            rawText = text,
            detectedLanguage = detectedLanguage,
            peopleCount = peopleCount,
            isMedicalUrgent = isMedicalUrgent,
            hazardType = detectedHazard,
            vulnerabilities = vulnerabilities,
            urgencyScore = finalScore,
            structuredSummary = summaryParts.joinToString(" • ")
        )
    }

    private fun detectLanguage(text: String): String {
        return when {
            text.any { it in '\u0B00'..'\u0B7F' } -> "or" // Odia
            text.any { it in '\u0900'..'\u097F' } -> "hi" // Hindi / Devanagari
            text.any { it in '\u0980'..'\u09FF' } -> "bn" // Bengali / Assamese
            text.any { it in '\u0C00'..'\u0C7F' } -> "te" // Telugu
            text.any { it in '\u0B80'..'\u0BFF' } -> "ta" // Tamil
            text.any { it in '\u0C80'..'\u0CFF' } -> "kn" // Kannada
            text.any { it in '\u0D00'..'\u0D7F' } -> "ml" // Malayalam
            text.any { it in '\u0600'..'\u06FF' } -> "ur" // Urdu
            else -> "en"
        }
    }
}
