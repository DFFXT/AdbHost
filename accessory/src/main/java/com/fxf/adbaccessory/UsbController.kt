package com.fxf.adbaccessory

import android.content.Context
import kotlin.concurrent.thread

class UsbController(private val ctx: Context) {
    private val connect = UsbConnect(this, ctx)
    private var transportor: UsbTransportor? = null

    fun connect() {
        connect.connect {
            transportor = it
            thread {
                while (true) {
                    Thread.sleep(2000)
                    send("hello, Im accessory".toByteArray())
                }
            }
        }
    }

    fun send(byteArray: ByteArray, offset: Int = 0, len: Int = byteArray.size) {
        transportor?.send(byteArray, offset, len)
    }

    fun terminate() {
        transportor?.terminate()
        connect.terminate()
        transportor = null
        thread {
            System.exit(0)
            /*Thread.sleep(8000)
            connect()*/
        }
    }
}
