
package com.sn.snfilemanager.core.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.sn.snfilemanager.feature.files.data.RecentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class FileUtils {

    private val BASE_PATH = "/storage/emulated/0"


    fun getFileExtension(file: File?): String {
        var extension = ""
        if (file != null && file.exists()) {
            val fileName = file.name
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex > 0 && dotIndex < fileName.length) {
                extension = fileName.substring(dotIndex + 1)
            }
        }
        return extension
    }

    fun getFileSize(file: File?): Long {
        var fileSize: Long = 0
        if (file != null && file.exists()) {
            fileSize = file.length()
        }
        return fileSize
    }

    fun deleteFile(file: File?): Boolean {
        var deleted = false
        if (file != null && file.exists()) {
            deleted = file.delete()
        }
        return deleted
    }



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

    // get date file

    // get date file
    fun formatDateShort(date: Date): String {
        val dateFormat = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
        return dateFormat.format(date)
    }

    fun formatDateLong(date: Date): String {
        val dateFormat = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
        return dateFormat.format(date)
    }


    fun sizeOfDirectory(directory: File): Long {
        if (!directory.exists() || !directory.isDirectory) {
            return 0L
        }

        var size: Long = 0
        val files = directory.listFiles() ?: return size
        for (file in files) {
            size += if (file.isDirectory) {
                sizeOfDirectory(file)
            } else {
                file.length()
            }
        }
        return size
    }




    fun getFormatDateFile(path: String, isShort: Boolean): String {
        val file = File(path)
        if (!file.isFile) {
            val lastModified = file.lastModified()
            val date = Date(lastModified)
            val formattedDate = formatDateShort(date)
            val formattedDateLong = formatDateLong(date)
            return if (isShort) {
                formattedDate
            } else {
                formattedDateLong
            }
        }
        val lastModified = Date(file.lastModified())
        val formattedDateShort = formatDateShort(lastModified)
        val formattedDateLong = formatDateLong(lastModified)
        return if (isShort) {
            formattedDateShort
        } else {
            formattedDateLong
        }
    }

    fun getFileSizeFormatted(fileSize: Long): String {
        return formatSizeFile(fileSize)
    }


    fun getCategoryFileCount(category: String): Int {
        // Define root directories for different categories
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val images = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val audioDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val videoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // Add other directories for different categories

        // Calculate size for the specified category
        return when (category) {
            "Documents" -> countFiles(documentsDir)
            "Audio" -> countFiles(audioDir)
            "Video" -> countFiles(videoDir)
            "Downloads" -> countFiles(downloadsDir)
            "Images" -> countFiles(images)

            // Calculate size for other categories similarly
            else -> 0
        }
    }

    private fun countFiles(directory: File): Int {
        var count = 0
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    count++
                } else if (file.isDirectory) {
                    count += countFiles(file)
                }
            }
        }
        return count
    }



    private fun fileIsApk(file: File): Boolean {
        val fileExtension = getFileExtension(file)
        return fileExtension == "apk"
    }

    private fun fileIsArchive(file: File): Boolean {
        val fileExtension = getFileExtension(file)
        return isArchive(fileExtension)
    }

    private fun isImage(file: File): Boolean{
        val fileExtension = getFileExtension(file)
        return fileExtension in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "heic", "heif", "tiff", "svg")
    }

    private fun isVideo(file: File): Boolean{
        val fileExtension = getFileExtension(file)
        return fileExtension in listOf("mp4", "mkv", "avi", "webm", "flv", "3gp", "mov", "wmv", "mpeg")
    }


