package com.sn.snfilemanager.providers.preferences

import com.sn.snfilemanager.feature.files.data.FileSortOptions

class Preferences {
    object Popup {
        var sortBy: FileSortOptions.SortBy
            get() = FileSortOptions.SortBy.valueOf(
                AppPreference.getString(
                    AppPreference.PreferenceKey.PREF_SORT_BY_STR))
            set(sortBy) {
                AppPreference.set(
                    AppPreference.PreferenceKey.PREF_SORT_BY_STR, sortBy.name)
            }
        var isDirectoriesFirst: Boolean
            get() = AppPreference.getBoolean(
                AppPreference.PreferenceKey.PREF_DIRECTORIES_FIRST_BOOL)
            set(directoriesFirst) {
                AppPreference.set(
                    AppPreference.PreferenceKey.PREF_DIRECTORIES_FIRST_BOOL, directoriesFirst
                )
            }

        var orderFiles: FileSortOptions.Order
            get() = FileSortOptions.Order.valueOf(
                AppPreference.getString(
                    AppPreference.PreferenceKey.PREF_ORDER_FILES_STR))
            set(value) {
                AppPreference.set(
                    AppPreference.PreferenceKey.PREF_ORDER_FILES_STR, value.name)
            }
        var showHiddenFiles: Boolean
            get() = AppPreference.getBoolean(
                AppPreference.PreferenceKey.PREF_SHOW_HIDDEN_FILE_BOOL)
            set(value) {
                AppPreference.set(
                    AppPreference.PreferenceKey.PREF_SHOW_HIDDEN_FILE_BOOL, value)
            }
        var isGridEnabled: Boolean
            get() = AppPreference.getBoolean(
                AppPreference.PreferenceKey.PREF_GRID_TOGGLE_BOOL)
            set(value) {
                AppPreference.set(
                    AppPreference.PreferenceKey.PREF_GRID_TOGGLE_BOOL, value)
            }
    }
}