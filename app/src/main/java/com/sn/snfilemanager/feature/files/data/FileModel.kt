package com.sn.snfilemanager.feature.files.data

import com.sn.mediastorepv.data.ConflictStrategy
import com.sn.snfilemanager.core.extensions.toHumanReadableByteCount
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.io.path.extension

data class FileModel(
    val id: Long,
    val name: String,
    val isDirectory: Boolean,
    val absolutePath: String,
    val lastModified: String,
    val readableSize: String,
    val size: Long,
    val extension: String,
    val isHidden: Boolean,
    var isSelected: Boolean,
    var conflictStrategy: ConflictStrategy,
)

fun Long.toFormattedDateAndTime(): String {
    val dateFormat = SimpleDateFormat("dd MMM  hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(this))
}


fun Path.toFileModel(): FileModel {
    return FileModel(
        id = UUID.randomUUID().mostSignificantBits,
        name = this.fileName.toString(),
        isDirectory = Files.isDirectory(this),
        absolutePath = this.toAbsolutePath().toString(),
        lastModified = Files.getLastModifiedTime(this).toMillis().toFormattedDateAndTime(),
        readableSize = if (Files.isDirectory(this)) {
            val itemCount = Files.list(this).count()
            if (itemCount <= 1) {
                "$itemCount item"
            } else {
                "$itemCount items"
            }
        } else {
            Files.size(this).toHumanReadableByteCount()
        },
        size = Files.size(this),
        extension = if (!Files.isDirectory(this)) this.extension else "",
        isHidden = Files.isHidden(this),
        isSelected = false,
        conflictStrategy = ConflictStrategy.OVERWRITE,
    )
}
