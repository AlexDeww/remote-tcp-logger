package com.alexdeww.remotetcploggerlibrary

import android.util.Log
import com.alexdeww.niosockettcpclientlib.NIOSocketTCPClient
import com.alexdeww.niosockettcpclientlib.NIOSocketTcpClientListener
import com.alexdeww.niosockettcpclientlib.core.NIOSocketOperationResult
import com.alexdeww.niosockettcpclientlib.core.NIOSocketWorkerState
import com.alexdeww.remotetcploggerlibrary.core.PacketProtocol
import com.alexdeww.remotetcploggerlibrary.core.PacketSerializer
import com.alexdeww.remotetcploggerlibrary.core.RemoteLoggerClient
import com.alexdeww.remotetcploggerlibrary.core.packets.BasePacket
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object RemoteLogger {

    private const val TAG = "RemoteLogger"

    const val VERBOSE: Int = 2
    const val DEBUG: Int = 3
    const val INFO: Int = 4
    const val WARN: Int = 5
    const val ERROR: Int = 6

    private class LogMessage(
            val tag: String,
            val level: Int,
            val message: String,
            val time: Long
    )

    private lateinit var loggerClient: RemoteLoggerClient
    private var config: RemoteLoggerConfig = RemoteLoggerConfig("", 0, 50)
    private var loggerId: String = "UNDEFINED_ID"
    private var isInit = false
    private var messageBufferCount = AtomicInteger(0)
    private var isSending = AtomicBoolean(false)
    private val messagesBuffer: Queue<LogMessage> = ConcurrentLinkedQueue()
    private var _isStarted = false

    val isStarted: Boolean
        get() = _isStarted

    fun initialize(config: RemoteLoggerConfig) {
        if (isInit) return

        this.config = config
        isInit = true
        initConnection()
    }

    fun setLoggerId(loggerId: String) {
        this.loggerId = loggerId
    }

    fun start() {
        if (!checkInit()) return

        _isStarted = true
        loggerClient.startConnection()
    }

    fun stop() {
        if (!checkInit()) return

        _isStarted = false
        loggerClient.stopConnection()
    }

    fun writeLog(tag: String, message: String?, level: Int, tr: Throwable? = null) {
        if (!checkInit()) return

        try {
            val msg = "$message${if (tr != null) "\n${Log.getStackTraceString(tr)}" else ""}"
            if (!addMessage(LogMessage(tag, level, msg, Date().time))) {
                Log.w(TAG, "Message buffer is full!!!")
                return
            }
            if (isSending.get()) return

            processSend()
        } catch (e: Throwable) {
            Log.e(TAG, "Write log error.", e)
        }
    }

    fun v(tag: String, msg: String?) = writeLog(tag, msg, VERBOSE)
    fun v(tag: String, msg: String?, tr: Throwable?) = writeLog(tag, msg, VERBOSE, tr)

    fun d(tag: String, msg: String?) = writeLog(tag, msg, DEBUG)
    fun d(tag: String, msg: String?, tr: Throwable?) = writeLog(tag, msg, DEBUG, tr)

    fun i(tag: String, msg: String?) = writeLog(tag, msg, INFO)
    fun i(tag: String, msg: String?, tr: Throwable?) = writeLog(tag, msg, INFO, tr)

    fun w(tag: String, msg: String?) = writeLog(tag, msg, WARN)
    fun w(tag: String, msg: String?, tr: Throwable?) = writeLog(tag, msg, WARN, tr)

    fun e(tag: String, msg: String?) = writeLog(tag, msg, ERROR)
    fun e(tag: String, msg: String?, tr: Throwable?) = writeLog(tag, msg, ERROR, tr)

    private fun addMessage(message: LogMessage): Boolean {
        if (messageBufferCount.get() > config.messageBufferSize) return false

        messagesBuffer.add(message)
        messageBufferCount.incrementAndGet()
        return true
    }

    private fun checkInit(): Boolean {
        if (!isInit) {
            Log.e(TAG, "Logger is not initialized!!!")
            return false
        }
        return true
    }

    private fun processSend() {
        isSending.set(true)
        if (!loggerClient.isConnected) return //если нет подключения, выйти и ожидать события подключения, затем запустить цикл отправки

        val message = messagesBuffer.peek()
        if (message == null) {
            isSending.set(false)
            return
        }

        loggerClient.sendLog(loggerId, message.tag, message.level, message.message, message.time, object : NIOSocketOperationResult() {
            override fun onComplete() {
                if (messagesBuffer.poll() != null) messageBufferCount.decrementAndGet()
                processSend()
            }
            override fun onError(error: Throwable) { processSend() }
        })
    }

    private fun initConnection() {
        loggerClient = RemoteLoggerClient(config.host, config.port, false, 8192, 5000, PacketProtocol(), PacketSerializer(), object : NIOSocketTcpClientListener<BasePacket> {
            override fun onConnected(client: NIOSocketTCPClient<BasePacket>) { processSend() }
            override fun onDisconnected(client: NIOSocketTCPClient<BasePacket>) {}
            override fun onError(client: NIOSocketTCPClient<BasePacket>, state: NIOSocketWorkerState, error: Throwable) {
                Log.e(TAG, "Error occurred, state: $state", error)
            }
            override fun onPacketReceived(client: NIOSocketTCPClient<BasePacket>, packet: BasePacket) {}
        })
    }

}