package com.thinkup.connectivity.mesh

import android.text.TextUtils
import androidx.lifecycle.LiveData
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner

class MeshNetworkLiveData: LiveData<MeshNetworkLiveData>() {
    private var meshNetwork: MeshNetwork? = null
    private var selectedAppKey: ApplicationKey? = null
    private var nodeName: String? = null

    /**
     * Loads the mesh network information in to live data
     *
     * @param meshNetwork provisioning settings
     */
    fun loadNetworkInformation(meshNetwork: MeshNetwork) {
        this.meshNetwork = meshNetwork
        postValue(this)
    }

    fun getMeshNetwork(): MeshNetwork? {
        return meshNetwork
    }

    /**
     * Refreshes the mesh network information
     *
     * @param meshNetwork provisioning settings
     */
    fun refresh(meshNetwork: MeshNetwork) {
        this.meshNetwork = meshNetwork
        postValue(this)
    }

    fun getNetworkKeys(): List<NetworkKey> {
        return meshNetwork!!.netKeys
    }

    /**
     * Returns the app keys list
     */
    fun getAppKeys(): List<ApplicationKey?> {
        return meshNetwork!!.appKeys
    }

    /**
     * Returns the list of [Provisioner]
     */
    fun getProvisioners(): List<Provisioner?>? {
        return meshNetwork!!.provisioners
    }

    fun getProvisioner(): Provisioner? {
        return meshNetwork!!.selectedProvisioner
    }

    /**
     * Return the selected app key to be added during the provisioning process.
     *
     * @return app key
     */
    fun getSelectedAppKey(): ApplicationKey {
        if (selectedAppKey == null) {selectedAppKey = meshNetwork!!.appKeys[0]}
        return selectedAppKey!!
    }

    /**
     * Set the selected app key to be added during the provisioning process.
     */
    fun setSelectedAppKey(appKey: ApplicationKey?) {
        selectedAppKey = appKey
        postValue(this)
    }

    fun resetSelectedAppKey() {
        selectedAppKey = null
    }

    /**
     * Returns the network name
     */
    fun getNetworkName(): String? {
        return meshNetwork!!.meshName
    }

    /**
     * Set the network name of the mesh network
     *
     * @param name network name
     */
    fun setNetworkName(name: String?) {
        meshNetwork!!.meshName = name
        postValue(this)
    }

    /**
     * Sets the node name
     *
     * @param nodeName node name
     */
    fun setNodeName(nodeName: String) {
        if (!TextUtils.isEmpty(nodeName)) {
            this.nodeName = nodeName
            postValue(this)
        }
    }

    /**
     * Returns the node name
     */
    fun getNodeName(): String? {
        return nodeName
    }
}