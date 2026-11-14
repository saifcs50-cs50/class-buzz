package com.saif.classbuzz.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream

class CalendarDownloaderTest {

    /** A stream that hands out one byte per read and moves a fake clock forward each time. */
    private class SlowStream(private val bytes: ByteArray, private val clock: LongArray, private val stepMs: Long) :
        InputStream() {
        private var position = 0
        override fun read(): Int = read(ByteArray(1), 0, 1).let { if (it < 0) -1 else 0 }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= bytes.size) return -1
            clock[0] += stepMs
            buffer[offset] = bytes[position++]
            return 1
        }
    }

    @Test
    fun `whole body is read before the deadline`() {
        val clock = longArrayOf(0)
        val text = CalendarDownloader.readAll(SlowStream("BEGIN:VCALENDAR".toByteArray(), clock, 1), 1_000) { clock[0] }
        assertEquals("BEGIN:VCALENDAR", text)
    }

    @Test
    fun `a download that drags past the deadline is abandoned`() {
        val clock = longArrayOf(0)
        val slow = SlowStream(ByteArray(100) { 'x'.code.toByte() }, clock, 50)
        val failure = runCatching { CalendarDownloader.readAll(slow, 1_000) { clock[0] } }.exceptionOrNull()
        assertTrue(failure is IOException)
    }
}
