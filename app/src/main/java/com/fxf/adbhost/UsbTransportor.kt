package com.fxf.adbhost

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class UsbTransportor(
    private val controller: UsbController,
    private val usbInterface: UsbInterface,
    private val connection: UsbDeviceConnection,
) {
    private val input: UsbEndpoint by lazy {
        for (i in 0 until usbInterface.endpointCount) {
            val end = usbInterface.getEndpoint(i)
            if (end.direction == UsbConstants.USB_DIR_IN) {
                log("类型in:${end.type}")
                return@lazy end
            }
        }
        throw Exception("")
    }
    private val out: UsbEndpoint by lazy {
        for (i in 0 until usbInterface.endpointCount) {
            val end = usbInterface.getEndpoint(i)
            if (end.direction == UsbConstants.USB_DIR_OUT) {
                log("类型out:${end.type}")
                return@lazy end
            }
        }
        throw Exception("222")
    }

    private val sendQueue = LinkedBlockingQueue<Message>()

    @Volatile
    private var isStop = false

    init {
        read()
        thread {
            while (!isStop) {
                Thread.sleep(2000)
                send("hello, Im host".toByteArray())
            }
        }
    }

    fun read() {
        thread {
            val buffer = ByteArray(1024)
            try {
                while (!isStop) {
                    log("start read")
                    val size = connection.bulkTransfer(input, buffer, 0, buffer.size, 0)
                    if (size > 0) {
                        log("read: ${String(buffer, 0, size)}")
                    } else {
                        throw Exception("bulkTransfer -1")
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                controller.terminate()
            }

        }
    }

    fun send(byteArray: ByteArray, offset: Int = 0, len: Int = byteArray.size) {
        // sendQueue.offer(Message(byteArray))
        // log("send in task list")
        try {
            log("start send： ${String(byteArray, offset, len)}")
            val size = connection.bulkTransfer(out, byteArray, offset, len, 0)
            log("send： $size")
            if (size < 0) {
                throw Exception("bulkTransfer -1")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            controller.terminate()
        }
    }
    fun terminate() {
        isStop = true
    }

    private fun log(msg: String) {
        Log.d("transport", msg)
    }
}
