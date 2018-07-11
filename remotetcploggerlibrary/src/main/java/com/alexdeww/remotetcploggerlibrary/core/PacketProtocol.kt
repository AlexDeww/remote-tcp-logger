package com.alexdeww.remotetcploggerlibrary.core

import com.alexdeww.niosockettcpclientlib.additional.NIOSocketPacketProtocol
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class PacketProtocol : NIOSocketPacketProtocol {

    private var mLastBuffer: ByteArray? = null

    override fun encode(packetData: ByteArray): ByteArray {
        val bb = ByteArrayOutputStream()
        val dos = DataOutputStream(bb)
        val zipData = zip(packetData)
        dos.writeInt(zipData.size)
        dos.write(zipData)
        return bb.toByteArray()
    }

    private fun zip(packetData: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val gzips = GZIPOutputStream(bos)
        gzips.write(packetData)
        gzips.close()
        return bos.toByteArray()
    }

    private fun unZip(rawData: ByteArray): ByteArray {
        val buffer = ByteArray(8192)
        val bos = ByteArrayOutputStream()
        val gzips = GZIPInputStream(ByteArrayInputStream(rawData))
        gzips.use {
            var len: Int
            while (it.available() > 0) {
                len = it.read(buffer)
                if (len > 0) bos.write(buffer, 0, len)
            }
        }
        return bos.toByteArray()
    }

    override fun decode(rawData: ByteArray): List<ByteArray> {
        val buffer = if (mLastBuffer != null && mLastBuffer!!.isNotEmpty()) mLastBuffer!!.plus(rawData) else rawData
        if (buffer.size <= 4) return listOf()

        val packets: ArrayList<ByteArray> = arrayListOf()
        try {
            val dis = DataInputStream(ByteArrayInputStream(buffer))
            while (dis.available() > 0) {
                val packetLength = dis.readInt()
                if (dis.available() < packetLength) {
                    mLastBuffer = buffer
                    break
                }

                val packetData = ByteArray(packetLength)
                dis.read(packetData)
                packets.add(unZip(packetData))
                mLastBuffer = buffer.copyOfRange(buffer.lastIndex - dis.available(), buffer.lastIndex)
            }
        } catch (e: Throwable) {
            mLastBuffer = null
        }
        return packets
    }

    override fun clearBuffers() {
        mLastBuffer = null
    }
}