package com.sn.snfilemanager.utils

import android.content.Context
import com.sn.snfilemanager.R
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class KotlinExtension {
    fun formatSizeFile(fileSize: Long): String {
        if (fileSize <= 0) {
            return "0 B"
        }
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(fileSize.toDouble()) / Math.log10(1024.0)).toInt()
        return (DecimalFormat("#,##0.#").format(fileSize / Math.pow(1024.0, digitGroups.toDouble()))
                + " "
                + units[digitGroups])
    }

    fun getFileSizeFormatted(fileSize: Long): String {
        return formatSizeFile(fileSize)
    }

    fun formatRecentFileTime(context: Context, timeInMillis: Long): String {
        val fileTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis * 1000), ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val minutesAgo = ChronoUnit.MINUTES.between(fileTime, now)
        val hoursAgo = ChronoUnit.HOURS.between(fileTime, now)

        return when {
            hoursAgo < 1 -> String.format(context.getString(R.string.minutes_ago), minutesAgo)
            hoursAgo < 24 -> String.format(context.getString(R.string.hours_ago), hoursAgo)
            else -> DateTimeFormatter.ofPattern("dd MMM yyyy").format(fileTime)
        }
    }
}