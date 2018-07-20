package com.alexdeww.remotetcploggerlibrary.core

import com.alexdeww.niosockettcpclientlib.NIOSocketTCPClient
import com.alexdeww.niosockettcpclientlib.NIOSocketTcpClientListener
import com.alexdeww.niosockettcpclientlib.additional.NIOSocketPacketProtocol
import com.alexdeww.niosockettcpclientlib.additional.NIOSocketSerializer
import com.alexdeww.niosockettcpclientlib.core.NIOSocketOperationResult
import com.alexdeww.niosockettcpclientlib.core.NIOSocketWorkerState
import com.alexdeww.niosockettcpclientlib.core.NIOTcpSocketWorker
import com.alexdeww.niosockettcpclientlib.core.safeCall
import com.alexdeww.niosockettcpclientlib.exception.AlreadyConnected
import com.alexdeww.niosockettcpclientlib.exception.Disconnected
import com.alexdeww.remotetcploggerlibrary.core.packets.BasePacket
import com.alexdeww.remotetcploggerlibrary.core.packets.server.S1PacketWriteLog
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class RemoteLoggerClient(
        host: String,
        port: Int,
        keepAlive: Boolean,
        bufferSize: Int,
        connectionTimeout: Int,
        protocol: NIOSocketPacketProtocol,
        serializer: NIOSocketSerializer<BasePacket>,
        clientListener: NIOSocketTcpClientListener<BasePacket>?
) : NIOSocketTCPClient<BasePacket>(host, port, keepAlive, bufferSize, connectionTimeout, protocol, serializer, clientListener) {

    companion object {
        private const val THREAD_NAME = "RemoteLoggerClientThread"
        private const val MSG_NOT_CONNECTED = "Not connected"
        private const val MSG_RESPONSE_TIMEOUT = "Response timeout"
        private const val MSG_REQUEST_TIMEOUT = "Response timeout"
        private const val REQUEST_TIMEOUT = 30L //seconds
        private const val RESPONSE_TIMEOUT = 30L //seconds
        private const val RECONNECT_INTERVAL = 10L //seconds
    }

    private class WaitResponseItem(
            val future: Future<*>,
            val result: NIOSocketOperationResult?
    )

    private val waitResponse: HashMap<Long, WaitResponseItem> = hashMapOf()
    private val executor = Executors.newSingleThreadScheduledExecutor {
        Thread(it).also {
            it.name = THREAD_NAME
            it.isDaemon = Thread.currentThread().isDaemon
        }
    }

    private inner class OperationWithResponse(
            val packet: BasePacket,
            var result: NIOSocketOperationResult?
    ) : NIOSocketOperationResult() {

        private var requestTask = executor.schedule({ onError(Exception(MSG_REQUEST_TIMEOUT)) }, REQUEST_TIMEOUT, TimeUnit.SECONDS)

        override fun onComplete() {
            cancelRequestTask()
            if (isCanceled) return

            executor.execute {
                val sf = executor.schedule({ onError(Exception(MSG_RESPONSE_TIMEOUT)) }, RESPONSE_TIMEOUT, TimeUnit.SECONDS)
                waitResponse[packet.uid] = WaitResponseItem(sf, result)
            }
        }

        override fun onError(error: Throwable) {
            cancelRequestTask()
            if (isCanceled) return

            val res = result
            cancel()
            safeCall { res?.onError(error) }
        }

        override fun cancel() {
            super.cancel()
            result?.cancel()
            result = null
        }

        private fun cancelRequestTask() {
            requestTask?.cancel(true)
            requestTask = null
        }
    }

    override fun onDisconnected(socket: NIOTcpSocketWorker) {
        executor.execute {
            waitResponse.values.forEach {
                it.future.cancel(true)
                safeCall { it.result?.onError(Disconnected()) }
            }
            waitResponse.clear()
        }
        super.onDisconnected(socket)
        reconnect()
    }

    override fun doOnPacketReceived(packet: BasePacket) {
        executor.execute {
            waitResponse.remove(packet.uid)?.also {
                it.future.cancel(true)
                if (it.result?.isCanceled == false) safeCall { it.result.onComplete() }
            }
        }
        super.doOnPacketReceived(packet)
    }

    fun sendLog(loggerId: String, tag: String, level: Int, message: String, time: Long, result: NIOSocketOperationResult) {
        executor.execute {
            val packet = S1PacketWriteLog(loggerId, tag, level, message, time)
            if (!sendPacket(packet, OperationWithResponse(packet, result))) {
                result.onError(Exception(MSG_NOT_CONNECTED))
            }
        }
    }

    fun startConnection() {
        reconnect()
    }

    private fun reconnect() {
        executor.schedule({
            try {
                connect()
            } catch (e: AlreadyConnected) {
                //ignore
            } catch (e: Throwable) {
                safeCall { clientListener?.onError(this, NIOSocketWorkerState.CONNECTING, e) }
                reconnect()
            }
        }, RECONNECT_INTERVAL, TimeUnit.SECONDS)
    }

}