package com.sn.snfilemanager.feature.files.data

import android.net.Uri

data class RecentFile(
    val id: Long,
    val displayName: String,
    val uri: Uri,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val duration: Long? = null, // Optional, mainly for videos and audio
    val path: String? = null // Optional, consider using Uri for file access
)