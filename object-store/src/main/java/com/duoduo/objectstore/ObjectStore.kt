package com.duoduo.objectstore

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import java.io.File
import java.io.Serializable

/**
 *  author : tianhetong
 *  date : 2022/12/28
 *  description : serialize the object to a file
 */
class ObjectStore<T : Serializable>(context: Context, fileName: String, dir: File? = null) :
    Handler.Callback {


    companion object {
        private const val TAG = "ObjectStore"
        private const val DEFAULT_DIR_NAME = ".object_store"
        private const val MIN_WRITE_INTERVAL = 400L
        private const val MSG_WRITE_FILE = 1
        private const val MSG_READ_FILE = 2
        private const val MSG_DELETE_FILE = 3
        private val syncThread = HandlerThread("object-store-sync")
    }

    private val file: File
    private val tempFile: File
    private val fileDir: File
    private val handler: Handler

    init {
        fileDir = dir ?: File(context.filesDir, DEFAULT_DIR_NAME)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        file = File(fileDir, "${fileName}.obj")
        tempFile = File(fileDir, "${fileName}obj.temp")
        log("<init>: file=${file}, previous version file exist=${file.exists()}")
        startThread()
        handler = Handler(syncThread.looper, this)
    }

    private fun startThread() {
        if (!syncThread.isAlive) {
            synchronized(ObjectStore::class.java) {
                if (!syncThread.isAlive) {
                    syncThread.start()
                }
            }
        }
        Log.d(TAG, "startThread: syncThread started, isAlive=${syncThread.isAlive}")
    }

    fun write(writeObject: T?) {
        handler.removeMessages(MSG_WRITE_FILE)
        Message.obtain().apply {
            what = MSG_WRITE_FILE
            obj = writeObject
        }.let {
            handler.sendMessageDelayed(it, MIN_WRITE_INTERVAL)
            Log.d(TAG, "write: schedule write to file")
        }
    }


    fun read(callback: (result: T?) -> Unit) {
        handler.removeMessages(MSG_READ_FILE)
        Message.obtain().apply {
            what = MSG_READ_FILE
            obj = callback
        }.let {
            handler.sendMessage(it)
        }

    }

    fun clear() {
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessage(MSG_DELETE_FILE)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_WRITE_FILE -> {
                writeSync(msg.obj)
            }
            MSG_DELETE_FILE -> {
                deleteFile()
            }
            MSG_READ_FILE -> {
                val getResult = msg.obj as (T?) -> Unit
                getResult(read0())
            }
        }
        return false
    }

    private fun read0(): T? {
        return try {
            FileUtils.readFromFile(file) as T?
        } catch (e: Exception) {
            Log.e(TAG, "read0: error", e)
            null
        }
    }

    private fun deleteFile() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "deleteFile: $file delete success")
            }
        }
    }


    private fun writeSync(writeObject: Any?) {
        if (writeObject == null) {
            FileUtils.deleteFile(file)
            FileUtils.deleteFile(tempFile)
            Log.d(TAG, "writeSync: writeObject is null, delete file=${file}")
            return
        }
        if (tempFile.exists()) {
            FileUtils.deleteFile(tempFile)
        } else {
            tempFile.parentFile?.mkdirs()
        }
        try {
            Log.d(TAG, "writeSync: write start...")
            FileUtils.writeToFile(writeObject, tempFile, false)
            //write success
            if (file.exists()) {
                FileUtils.deleteFile(file)
            }
            if (tempFile.renameTo(file)) {
                Log.d(TAG, "writeSync: success, rename $tempFile to $file")
            } else {
                Log.e(TAG, "writeSync: rename $tempFile to $file error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeSync: error", e)
        }
    }


    private fun log(msg: String, isError: Boolean = false, error: Throwable? = null) {
        if (isError) {
            Log.e(TAG, msg, error)
        } else {
            Log.d(TAG, msg)
        }
    }


}


