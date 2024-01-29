package com.fxf.adbhost

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class UsbTransportor(private val usbInterface: UsbInterface, private val connection: UsbDeviceConnection) {
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

    init {
        read()

        thread {
            while (true) {
                val msg = sendQueue.poll(10000, TimeUnit.MILLISECONDS) ?: continue
                log("send start")
                repeat(100) {
                    Thread.sleep(1000)
                    connection.bulkTransfer(out, msg.body, 0, msg.body.size, 0)
                }
                // connection.bulkTransfer(out, msg.body, 0, msg.body.size, 0)
                log("send end")
            }
        }
        thread {
            while (true) {
                Thread.sleep(2000)
                send("hello, Im host".toByteArray())
            }
        }
    }

    fun read() {
        thread {
            val buffer = ByteArray(1024)
            while (true) {
                log("start read")
                val size = connection.bulkTransfer(input, buffer, 0, buffer.size, 0)
                if (size > 0) {
                    log("read: ${String(buffer, 0, size)}")
                } else {
                    throw Exception("size=$size")
                }
            }
        }
    }

    fun send(byteArray: ByteArray, offset: Int = 0, len: Int = byteArray.size) {
        // sendQueue.offer(Message(byteArray))
        // log("send in task list")
        log("start send： ${String(byteArray, offset, len)}")
        val size = connection.bulkTransfer(out, byteArray, offset, len, 0)
        log("send： $size")
    }

    private fun log(msg: String) {
        Log.d("transport", msg)
    }
}
