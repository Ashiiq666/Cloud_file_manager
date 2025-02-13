package com.sn.snfilemanager.core.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class MimeTypes(val types: String, val values: List<String>) : Parcelable {
    IMAGES("images", listOf("jpeg", "jpg", "png", "gif", "bmp", "WebP", "HEIF")),
    VIDEOS("videos", listOf("mp4", "3gp", "avi", "mkv", "wmv")),
    AUDIOS("audios", listOf("mp3", "aac", "wav", "ogg", "mid", "flac", "amr")),
    DOCUMENTS("document", listOf("pdf", "ppt", "doc", "xls", "txt", "docx", "pptx", "xml", "xlsx", "log", "psd", "ai", "indd")),
    ARCHIVES("archive", listOf("zip", "rar", "7z", "tar")),
    DOWNLOADS("downloads", listOf("pdf", "jpg", "mp4")),
    APK("apk", listOf("apk")),
}
