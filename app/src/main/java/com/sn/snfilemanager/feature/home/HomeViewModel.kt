package com.sn.snfilemanager.feature.home

import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn.mediastorepv.data.MediaType
import com.sn.snfilemanager.core.base.BaseResult
import com.sn.snfilemanager.core.extensions.toHumanReadableByteCount
import com.sn.snfilemanager.core.util.Config
import com.sn.snfilemanager.core.util.Event
import com.sn.snfilemanager.feature.files.data.FileModel
import com.sn.snfilemanager.feature.files.data.RecentFile
import com.sn.snfilemanager.feature.files.data.toFileModel
import com.sn.snfilemanager.feature.files.presentation.FilesListViewModel
import com.sn.snfilemanager.providers.filepath.FilePathProvider
import com.sn.snfilemanager.providers.mediastore.MediaStoreProvider
import com.sn.snfilemanager.providers.preferences.MySharedPreferences
import com.sn.snfilemanager.providers.preferences.PrefsTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val filePathProvider: FilePathProvider,
        private val preferences: MySharedPreferences,
        private val mediaStoreProvider: MediaStoreProvider,

        ) : ViewModel() {
        var notificationRuntimeRequested: Boolean = false
        private val _availableStorageLiveData: MutableLiveData<String> = MutableLiveData()
        val availableStorageLiveData: LiveData<String> = _availableStorageLiveData
    private val handler = Handler(Looper.getMainLooper())

    private var searchRunnable: Runnable? = null

    var currentPath: String? = null

    private var fileListJob: Job? = null

    private val _updateListLiveData: MutableLiveData<Event<MutableList<FileModel>>> =
        MutableLiveData()
    val updateListLiveData: LiveData<Event<MutableList<FileModel>>> = _updateListLiveData

    private val _searchStateLiveData: MutableLiveData<Event<Pair<Boolean, Boolean>>> =
        MutableLiveData()
    val searchStateLiveData: LiveData<Event<Pair<Boolean, Boolean>>> = _searchStateLiveData

        private val _availableExternalStorageLiveData: MutableLiveData<String?> = MutableLiveData()
        val availableExternalStorageLiveData: LiveData<String?> =
            _availableExternalStorageLiveData

    private var selectedItemList: MutableList<RecentFile> = mutableListOf()


    companion object {
        private const val SEARCH_DELAY = 500L
        private const val BATCH_SIZE = 100L
    }

    init {
            getFreeInternalMemory()
            getFreeExternalMemory()
        }
    fun getSelectedItem() = selectedItemList

        fun hasStorageRequestedPermissionBefore() = preferences.getBoolean(PrefsTag.PERMISSION_STORAGE)

        fun setStoragePermissionRequested() = preferences.putBoolean(PrefsTag.PERMISSION_STORAGE, true)

        fun hasNotificationRequestedPermissionBefore() = preferences.getBoolean(PrefsTag.PERMISSION_NOTIFICATION)

        fun setNotificationPermissionRequested() = preferences.putBoolean(PrefsTag.PERMISSION_NOTIFICATION, true)

        private fun getFreeInternalMemory() {
            viewModelScope.launch {
                val memory =
                    withContext(Dispatchers.IO) {
                        getFreeMemory(filePathProvider.internalStorageDirectory).toHumanReadableByteCount()
                    }
                _availableStorageLiveData.value = memory
            }
        }

        private fun getFreeExternalMemory() {
            viewModelScope.launch {
                val memory =
                    withContext(Dispatchers.IO) {
                        filePathProvider.externalSdCardDirectories.firstOrNull()?.let {
                            getFreeMemory(it).toHumanReadableByteCount()
                        }
                    }
                _availableExternalStorageLiveData.value = memory
            }
        }

        private fun getFreeMemory(path: File): Long {
            val stats = StatFs(path.absolutePath)
            return stats.availableBlocksLong * stats.blockSizeLong
        }

    private fun removeSearchCallback() {
        searchRunnable?.let { handler.removeCallbacks(it) }
    }

    fun clearSelectionList() {
        if (selectedItemList.isNotEmpty()) {
            selectedItemList.clear()
        }
    }

    fun getFilesList(path: String) {
        fileListJob =
            viewModelScope.launch {
                val directory = Paths.get(path)
                val totalFiles =
                    withContext(Dispatchers.IO) {
                        Files.list(directory)
                    }.count()
                var processedFiles: Long = 0
                val fileList: MutableList<FileModel> = mutableListOf()

                if (totalFiles == 0L) {
                    _updateListLiveData.postValue(Event(mutableListOf()))
                    return@launch
                }

                while (processedFiles < totalFiles) {
                    val remainingFiles = totalFiles - processedFiles
                    val currentBatchSize =
                        java.lang.Long.min(remainingFiles, HomeViewModel.BATCH_SIZE)
                    withContext(Dispatchers.IO) {
                        Files.list(directory)
                            .skip(processedFiles)
                            .limit(currentBatchSize)
                            .forEach { file ->
                                if (Files.isReadable(file) && (
                                            Config.hiddenFile ||
                                                    !Files.isHidden(
                                                        file,
                                                    )
                                            )
                                ) {
                                    fileList.add(file.toFileModel())
                                }
                            }
                    }
                    withContext(Dispatchers.Main) {
                        _updateListLiveData.postValue(Event(fileList))
                    }
                    processedFiles += currentBatchSize
                }
            }
    }

    fun searchFiles(query: String?) {
        removeSearchCallback()
        _searchStateLiveData.postValue(Event(Pair(true, false)))

        if (query.isNullOrEmpty()) {
            currentPath?.let { getFilesList(it) }
            _searchStateLiveData.postValue(Event(Pair(false, false)))
            return
        }

        if (query.length < 3) {
            _searchStateLiveData.postValue(Event(Pair(true, false)))
            return
        }

        _searchStateLiveData.postValue(Event(Pair(true, true))) // Show progress bar

        searchRunnable = Runnable {
            currentPath?.let { path ->
                viewModelScope.launch {
                    val result = mediaStoreProvider.searchInPath(
                        query,
                        path,
                        MediaType.FILES,
                    )
                    when (result) {
                        is BaseResult.Success -> {
                            val list = result.data.map { Paths.get(it).toFileModel() }
                            _updateListLiveData.postValue(Event(list.toMutableList()))
                        }
                        is BaseResult.Failure -> {
                            // Handle failure
                            Log.e("Search", "Error searching files: ${result.exception.message}")
                        }
                    }
                    // Add the newly created folder to the search results if applicable
                    val targetPath = Paths.get(path, query)
                    if (Files.isDirectory(targetPath)) {
                        val newFolder = targetPath.toFileModel()
                        _updateListLiveData.postValue(Event(mutableListOf(newFolder)))
                    }

                    _searchStateLiveData.postValue(Event(Pair(false, false))) // Hide progress bar
                }
            }
        }
        searchRunnable?.let { handler.postDelayed(it, SEARCH_DELAY) }
    }

}
