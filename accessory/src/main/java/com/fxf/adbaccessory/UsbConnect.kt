package com.fxf.adbaccessory

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class UsbConnect(val ctx: Context) : BroadcastReceiver() {
    companion object {
        private const val PERMISSION_ACTION = "usb.accessory.permission"
    }

    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var connectionCallback: ((UsbTransportor) -> Unit)? = null
    var isReady: Boolean = false
        private set

    fun connect(callback: (UsbTransportor) -> Unit) {
        this.connectionCallback = callback
        usbManager.accessoryList ?: return log("没找到设备")
        for (d in usbManager.accessoryList) {
            log("try connect to:$d")
            if (requestPermission(device = d)) {
                onConnected(d)
                break
            }
        }
        val filter = IntentFilter().apply {
            addAction(PERMISSION_ACTION)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(
                this,
                filter,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            ctx.registerReceiver(this, filter)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        log("onReceive")
        intent?.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let {
            usbModify(intent.action ?: "", it)
        }
    }

    private fun usbModify(action: String, device: UsbAccessory) {
        when (action) {
            PERMISSION_ACTION -> {
                if (usbManager.hasPermission(device)) {
                    onConnected(device)
                } else {
                    log("no permission")
                }
            }

            UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                if (requestPermission(device)) {
                    onConnected(device)
                }
            }

            UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                reset()
            }
        }
    }

    private fun requestPermission(device: UsbAccessory): Boolean {
        if (usbManager.hasPermission(device)) {
            return true
        } else {
            usbManager.requestPermission(device, PendingIntent.getBroadcast(ctx, 0, Intent(PERMISSION_ACTION), 0))
            // permissionSignal.lock()
        }
        return false
    }

    private fun onConnected(device: UsbAccessory) {
        log("onConnected ")
        isReady = true
        this.connectionCallback?.invoke(UsbTransportor(usbManager, device))
    }

    private fun reset() {
        connection = null
        isReady = false
    }

    private fun release() {
        ctx.unregisterReceiver(this)
    }

    fun log(msg: String) {
        Log.d("Connect", msg)
    }
}
