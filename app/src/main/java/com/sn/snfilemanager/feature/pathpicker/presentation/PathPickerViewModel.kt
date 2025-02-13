package com.sn.snfilemanager.feature.pathpicker.presentation

import androidx.lifecycle.ViewModel
import com.sn.snfilemanager.core.util.RootPath
import com.sn.snfilemanager.providers.filepath.FilePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.io.path.isDirectory

@HiltViewModel
class PathPickerViewModel
@Inject
    constructor(
        private val filePathProvider: FilePathProvider,
    ) : ViewModel() {
        private val directoryList: MutableList<String> = mutableListOf()
        var currentPath: String? = null

        fun updateDirectoryList(path: String) {
            if (!directoryList.contains(path)) {
                directoryList.add(path)
            }
        }

        fun updateDirectoryListWithPos(position: Int) {
            if (position >= 0 && position < directoryList.size) {
                directoryList.subList(position + 1, directoryList.size).clear()
            }
        }

        fun getDirectoryList() = directoryList

        fun getStoragePath(rootPath: RootPath): String =
            when (rootPath) {
                RootPath.INTERNAL -> filePathProvider.internalStorageRootPath
                RootPath.EXTERNAL -> filePathProvider.externalStorageRootPath
            }

        fun getDirectoryList(directoryPath: String): List<Path> {
            val directory = Paths.get(directoryPath)
            return Files.list(directory).filter { it.isDirectory() && Files.isReadable(it) }
                .collect(Collectors.toList())
        }
    }
