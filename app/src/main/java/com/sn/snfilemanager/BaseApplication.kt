package com.sn.snfilemanager

import android.app.Application
import com.sn.snfilemanager.feature.settings.SettingsUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val theme = SettingsUtils.resolveThemeMode(this)
        SettingsUtils.changeTheme(applicationContext, theme)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
