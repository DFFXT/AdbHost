package com.fxf.adbhost

import android.util.Log

fun log(any: Any, msg: String) {
    Log.d(any::class.java.name, msg)
}