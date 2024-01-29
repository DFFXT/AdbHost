package com.fxf.adbhost

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import java.util.LinkedList

class UsbConnect(private val controller: UsbController, val ctx: Context) : BroadcastReceiver() {
    companion object {
        private const val PERMISSION_ACTION = "afd.afasdfsdf"

        private val AOAId = LinkedList<String>().apply {
            add("Baidu2")
            add("CarLifes")
            add("Baidu CarLifes")
            add("1.0.0")
            add("http://carlife.baidu.com/")
            add("0720SerialNfo.")
        }

        const val AOA_GET_PROTOCOL = 51
        const val AOA_SEND_IDENT = 52
        const val AOA_START_ACCESSORY = 53
        const val AOA_AUDIO_SUPPORT = 58

        const val AOA_MANUFACTURER = "fxf"
        const val AOA_MODEL_NAME = "Usb-Test"
        const val AOA_DESCRIPTION = "ADB-HOST"
        const val AOA_VERSION = "1.0"
        const val AOA_URI = "http://www.baidu.com/adb-host"
        const val AOA_SERIAL_NUMBER = "0720SerialNo."
    }

    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var connectedCallback: ((UsbTransportor) -> Unit)? = null
    fun connect(callback: ((UsbTransportor) -> Unit)) {
        this.connectedCallback = callback

        val filter = IntentFilter().apply {
            addAction(PERMISSION_ACTION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
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
        for (d in usbManager.deviceList.values) {
            log("try connect to:$d")
            if (isRightDevice(d) && switchAoA(device = d)) {
                onConnected(d)
                break
            }
        }
    }

    private fun isWifiDevice(device: UsbDevice?): Boolean {
        device ?: return false
        if (1 == device.interfaceCount) {
            val usbInter = device.getInterface(UsbConstants.USB_CLASS_MASS_STORAGE)
            if (UsbConstants.USB_CLASS_VENDOR_SPEC == usbInter.interfaceClass &&
                UsbConstants.USB_CLASS_VENDOR_SPEC == usbInter.interfaceSubclass &&
                UsbConstants.USB_CLASS_VENDOR_SPEC == usbInter.interfaceProtocol
            ) {
                log("this device is wifi")
                return true
            }
        }
        return false
    }

    private fun isRightDevice(device: UsbDevice): Boolean {
        return !isWifiDevice(device)
    }

    private fun isAccessory(device: UsbDevice): Boolean {
        return device.vendorId == 0x18D1 &&
            device.productId >= 0x2D00 &&
            device.productId <= 0x2D05
    }

    private fun handShack(device: UsbDevice): Boolean {
        usbInterface = usbInterface ?: device.getInterface(0)
        connection = connection ?: usbManager.openDevice(device)
        val claim = connection!!.claimInterface(usbInterface, true)
        if (claim) {
            val byteArray = ByteArray(2)
            val result = connection!!.controlGet(51, 0, 0, byteArray, 0)
            if (result > 0) {
                if (sendIdentities()) {
                    return true
                } else {
                    log("cewfsdf")
                    return false
                }
                /*for (i in AOAId.indices) {
                    if (connection!!.controlPush(52, 0, i, AOAId[i].toByteArray(), 0) < 0) {
                        log("error hand control $i")
                        return false
                    }
                }*/
                return true
            } else {
                log("error version")
                return false
            }
        } else {
            log("error claim")
            return false
        }
    }

    private fun startAccessory(): Boolean {
        return controlTransferOut(AOA_START_ACCESSORY, 0, 0) >= 0
    }

    private fun switchAoA(device: UsbDevice): Boolean {
        if (requestPermission(device)) {
            if (isAccessory(device)) {
                log("already accessory, hand")
                return handShack(device)
            } else {
                log("hand try switch to accessory")
                if (handShack(device)) {
                    // val r  = connection!!.controlPush(53, 0 ,0, null, 1000)
                    val r = startAccessory()
                    if (r) {
                        log("switch aoa true")
                        return false
                    } else {
                        log("switch aoa error")
                        return false
                    }
                } else {
                    log("handShack error")
                    return false
                }
            }
        } else {
            log("has no permission")
            return false
        }
    }

    private fun UsbDeviceConnection.controlPush(request: Int, value: Int, index: Int, byteArray: ByteArray?, timeout: Int): Int {
        return controlTransfer(UsbConstants.USB_DIR_OUT or UsbConstants.USB_TYPE_VENDOR, request, value, index, byteArray, byteArray?.size ?: 0, timeout)
    }

    private fun UsbDeviceConnection.controlGet(request: Int, value: Int, index: Int, byteArray: ByteArray, timeout: Int): Int {
        return controlTransfer(UsbConstants.USB_DIR_IN or UsbConstants.USB_TYPE_VENDOR, request, value, index, byteArray, byteArray.size, timeout)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        log("onReceive")
        intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let {
            usbModify(intent.action ?: "", it)
        } ?: log("error no device")
    }

    private fun usbModify(action: String, device: UsbDevice) {
        log("usbModify $device")
        for (i in 0 until device.interfaceCount) {
            val face = device.getInterface(i)
            log("face$i: ${face.name} ${face.interfaceClass} ${face.interfaceProtocol} ${face.interfaceSubclass} ${face.alternateSetting} ${face.endpointCount}")
        }
        when (action) {
            PERMISSION_ACTION -> {
                if (usbManager.hasPermission(device)) {
                    if (switchAoA(device)) {
                        onConnected(device)
                    }
                } else {
                    log("no permission")
                }
            }

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                if (switchAoA(device)) {
                    onConnected(device)
                } else {
                    log("re switchAoA false")
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                reset()
            }
        }
    }

    private fun requestPermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) {
            return true
        } else {
            usbManager.requestPermission(device, PendingIntent.getBroadcast(ctx, 0, Intent(PERMISSION_ACTION), 0))
            // permissionSignal.lock()
        }
        return false
    }

