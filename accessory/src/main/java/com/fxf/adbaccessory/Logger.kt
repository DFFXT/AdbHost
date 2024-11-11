package com.fxf.adbaccessory

import android.util.Log

fun log(any: Any, msg: String) {
    Log.d(any::class.java.name, msg)
}