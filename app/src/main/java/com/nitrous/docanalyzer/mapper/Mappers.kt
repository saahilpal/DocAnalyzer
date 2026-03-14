package com.nitrous.docanalyzer.mapper

import android.text.format.DateUtils
import com.nitrous.docanalyzer.model.*
import com.nitrous.docanalyzer.network.dto.HistoryItemDto
import com.nitrous.docanalyzer.network.dto.SessionDto
import com.nitrous.docanalyzer.network.dto.UserDto
import java.text.SimpleDateFormat
import java.util.*

fun UserDto.toDomain(): User {
    return User(
        id = this.id ?: -1,
        name = this.name ?: "Unknown User",
        email = this.email ?: "",
        createdAt = this.normalizedCreatedAt,
        isActive = this.normalizedIsActive
    )
}

fun SessionDto.toModel(): ChatSession {
    val timestamp = this.normalizedCreatedAt.toTimestamp()
    val count = this.pdfCount ?: 0
    return ChatSession(
        id = this.id?.toString() ?: UUID.randomUUID().toString(),
        title = if (this.title.isNullOrBlank()) "Untitled Analysis" else this.title,
        createdAt = timestamp,
        displayDate = timestamp.toRelativeTimeSpan(),
        pdfCountBadge = if (count > 0) "$count document${if (count > 1) "s" else ""}" else "",
        lastMessagePreview = this.lastMessagePreview
    )
}

fun HistoryItemDto.toModel(): ChatMessage {
    val timestamp = this.createdAt.toTimestamp()
    val role = when (this.role?.lowercase()) {
        "assistant" -> MessageRole.ASSISTANT
        "system" -> MessageRole.SYSTEM
        else -> MessageRole.USER
    }
    
    return ChatMessage(
        id = this.id ?: UUID.randomUUID().toString(),
        sessionId = this.sessionId?.toString() ?: "",
        content = this.text ?: "", // Safe default: empty string instead of null
        role = role,
        timestamp = timestamp
    )
}

fun Long.toRelativeTimeSpan(): String {
    if (this == 0L) return ""
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

fun String?.toHumanReadableDate(): String {
    val time = this.toTimestamp()
    if (time == 0L) return "Recently"
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))
}

fun String?.toTimestamp(): Long {
    if (this.isNullOrBlank()) return 0L
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        var result = 0L
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(this)
                if (date != null) {
                    result = date.time
                    break
                }
            } catch (_: Exception) {}
        }
        result
    } catch (_: Exception) {
        0L
    }
}
