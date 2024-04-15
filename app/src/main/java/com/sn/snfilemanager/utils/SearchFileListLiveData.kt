

package com.sn.snfilemanager.utils

import android.os.AsyncTask
import android.util.Log
import com.sn.snfilemanager.feature.files.data.FileModel
import com.sn.snfilemanager.feature.files.data.toFileModel
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.io.path.isDirectory

class SearchFileListLiveData(
    private val path: Path,
    private val query: String
) : CloseableLiveData<Stateful<List<FileModel>>>() {
    private var future: Future<Unit>? = null

    init {
        loadValue()
    }

    fun loadValue() {
        future?.cancel(true)
        value = Loading(emptyList())
        future = (AsyncTask.THREAD_POOL_EXECUTOR as ExecutorService).submit<Unit> {
            val fileList = mutableListOf<FileModel>()
            try {
                path.search(query, INTERVAL_MILLIS) { paths: List<Path> ->
                    for (path in paths) {
                        val fileItem = try {
                            path.toFileModel()
                        } catch (e: IOException) {
                            Log.e("WEER", "ERRO: $e")

                            e.printStackTrace()
                            continue
                        }
                        fileList.add(fileItem)
                    }
                    postValue(Loading(fileList.toList()))
                }
                postValue(Success(fileList))
            } catch (e: Exception) {
                Log.e("WEER", "ERRO: $e")
                Failure(value, e)
            }
            Log.i("RESULTS", "Results: $fileList")
        }
    }

    @Throws(IOException::class)
    fun Path.search(query: String, intervalMillis: Long, listener: (List<Path>) -> Unit) {
        val searchResults = mutableListOf<Path>()
        if (isDirectory()){
            val files = toFile().listFiles()
            if (files != null){
                for (file in files){
                    if (isDirectory()){
                        searchResults.addAll(searchDirectory(this, query))
                    } else{
                        if (file.name.contains(query, ignoreCase = true)){
                            searchResults.add(file.toPath())
                        }
                    }
                }
            }
        }
        listener(searchResults)
    }

    private fun searchDirectory(directory: Path, query: String): List<Path> {
        val results = mutableListOf<Path>()
        if (directory.isDirectory()) {
            val files = directory.toFile().listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        results.addAll(searchDirectory(file.toPath(), query))
                    } else {
                        if (file.name.contains(query, ignoreCase = true)) {
                            results.add(file.toPath())
                        }
                    }
                }
            }
        }
        return results
    }


    override fun close() {
        future?.cancel(true)
    }

    companion object {
        private const val INTERVAL_MILLIS = 500L
    }
}
class SearchState(val isSearching: Boolean, val query: String)
