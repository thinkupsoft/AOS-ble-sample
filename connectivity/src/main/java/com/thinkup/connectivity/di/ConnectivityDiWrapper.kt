package com.thinkup.connectivity.di

import com.thinkup.connectivity.groups.GroupViewModel
import com.thinkup.connectivity.mesh.BleMeshManager
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.nodes.NodesViewModel
import com.thinkup.connectivity.provisioning.ProvisioningViewModel
import com.thinkup.connectivity.scanner.ScannerRepository
import com.thinkup.connectivity.scanner.ScannerViewModel
import com.thinkup.connectivity.utils.CapabilitiesUtil
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import org.koin.dsl.module

object ConnectivityDiWrapper {

    fun viewModelsModule() = module {
        factory { ScannerViewModel(get()) }
        factory { ProvisioningViewModel(get(), get()) }
        factory { NodesViewModel(get(), get()) }
        factory { GroupViewModel(get(), get()) }
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