/*
        fun getFileIconByPath(context: Context, imagePath: String): Drawable? {
            val file = File(imagePath)
            if (file.exists() && file.isFile) {
                val mimeType = getMimeType(imagePath)
                return getDrawableForMimeType(context, mimeType)
            }
            return null
        }*/

        private fun getMimeType(filePath: String): String? {
            val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }

       /* private fun getDrawableForMimeType(context: Context, mimeType: String?): Drawable? {
            return try {
                val iconRes = when (mimeType) {
                    "image/jpeg", "image/png" -> R.drawable.ic_image
                    "video/mp4" -> R.drawable.ic_video
                    "audio/mp3" -> R.drawable.ic_audio
                    // Add more cases for other mime types if needed
                    else -> R.drawable.ic_file
                }
                ContextCompat.getDrawable(context, iconRes)
            } catch (e: Exception) {
                null
            }
        }*/



    private fun isArchive(fileExtension: String): Boolean {
        return fileExtension in listOf("zip", "7z", "tar", "tar.bz2", "tar.gz", "tar.xz", "tar.lz4", "tar.zstd")
    }


    fun getStorageSpaceInGB(spaceType: SpaceType): Int {
        val file = File(BASE_PATH)
        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usedSpace = totalSpace - freeSpace

        return when (spaceType) {
            SpaceType.TOTAL -> convertBytesToGB(totalSpace)
            SpaceType.FREE -> convertBytesToGB(freeSpace)
            SpaceType.USED -> convertBytesToGB(usedSpace)
        }
    }

    private fun convertBytesToGB(bytes: Long): Int {
        val gb = bytes / (1024 * 1024 * 1024)
        return gb.toInt()
    }

    enum class SpaceType {
        TOTAL,
        FREE,
        USED
    }

    enum class CreationOption {
        FILE,
        FOLDER
    }


    fun createFileAndFolder(path: String, name: String, creationOption: CreationOption): Boolean {
        return when (creationOption) {
            CreationOption.FOLDER -> createFolder(path, name)
            CreationOption.FILE -> createFile(path, name)
        }
    }


    fun createFolder(folderPath: String, folderName: String): Boolean {
        val folder = File(folderPath, folderName)
        return if (!folder.exists() && folder.mkdirs()) {
            true
        } else {
            false
        }
    }

    fun createFile(filePath: String, fileName: String): Boolean {
        val file = File(filePath, fileName)

        return try {
            file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    suspend fun getRecentFiles(context: Context): List<RecentFile> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        // Query URI for files, considering all types of files including images, videos, and audio
        val queryUri = MediaStore.Files.getContentUri("external")

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}" +
                " AND (${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'" +
                " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'" +
                " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'audio/%')"

        val recentFilesList = ArrayList<RecentFile>()

        context.contentResolver.query(queryUri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            var count = 0
            while (cursor.moveToNext() && count < 10) { // Assuming you want the top 10 recent files
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val contentUri: Uri = ContentUris.withAppendedId(queryUri, id)
                recentFilesList.add(RecentFile(id, displayName, contentUri, mimeType, 0, dateAdded, 0))
                count++
            }
        }

        recentFilesList
    }




//    suspend fun getAllRecentFiles(context: Context): ArrayList<RecentFile> = withContext(Dispatchers.IO) {
//        val projection = arrayOf(
//            MediaStore.Files.FileColumns._ID,
//            MediaStore.Files.FileColumns.DATA,
//            MediaStore.Files.FileColumns.DATE_ADDED,
//            MediaStore.Files.FileColumns.MEDIA_TYPE
//        )
//
//        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
//
//        val selectionArgs = arrayOf(
//            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
//            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
//        )
//
//        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
//
//        val queryUri = MediaStore.Files.getContentUri("external")
//
//        val recentFilesList = ArrayList<RecentFile>()
//
//        context.contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
//            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
//            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
//            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
//
//            while (cursor.moveToNext()) {
//                val filePath = cursor.getString(dataColumn)
//                val file = File(filePath)
//                val fileTitle = file.name
//                val fileDateAdded = cursor.getLong(dateAddedColumn)
//                val fileMediaType = cursor.getInt(mediaTypeColumn)
//
//                val fileTime = getFormatDateFile(fileDateAdded.toString(), true)
//
//                val recentFile = RecentFile(filePath, fileTitle, fileTime, fileMediaType)
//                recentFilesList.add(recentFile)
//            }
//        }
//
//        recentFilesList
//    }





    fun loadIcon(){

    }


    companion object {
        private var instance: FileUtils? = null

        fun getInstance(): FileUtils {
            if (instance == null) {
                instance = FileUtils()
            }
            return instance!!
        }
    }
}