package com.thinkup.blesample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionUtil {
    companion object {
        const val REQUEST_PERMISSION_LOCATION = 200
        const val REQUEST_ENABLE_BLUETOOTH = 201
    }

    fun check(activity: Activity?, permission: String): Boolean {
        activity?.let {
            return ContextCompat.checkSelfPermission(
                it,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    fun request(activity: Activity?, permission: String, request: Int) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(it, permission)) {
                    ActivityCompat.requestPermissions(it, arrayOf(permission), request)
                } else {
                    ActivityCompat.requestPermissions(it, arrayOf(permission), request)
                }
            }
        }
    }
}