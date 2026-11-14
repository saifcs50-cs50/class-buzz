package com.saif.classbuzz.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class IcsEvent(
    val uid: String,
    val start: Instant,
    val end: Instant,
    val summary: String,
    val isAllDay: Boolean,
    val isCancelled: Boolean,
)

/** Reads the VEVENTs out of an iCal (.ics) file, the way Google Calendar writes them. */
object IcsParser {
    private val DEFAULT_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val ESCAPE = Regex("""\\([\\;,nN])""")

    private data class Property(val params: Map<String, String>, val value: String)

    /** True only for a whole calendar: guards against login pages and downloads cut off part way. */
    fun isCompleteCalendar(text: String): Boolean =
        text.trimStart().startsWith("BEGIN:VCALENDAR") && text.contains("END:VCALENDAR")

    fun parse(ics: String): List<IcsEvent> {
        val lines = unfold(ics)
        val calendarZone = lines.firstOrNull { it.startsWith("X-WR-TIMEZONE:") }
            ?.let { zoneOrNull(it.substringAfter(':')) }
            ?: DEFAULT_ZONE

        val events = mutableListOf<IcsEvent>()
        var properties: MutableMap<String, Property>? = null
        for (line in lines) {
            val open = properties
            when {
                line == "BEGIN:VEVENT" -> properties = mutableMapOf()
                line == "END:VEVENT" -> {
                    open?.let { toEvent(it, calendarZone) }?.let(events::add)
                    properties = null
                }
                open != null -> parseLine(line)?.let { (name, property) -> open.putIfAbsent(name, property) }
            }
        }
        return events
    }

    /** A line starting with a space or tab continues the one before it. */
    private fun unfold(ics: String): List<String> {
        val lines = mutableListOf<String>()
        for (raw in ics.split("\r\n", "\n")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && lines.isNotEmpty()) {
                lines[lines.lastIndex] = lines.last() + raw.substring(1)
            } else {
                lines.add(raw)
            }
        }
        return lines
    }

    /** `NAME;PARAM=x;PARAM=y:value` into its name and property. */
    private fun parseLine(line: String): Pair<String, Property>? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).mapNotNull { param ->
            val equals = param.indexOf('=')
            if (equals <= 0) null else param.substring(0, equals).uppercase() to param.substring(equals + 1).trim('"')
        }.toMap()
        return head.first().uppercase() to Property(params, line.substring(colon + 1))
    }

    private fun toEvent(properties: Map<String, Property>, calendarZone: ZoneId): IcsEvent? {
        val uid = properties["UID"]?.value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startProperty = properties["DTSTART"] ?: return null
        val start = parseTime(startProperty, calendarZone) ?: return null
        return IcsEvent(
            uid = uid,
            start = start,
            end = properties["DTEND"]?.let { parseTime(it, calendarZone) } ?: start,
            summary = unescape(properties["SUMMARY"]?.value.orEmpty()),
            isAllDay = isDateOnly(startProperty),
            isCancelled = properties["STATUS"]?.value?.trim().equals("CANCELLED", ignoreCase = true),
        )
    }

    private fun isDateOnly(property: Property): Boolean =
        property.params["VALUE"].equals("DATE", ignoreCase = true) || property.value.trim().length == 8

    private fun parseTime(property: Property, calendarZone: ZoneId): Instant? = runCatching {
        val value = property.value.trim()
        when {
            isDateOnly(property) ->
                LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay(calendarZone).toInstant()
            value.endsWith("Z") -> LocalDateTime.parse(value.dropLast(1), DATE_TIME).toInstant(ZoneOffset.UTC)
            else -> {
                val zone = property.params["TZID"]?.let(::zoneOrNull) ?: calendarZone
                LocalDateTime.parse(value, DATE_TIME).atZone(zone).toInstant()
            }
        }
    }.getOrNull()

    private fun zoneOrNull(id: String): ZoneId? = runCatching { ZoneId.of(id.trim()) }.getOrNull()

    private fun unescape(text: String): String =
        ESCAPE.replace(text) { match ->
            when (val escaped = match.groupValues[1]) {
                "n", "N" -> " "
                else -> escaped
            }
        }.trim()
}
