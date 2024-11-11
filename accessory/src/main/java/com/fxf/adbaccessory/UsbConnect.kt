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
import kotlin.concurrent.thread

class UsbConnect(private val controller: UsbController, val ctx: Context) : BroadcastReceiver() {
    companion object {
        private const val PERMISSION_ACTION = "usb.accessory.permission"
    }

    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var connectionCallback: ((UsbTransportor) -> Unit)? = null
    private var device: UsbAccessory? = null
    var isReady: Boolean = false
        private set

    fun connect(callback: (UsbTransportor) -> Unit) {
        this.connectionCallback = callback

        val filter = IntentFilter().apply {
            addAction(PERMISSION_ACTION)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        }
        // ctx.unregisterReceiver(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(
                this,
                filter,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            ctx.registerReceiver(this, filter)
        }
        thread {
            while (true) {
                Thread.sleep(1000)
                log(this, "usbAccessory size :${usbManager.accessoryList?.size}")

            }
        }
        usbManager.accessoryList ?: return log("没找到设备")
        log("find usb accessory: ${usbManager.accessoryList.size}")
        for (d in usbManager.accessoryList) {
            log("try connect to:$d")
            if (requestPermission(device = d)) {
                onConnected(d)
                break
            }
        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        log("onReceive: ${intent?.action}")
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
                // reset()
                controller.terminate()
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
        thread {
            this.device = device
            Thread.sleep(3000)
            val parcelFileDescriptor = usbManager.openAccessory(device)
            this.connectionCallback?.invoke(UsbTransportor(controller, parcelFileDescriptor))
            isReady = true
        }

    }

    private fun reset() {
        connection?.close()
        connection = null
        isReady = false
    }

    fun terminate() {
        log(this, "terminate")
        // ctx.unregisterReceiver(this)
        connection?.close()
        connection = null
    }


    fun log(msg: String) {
        Log.d("Connect", msg)
    }
}
