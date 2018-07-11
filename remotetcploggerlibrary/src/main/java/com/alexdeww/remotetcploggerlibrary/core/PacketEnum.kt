package com.alexdeww.remotetcploggerlibrary.core

import com.alexdeww.remotetcploggerlibrary.core.packets.BasePacket
import com.alexdeww.remotetcploggerlibrary.core.packets.client.C1PacketWriteLogResponse
import com.alexdeww.remotetcploggerlibrary.core.packets.server.S1PacketWriteLog
import kotlin.reflect.KClass

enum class PacketEnum(
        val packetClass: KClass<out BasePacket>
) {
    S_1(S1PacketWriteLog::class),
    C_1(C1PacketWriteLogResponse::class),
}