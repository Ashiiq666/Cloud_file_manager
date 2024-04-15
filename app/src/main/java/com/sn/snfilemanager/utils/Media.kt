package com.sn.snfilemanager.utils

import android.net.Uri
import android.os.Parcelable

import com.sn.snfilemanager.feature.files.data.FileModel
import kotlinx.parcelize.Parcelize
import java.nio.file.Paths

@Parcelize
data class Media(
    val uri: Uri,
    val id: Long = 0,
    val mimeType: MimeType
) : Parcelable {
    override fun toString(): String {
        return "id=$id, uri=$uri, mimeType=$mimeType"
    }

    companion object {
        fun createFromFileModel(file: FileModel): Media {
            val uri = Paths.get(file.absolutePath).fileProviderUri
            val mime = FileUtil().getMimeType(uri, null)!!
            val mimeType = MimeType(mime)
            val id = if (file.id.toString().startsWith("-")) {
                file.id.toString().removePrefix("-").toLong()
            } else {
                file.id
            }


            return Media(
                uri = uri,
                id = id,
                mimeType = mimeType
            )
        }
    }
}
