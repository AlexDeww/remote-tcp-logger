package com.alexdeww.remotetcploggerlibrary.core.packets

import java.io.DataInput
import java.io.DataOutput
import java.util.*

abstract class BasePacket {

    var uid: Long = Date().time

    open fun serialize(output: DataOutput) {
        output.writeLong(uid)
    }

    open fun deSerialize(input: DataInput) {
        uid = input.readLong()
    }

}