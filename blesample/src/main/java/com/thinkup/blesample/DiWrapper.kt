package com.thinkup.blesample

import android.content.Context
import com.thinkup.connectivity.di.ConnectivityDiWrapper
import com.thinkup.connectivity.scanner.ScannerViewModel
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
                    ConnectivityDiWrapper.viewModelsModule(),
                    ConnectivityDiWrapper.repositoriesModule(),
                    ConnectivityDiWrapper.libModule(),
                    ConnectivityDiWrapper.utilsModule()
                )
            )
        }
    }


}