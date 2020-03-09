package com.thinkup.connectivity.di

import com.thinkup.connectivity.*
import com.thinkup.connectivity.impl.*
import com.thinkup.connectivity.mesh.BleMeshManager
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.scanner.ScannerRepository
import com.thinkup.connectivity.utils.CapabilitiesUtil
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import org.koin.dsl.module

object ConnectivityDiWrapper {

    fun implModule() = module {
        factory<BleScanner> { BleScannerImpl(get()) }
        factory<BleProvisioner> { BleProvisionerImpl(get(), get(), get()) }
        factory<BleNode> { BleNodeImpl(get(), get(), get()) }
        factory<BleGroup> { BleGroupImpl(get(), get(), get()) }
        single<BleSession> { BleSessionImpl(get(), get(), get(), get()) }
        factory<BleFastTraining> { BleFastTrainingImpl(get(), get(), get()) }
        factory<BleScheduleTraining> { BleScheduleTrainingImpl(get(), get(), get()) }
    }

    fun repositoriesModule() = module {
        factory { ScannerRepository(get(), get(), get()) }
        single { NrfMeshRepository(get(), get()) }
    }

    fun libModule() = module {
        factory { MeshManagerApi(get()) }
        factory { BleMeshManager(get()) }
    }

    fun utilsModule() = module {
        factory { CapabilitiesUtil() }
    }
}