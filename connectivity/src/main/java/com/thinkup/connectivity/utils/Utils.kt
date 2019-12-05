package com.thinkup.connectivity.utils

import android.os.Build
import android.os.ParcelUuid
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

object Utils {

    fun isMarshmallowOrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    fun getServiceData(result: ScanResult, serviceUuid: UUID): ByteArray? {
        val scanRecord = result.scanRecord
        return scanRecord?.getServiceData(ParcelUuid(serviceUuid))
    }
}