package com.fxf.adbhost

import android.content.Context
import kotlin.concurrent.thread

class UsbController(private val ctx: Context) {
    private val connect = UsbConnect(this, ctx)
    private var transportor: UsbTransportor? = null

    fun connect() {
        connect.connect {
            transportor = it
        }
    }

    fun send(byteArray: ByteArray, offset: Int = 0, len: Int = byteArray.size) {
        thread {
            transportor?.send(byteArray, offset, len)
        }
    }

    fun terminate() {
        transportor?.terminate()
        connect.terminate()
        thread {
            /*Thread.sleep(3000)
            connect()*/
        }
    }
}
