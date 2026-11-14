package com.saif.classbuzz.calendar

enum class Subject(val label: String, val short: String, val color: Int) {
    CHEMISTRY("Chemistry", "CHEM", 0xFF4ADE80.toInt()),
    PHYSICS("Physics", "PHY", 0xFF60A5FA.toInt()),
    MATHS("Maths", "MATHS", 0xFFFB923C.toInt()),
}

const val NEUTRAL_COLOR = 0xFFA1A1AA.toInt()

data class ClassInfo(
    val subject: Subject?,
    val topic: String,
    val teachers: List<String>,
    val batchTag: String?,
    val isTest: Boolean,
    val isNoBuzz: Boolean,
)

/**
 * Reads a coaching-class title format, e.g. `ABC;Chem-Ionic Equilibrium-3. pH;Batch-Batch A`
 * (teacher initials; subject-topic; batch). Never fails: odd titles keep their text as the topic.
 */
object ClassTitle {
    private val BATCH_INLINE = Regex("""(?i)^batch\s*-\s*(.+)$""")
    private val TEACHERS = Regex("""^[A-Z]{2,4}(/[A-Z]{2,4})*$""")
    private val SUBJECT_PREFIX = Regex("""(?i)^(chemistry|chem|physics|phy|mathematics|maths|math)\b\s*-?\s*(.*)$""")
    private val TEST = Regex("""(?i)\b(test|exam)\b""")
    private val NO_BUZZ = Regex("""(?i)holiday|cancel""")

    fun parse(summary: String): ClassInfo {
        val parts = summary.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val (batchTag, withoutBatch) = takeBatch(parts)
        val teachers = withoutBatch.takeWhile { TEACHERS.matches(it) }
        val rest = withoutBatch.drop(teachers.size)
        val (subject, topic) = subjectAndTopic(rest)
        return ClassInfo(
            subject = subject,
            topic = topic ?: rest.joinToString(" · ").ifEmpty { summary.trim() },
            teachers = teachers.flatMap { it.split('/') },
            batchTag = batchTag,
            isTest = TEST.containsMatchIn(summary),
            isNoBuzz = NO_BUZZ.containsMatchIn(summary),
        )
    }

    /** Pulls out `Batch-NAME`, or `Batch` followed by `NAME` as the next part. */
    private fun takeBatch(parts: List<String>): Pair<String?, List<String>> {
        parts.forEachIndexed { index, part ->
            BATCH_INLINE.find(part)?.let { match ->
                return match.groupValues[1].trim() to parts.filterIndexed { i, _ -> i != index }
            }
            if (part.equals("batch", ignoreCase = true) && index + 1 < parts.size) {
                return parts[index + 1] to parts.filterIndexed { i, _ -> i != index && i != index + 1 }
            }
        }
        return null to parts
    }

    /**
     * The subject and the topic, when a subject is named. A null topic means "no subject found",
     * so the caller falls back to the whole text. An empty topic means the title named only a subject.
     */
    private fun subjectAndTopic(parts: List<String>): Pair<Subject?, String?> {
        val first = parts.firstOrNull() ?: return null to null
        SUBJECT_PREFIX.find(first)?.let { match ->
            val inline = match.groupValues[2].trim()
            val topic = inline.ifEmpty { parts.drop(1).joinToString(" · ") }
            return subjectFor(match.groupValues[1]) to topic
        }
        val named = parts.indexOfFirst { SUBJECT_PREFIX.find(it)?.groupValues?.get(2)?.isEmpty() == true }
        if (named < 0) return null to null
        val topic = parts.filterIndexed { i, _ -> i != named }.joinToString(" · ")
        return subjectFor(parts[named]) to topic
    }

    private fun subjectFor(word: String): Subject? = when (word.lowercase()) {
        "chem", "chemistry" -> Subject.CHEMISTRY
        "phy", "physics" -> Subject.PHYSICS
        "math", "maths", "mathematics" -> Subject.MATHS
        else -> null
    }
}
