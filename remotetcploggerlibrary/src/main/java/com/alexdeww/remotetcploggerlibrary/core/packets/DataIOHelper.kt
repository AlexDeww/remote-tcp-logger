package com.alexdeww.remotetcploggerlibrary.core.packets

import java.io.DataInput
import java.io.DataOutput

fun DataOutput.writeString(str: String) {
    this.writeInt(str.length)
    this.writeBytes(str)
}

fun DataInput.readString(): String {
    val sl = this.readInt()
    val ba = ByteArray(sl)
    this.readFully(ba)
    return String(ba)
}