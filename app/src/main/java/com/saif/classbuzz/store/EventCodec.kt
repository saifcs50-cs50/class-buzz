package com.saif.classbuzz.store

import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.calendar.ClassInfo
import com.saif.classbuzz.calendar.Subject
import java.time.Instant

/** The saved schedule as plain text: one event per line, tab-separated. */
object EventCodec {
    private const val FIELDS = 9
    private val WHITESPACE_BREAKS = Regex("[\t\r\n]")

    fun encode(events: List<ClassEvent>): String = events.joinToString("\n") { event ->
        val info = event.info
        listOf(
            event.uid,
            event.start.epochSecond.toString(),
            event.end.epochSecond.toString(),
            info.subject?.name.orEmpty(),
            info.topic,
            info.teachers.joinToString("/"),
            info.batchTag.orEmpty(),
            info.isTest.toString(),
            info.isNoBuzz.toString(),
        ).joinToString("\t") { clean(it) }
    }

    fun decode(text: String): List<ClassEvent> = text.lines().mapNotNull(::decodeLine)

    private fun decodeLine(line: String): ClassEvent? {
        val fields = line.split('\t')
        if (fields.size != FIELDS) return null
        val start = fields[1].toLongOrNull() ?: return null
        val end = fields[2].toLongOrNull() ?: return null
        val subject = fields[3].takeIf { it.isNotEmpty() }?.let { name -> Subject.entries.find { it.name == name } }
        return ClassEvent(
            uid = fields[0],
            start = Instant.ofEpochSecond(start),
            end = Instant.ofEpochSecond(end),
            info = ClassInfo(
                subject = subject,
                topic = fields[4],
                teachers = fields[5].split('/').filter { it.isNotEmpty() },
                batchTag = fields[6].takeIf { it.isNotEmpty() },
                isTest = fields[7].toBooleanStrictOrNull() ?: return null,
                isNoBuzz = fields[8].toBooleanStrictOrNull() ?: return null,
            ),
        )
    }

    private fun clean(text: String): String = WHITESPACE_BREAKS.replace(text, " ")
}
