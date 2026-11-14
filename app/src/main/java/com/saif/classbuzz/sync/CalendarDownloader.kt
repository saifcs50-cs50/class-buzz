package com.saif.classbuzz.sync

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Plain HTTPS GET of the calendar file, with a cap on the whole download. The platform handles gzip. */
object CalendarDownloader {
    private const val TIMEOUT_MS = 15_000
    private const val WHOLE_DOWNLOAD_MS = 30_000L
    private const val BUFFER_BYTES = 16 * 1024

    fun download(url: String): Result<String> = runCatching {
        val deadline = System.currentTimeMillis() + WHOLE_DOWNLOAD_MS
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            connection.inputStream.use { readAll(it, deadline, System::currentTimeMillis) }
        } finally {
            connection.disconnect()
        }
    }

    /** Reads the whole stream as UTF-8, giving up once [deadlineMillis] has passed. */
    fun readAll(input: InputStream, deadlineMillis: Long, nowMillis: () -> Long): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            out.write(buffer, 0, count)
            if (nowMillis() > deadlineMillis) throw IOException("download took too long")
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
