package com.thinkup.connectivity.mesh

import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleManagerCallbacks
import no.nordicsemi.android.log.ILogSession

abstract class LoggableBleManager<T: BleManagerCallbacks>(context: Context): BleManager<T>(context) {
    var logSession: ILogSession? = null
}