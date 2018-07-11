package com.alexdeww.remotetcploggerlibrary.core.packets.server

import com.alexdeww.remotetcploggerlibrary.core.packets.BasePacket
import com.alexdeww.remotetcploggerlibrary.core.packets.readString
import com.alexdeww.remotetcploggerlibrary.core.packets.writeString
import java.io.DataInput
import java.io.DataOutput

data class S1PacketWriteLog(
        var loggerId: String = "",
        var tag: String = "",
        var level: Int = 0,
        var message: String = "",
        var time: Long = 0
) : BasePacket() {

    override fun serialize(output: DataOutput) {
        super.serialize(output)
        output.writeString(loggerId)
        output.writeString(tag)
        output.writeInt(level)
        output.writeString(message)
        output.writeLong(time)
    }

    override fun deSerialize(input: DataInput) {
        super.deSerialize(input)
        loggerId = input.readString()
        tag = input.readString()
        level = input.readInt()
        message = input.readString()
        time = input.readLong()
    }

}