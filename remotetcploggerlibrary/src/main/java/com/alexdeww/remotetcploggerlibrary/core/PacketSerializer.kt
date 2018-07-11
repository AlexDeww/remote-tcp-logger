package com.alexdeww.remotetcploggerlibrary.core

import com.alexdeww.niosockettcpclientlib.additional.NIOSocketSerializer
import com.alexdeww.remotetcploggerlibrary.core.packets.BasePacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.reflect.KClass

class PacketSerializer : NIOSocketSerializer<BasePacket> {

    private val mPacketIds: HashMap<KClass<out BasePacket>, Int> = hashMapOf()
    private val mPacketClasses: HashMap<Int, KClass<out BasePacket>> = hashMapOf()

    init {
        PacketEnum.values().forEach {
            mPacketIds[it.packetClass] = it.ordinal
            mPacketClasses[it.ordinal] = it.packetClass
        }
    }

    private fun getPacketId(packet: BasePacket): Int =
            mPacketIds[packet.javaClass.kotlin] ?: throw Exception("Can't find packet id for '${packet.javaClass.name}'")

    private fun getPacketClass(packetId: Int): KClass<out BasePacket> =
            mPacketClasses[packetId] ?: throw Exception("Can't find packet class, packet id = $packetId")

    override fun serialize(packet: BasePacket): ByteArray {
        val bbStream = ByteArrayOutputStream()
        val dos = DataOutputStream(bbStream)

        dos.writeInt(getPacketId(packet))
        packet.serialize(dos)
        return bbStream.toByteArray()
    }

    override fun deSerialize(packetData: ByteArray): BasePacket {
        val bbStream = ByteArrayInputStream(packetData)
        val dis = DataInputStream(bbStream)

        val packetId = dis.readInt()
        val packetClass = getPacketClass(packetId)
        val packet = packetClass.java.newInstance()
        (packet as BasePacket).deSerialize(dis)
        return packet
    }
}