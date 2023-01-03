package com.duoduo.objectstore

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.Serializable

/**
 *  author : tianhetong
 *  date : 2022/12/28
 *  description : serialize the object to a file
 */
class ObjectStore<T : Serializable>(
    context: Context,
    private val fileName: String,
    dir: File? = null
) :
    Handler.Callback {


    companion object {
        private const val TAG = "ObjectStore"
        private const val DEFAULT_DIR_NAME = ".object_store"
        private const val MIN_WRITE_INTERVAL = 4000L
        private const val MSG_WRITE_FILE = 1
        private const val MSG_READ_FILE = 2
        private const val MSG_DELETE_FILE = 3
        private val syncThread = HandlerThread("object-store-sync")

    }

    private val file: File
    private val tempFile: File
    private val fileDir: File
    private val handler: Handler
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hasTaskLock = Object()

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
        log("startThread: syncThread started, isAlive=${syncThread.isAlive}")
    }

    /**
     * 将对象序列化，并异步写入文件
     */
    fun write(writeObject: T?) {
        handler.removeMessages(MSG_WRITE_FILE)
        PendingTask(MSG_WRITE_FILE) {
            writeSync(writeObject)
        }.toMessage().let {
            handler.sendMessageDelayed(it, MIN_WRITE_INTERVAL)
            log("write: schedule write to file")
        }
    }


    /**
     * 异步读取文件中的对象
     */
    fun read(returnInUIThread: Boolean = false, callback: (result: T?) -> Unit) {
        PendingTask(MSG_READ_FILE) {
            if (returnInUIThread) {
                uiHandler.post {
                    callback(read0())
                }
            } else {
                callback(read0())
            }
        }.toMessage().let {
            handler.sendMessage(it)
        }
    }

    /**
     * 返回包含数据的liveData，回调会在ui线程，
     * 可以避免页面结束之后，调用回调导致的相关问题
     */
    fun read(): LiveData<T?> {
        val liveData = MutableLiveData<T?>()
        PendingTask(MSG_READ_FILE) {
            liveData.postValue(read0())
        }.toMessage().let {
            handler.sendMessage(it)
        }
        return liveData
    }

    /**
     * 清空数据
     */
    fun clear() {
        handler.removeMessages(MSG_WRITE_FILE)
        handler.removeMessages(MSG_DELETE_FILE)
        PendingTask(MSG_READ_FILE) {
            deleteFile()
        }.toMessage().let {
            handler.sendMessage(it)
        }
    }


    /**
     * 等待所有操作完成
     */
    fun waitWriteFinish() {
        var wait = false
        synchronized(hasTaskLock) {
            while (hasPendingTask()) {
                Log.e(
                    TAG,
                    "waitWriteFinish: has pending task, blocking thread=${Thread.currentThread().name}, for wait all task execute finish"
                )
                wait = true
                hasTaskLock.wait()
            }
        }
        if (wait) {
            log("waitWriteFinish: continue thread=${Thread.currentThread().name}")
        }
    }


    private fun hasPendingTask(): Boolean {
        return handler.hasMessages(MSG_READ_FILE) ||
                handler.hasMessages(MSG_WRITE_FILE) ||
                handler.hasMessages(MSG_DELETE_FILE)
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.obj is ObjectStore<*>.PendingTask) {
            msg.obj.let {
                it as ObjectStore<*>.PendingTask
            }.also {
                it.run()
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
                log("deleteFile: $file delete success")
            }
        }
    }


    private fun writeSync(writeObject: Any?) {
        if (writeObject == null) {
            FileUtils.deleteFile(file)
            FileUtils.deleteFile(tempFile)
            log("writeSync: writeObject is null, delete file=${file}")
            return
        }
        if (tempFile.exists()) {
            FileUtils.deleteFile(tempFile)
        } else {
            tempFile.parentFile?.mkdirs()
        }
        try {
            log("writeSync: write start...")
            FileUtils.writeToFile(writeObject, tempFile, false)
            //write success
            if (file.exists()) {
                FileUtils.deleteFile(file)
            }
            if (tempFile.renameTo(file)) {
                log("writeSync: success, rename $tempFile to $file")
            } else {
                Log.e(TAG, "writeSync: rename $tempFile to $file error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeSync: error", e)
        }
    }


    private fun log(msg: String, isError: Boolean = false, error: Throwable? = null) {
        if (isError) {
            Log.e(TAG, "[$fileName]$msg", error)
        } else {
            Log.d(TAG, "[$fileName]$msg")
        }
    }


    inner class PendingTask(private val what: Int, private val task: Runnable) : Runnable {

        private val whatName: String
            get() {
                return when (what) {
                    MSG_WRITE_FILE -> "MSG_WRITE_FILE"
                    MSG_READ_FILE -> "MSG_READ_FILE"
                    MSG_DELETE_FILE -> "MSG_DELETE_FILE"
                    else -> "UNKNOWN"
                }
            }

        override fun run() {
            try {
                log("run: start what=$whatName")
                val start = SystemClock.uptimeMillis()
                task.run()
                Log.d(
                    TAG,
                    "run: finish what=$whatName, use time=${(SystemClock.uptimeMillis() - start) / 100 / 10f}s"
                )
            } finally {
                synchronized(hasTaskLock) {
                    hasTaskLock.notifyAll()
                }
            }
        }


        fun toMessage(): Message {
            return Message.obtain().also {
                it.what = what
                it.obj = this
            }
        }

    }

}


