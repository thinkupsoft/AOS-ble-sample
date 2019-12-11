package com.thinkup.connectivity.di

import com.thinkup.connectivity.BleGroup
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.BleProvisioner
import com.thinkup.connectivity.BleScanner
import com.thinkup.connectivity.impl.BleGroupImpl
import com.thinkup.connectivity.mesh.BleMeshManager
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.impl.BleNodeImpl
import com.thinkup.connectivity.impl.BleProvisionerImpl
import com.thinkup.connectivity.scanner.ScannerRepository
import com.thinkup.connectivity.impl.BleScannerImpl
import com.thinkup.connectivity.utils.CapabilitiesUtil
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import org.koin.dsl.module

object ConnectivityDiWrapper {

    fun implModule() = module {
        factory<BleScanner> { BleScannerImpl(get()) }
        factory<BleProvisioner> { BleProvisionerImpl(get(), get()) }
        factory<BleNode> { BleNodeImpl(get(), get()) }
        factory<BleGroup> { BleGroupImpl(get(), get()) }
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