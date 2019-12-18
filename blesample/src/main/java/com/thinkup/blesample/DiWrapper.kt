package com.thinkup.blesample

import android.content.Context
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.di.ConnectivityDiWrapper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

object DiWrapper {

    fun start(context: Context) {
        startKoin {
            androidLogger()
            androidContext(context)
            modules(
                listOf(
                    ConnectivityDiWrapper.implModule(),
                    ConnectivityDiWrapper.repositoriesModule(),
                    ConnectivityDiWrapper.libModule(),
                    ConnectivityDiWrapper.utilsModule(),
                    moduleSample()
                )
            )
        }
    }

    private fun moduleSample() = module {
        single<BleSetting> { BleSettingImpl(get()) }
    }


}