package com.sn.snfilemanager.utils

import android.os.AsyncTask
import android.util.Log
import com.sn.snfilemanager.feature.files.data.FileModel
import com.sn.snfilemanager.feature.files.data.toFileModel
import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class FileListLiveData(private val path: Path) : CloseableLiveData<Stateful<List<FileModel>>>() {
    private var future: Future<Unit>? = null

    private val observer: DirectoryObserver

    @Volatile
    private var isChangedWhileInactive = false

    init {
        loadValue()
        observer = DirectoryObserver(path) { onChangeObserved() }

    }

    fun loadValue() {
        future?.cancel(true)
        value = Loading(value?.value)
        future = (AsyncTask.THREAD_POOL_EXECUTOR as ExecutorService).submit<Unit> {
            val value = try {
                path.newDirectoryStream().use { directoryStream ->
                    val fileList = mutableListOf<FileModel>()
                    for (path in directoryStream) {
                        try {
                            fileList.add(path.toFileModel())
                        } catch (e: DirectoryIteratorException) {
                            Log.e("LIVEDATAAA", "ERRO:: $e")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    Success(fileList as List<FileModel>)
                }
            } catch (e: Exception) {
                Failure(value, e)

            }
            postValue(value as Stateful<List<FileModel>>?)
        }
    }

    @Throws(IOException::class)
    fun Path.newDirectoryStream(): DirectoryStream<Path> = Files.newDirectoryStream(this)

    private fun onChangeObserved() {
        if (hasActiveObservers()) {
            loadValue()
        } else {
            isChangedWhileInactive = true
        }
    }

    override fun onActive() {
        if (isChangedWhileInactive) {
            loadValue()
            isChangedWhileInactive = false
        }
    }

    override fun close() {
        observer.close()
        future?.cancel(true)
    }
}