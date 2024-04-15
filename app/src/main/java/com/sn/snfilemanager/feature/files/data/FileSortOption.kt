package com.sn.snfilemanager.feature.files.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileSortOptions(
    val sortBy: SortBy,
    val order: Order,
    val isDirectoriesFirst: Boolean
): Parcelable {
    fun createComparator(): Comparator<FileModel> {
        return compareBy<FileModel> { file ->
            NAME_UNIMPORTANT_PREFIXES.any { prefix -> file.name.startsWith(prefix) }
        }.thenBy { file ->
            when (sortBy) {
                SortBy.NAME -> file.name
                SortBy.TYPE -> file.extension
                SortBy.SIZE -> file.size
                else -> file.name
            }
        }.thenBy { file ->
            if (isDirectoriesFirst) {
                file.isDirectory
            } else {
                null
            }
        }.let { comparator ->
            if (order == Order.DESCENDING) {
                comparator.reversed()
            } else {
                comparator
            }
        }
    }
    companion object {
        private val NAME_UNIMPORTANT_PREFIXES = listOf(".", "#")
    }
    enum class SortBy {
        NAME,
        TYPE,
        SIZE,
        LAST_MODIFIED
    }

    enum class Order {
        ASCENDING, DESCENDING
    }
}