    private fun onConnected(device: UsbDevice) {
        log("onConnected ")
        connectedCallback?.invoke(UsbTransportor(controller, usbInterface!!, connection!!))
    }

    private fun reset() {
        connection = null
    }

    private fun release() {
        ctx.unregisterReceiver(this)
    }

    private fun sendIdentities(): Boolean {
        val timeout = 1000
        if (controlTransferOut(AOA_SEND_IDENT, 0, 0, AOA_MANUFACTURER.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_MANUFACTURER false")
            return false
        }

        if (controlTransferOut(AOA_SEND_IDENT, 0, 1, AOA_MODEL_NAME.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_MODEL_NAME false")
            return false
        }

        if (controlTransferOut(AOA_SEND_IDENT, 0, 2, AOA_DESCRIPTION.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_DESCRIPTION false")
            return false
        }

        if (controlTransferOut(AOA_SEND_IDENT, 0, 3, AOA_VERSION.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_VERSION false")
            return false
        }

        if (controlTransferOut(AOA_SEND_IDENT, 0, 4, AOA_URI.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_URI false")
            return false
        }

        if (controlTransferOut(AOA_SEND_IDENT, 0, 5, AOA_SERIAL_NUMBER.toByteArray(), timeout = timeout) < 0) {
            // Logger.d(Constants.TAG,"AOA_SERIAL_NUMBER false")
            return false
        }
        // Logger.d(Constants.TAG,"sendIdentities true");
        return true
    }

    private fun controlTransferOut(
        request: Int,
        value: Int,
        index: Int,
        buffer: ByteArray? = null,
        length: Int = buffer?.size ?: 0,
        timeout: Int = 0,
    ): Int {
        // Logger.d(Constants.TAG, "controlTransferOut:start,request:",request,",value",value,",index:",index,",buffer:",buffer,",len:",length)
        var funValue = connection!!.controlTransfer(
            UsbConstants.USB_DIR_OUT or UsbConstants.USB_TYPE_VENDOR,
            request,
            value,
            index,
            buffer,
            length,
            timeout,
        )
        // Logger.d(Constants.TAG, "controlTransferOut:",funValue)
        return funValue
    }

    fun log(msg: String) {
        Log.d("Connect", msg)
    }

    fun terminate() {
        connection?.close()
        connectedCallback = null
        connection = null
        usbInterface = null
    }
}
