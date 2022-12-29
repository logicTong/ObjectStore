package com.duoduo.objectstore

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message

/**
 * Created by tianhetong on 2022/12/29
 */
class KeepAliveTimeThread(private val threadName: String, private val keepAliveTime: Long) :
    Handler.Callback {

    companion object {
        private const val MSG_SHUTDOWN = 99887766
    }

    private var handlerThread: HandlerThread? = null

    private var shutdownHandler: Handler? = null

    fun start() {
        if (handlerThread == null) {
            synchronized(KeepAliveTimeThread::class.java) {
                if (handlerThread == null) {
                    handlerThread = HandlerThread(threadName).also {
                        it.start()
                        shutdownHandler = Handler(it.looper)
                    }
                }
            }
        }

    }

    fun getLooper(): Looper {
        return handlerThread!!.looper
    }

    override fun handleMessage(msg: Message): Boolean {
        shutdownHandler!!.removeMessages(MSG_SHUTDOWN)
        if (msg.what == MSG_SHUTDOWN) {

        }
        shutdownHandler?.apply {
            removeMessages(MSG_SHUTDOWN)
            sendEmptyMessageDelayed(MSG_SHUTDOWN, keepAliveTime)
        }
        return false
    }

    fun shutdown() {
        handlerThread?.quitSafely()
        handlerThread = null
        shutdownHandler = null
    }

}