package com.fxf.adbaccessory

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class UsbTransportor(private val controller: UsbController, parcelFileDescriptor: ParcelFileDescriptor) {
    private val input = FileInputStream(parcelFileDescriptor.fileDescriptor)
    private val output = FileOutputStream(parcelFileDescriptor.fileDescriptor)
    private val sendQueue = LinkedBlockingQueue<Message>()
    @Volatile
    private var isStop = false

    init {
        thread {
            read()
        }
        thread {
            while (!isStop) {
                val msg = sendQueue.poll(10000, TimeUnit.MILLISECONDS) ?: continue
                log("send start")
                output.write(msg.body)
                // output.flush()
                log("send end")
            }
        }
    }

    fun send(byteArray: ByteArray, offset: Int = 0, len: Int = byteArray.size) {
        sendQueue.offer(Message(byteArray))
       /* log("send: ${String(byteArray)}")
        output.write(byteArray, offset, len)
        output.flush()*/
    }

    fun read() {
        val byteArray = ByteArray(1024)
        try {
            while (!isStop) {
                val readSize = input.read(byteArray, 0, byteArray.size)
                if (readSize > 0) {
                    log("收到： ${String(byteArray, 0, readSize)}")
                } else {
                    log("错误 -1")
                }
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
