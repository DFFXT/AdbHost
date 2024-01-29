package com.fxf.adbaccessory

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class UsbTransportor(private val manager: UsbManager, private val device: UsbAccessory) {
    private val fileDescriptor = manager.openAccessory(device)
    private val input = FileInputStream(fileDescriptor.fileDescriptor)
    private val output = FileOutputStream(fileDescriptor.fileDescriptor)
    private val sendQueue = LinkedBlockingQueue<Message>()

    init {
        thread {
            read()
        }
        thread {
            while (true) {
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
        while (true) {
            val readSize = input.read(byteArray, 0, byteArray.size)
            if (readSize > 0) {
                log("收到： ${String(byteArray, 0, readSize)}")
            } else {
                log("错误 -1")
            }
        }
    }

    private fun log(msg: String) {
        Log.d("transport", msg)
    }
}
