package com.duoduo.objectstore

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.SystemClock
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

/**
 *  author : tianhetong
 *  date : 2022/12/28
 *  description : serialize the object to a file
 */
class ObjectStore<T>(context: Context, fileName: String, dir: File? = null) : Handler.Callback {


    companion object {
        const val TAG = "ObjectStore"
        const val DEFAULT_DIR_NAME = ".object_store"
        const val MIN_WRITE_INTERVAL = 400L
        const val MSG_WRITE_FILE = 1
        private val syncThread = HandlerThread("object-store-sync")
    }

    private val file: File
    private val tempFile: File
    private val fileDir: File
    private val handler: Handler

    init {
        fileDir = dir ?: File(context.cacheDir, DEFAULT_DIR_NAME)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        file = File(fileDir, fileName)
        tempFile = File(fileDir, "${fileName}.temp")
        Log.d(TAG, "<init>: file=${file}, previous version file exist=${file.exists()}")
        if (!syncThread.isAlive) {
            syncThread.start()
        }
        handler = Handler(syncThread.looper, this)
    }


    fun write(writeObject: T?) {
        handler.removeMessages(MSG_WRITE_FILE)
        val msg = Message.obtain().apply {
            what = MSG_WRITE_FILE
            obj = writeObject
        }
        handler.sendMessageDelayed(msg, MIN_WRITE_INTERVAL)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_WRITE_FILE -> {

            }
        }
        return false
    }


    fun writeSync(writeObject: T?) {
        if(file.renameTo(tempFile)){

        }


    }


    fun writeToFile(writeObject: T?) {
        val outStream = BufferedOutputStream(FileOutputStream(file))
        val objectStream = ObjectOutputStream(outStream)
        objectStream.writeObject(writeObject)

    }

}