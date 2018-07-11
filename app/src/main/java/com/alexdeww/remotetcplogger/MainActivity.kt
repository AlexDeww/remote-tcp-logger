package com.alexdeww.remotetcplogger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.alexdeww.remotetcploggerlibrary.RemoteLogger
import com.alexdeww.remotetcploggerlibrary.RemoteLoggerConfig
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RemoteLogger.initialize(RemoteLoggerConfig("192.168.0.3", 43568, 100))
        RemoteLogger.setLoggerId("TEST_LOGGER")

        btnSendLog.setOnClickListener {
            RemoteLogger.d("MainActivity_D", "test debug message, $this")
            RemoteLogger.w("MainActivity_W", "test warn message, $this")
            RemoteLogger.i("MainActivity_I", "test info message, $this")
            RemoteLogger.v("MainActivity_V", "test verbose message, $this")
            RemoteLogger.e("MainActivity_E", "test error message, $this", Exception("Test exception"))
        }
    }
}
