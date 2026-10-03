package com.baiviet.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** DataStore dùng chung cho cả app (ví, cài đặt, luật nhà). */
internal val Context.baiVietPrefs: DataStore<Preferences> by preferencesDataStore(name = "bai_viet")
