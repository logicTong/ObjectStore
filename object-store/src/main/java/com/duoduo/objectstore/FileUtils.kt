package com.duoduo.objectstore

import android.util.Log
import java.io.*

/**
 *  author : tianhetong
 *  date : 2022/12/29
 *  description :
 */
object FileUtils {

    private const val TAG = "IOUtil"


    fun closeStream(stream: Closeable?) {
        stream?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "closeStream: stream=${it}", e)
            }
        }
    }

    fun deleteFile(file: File) {
        if (file.exists()) {
            if (file.isDirectory) {
                file.listFiles()?.forEach {
                    deleteFile(it)
                }
            }
            file.delete()
        }
    }

    fun writeToFile(obj: Any, file: File, catchError: Boolean = true) {
        var outStream: BufferedOutputStream? = null
        var objectStream: ObjectOutputStream?
        try {
            outStream = BufferedOutputStream(FileOutputStream(file))
            objectStream = ObjectOutputStream(outStream)
            objectStream.writeObject(obj)
            outStream.flush()
        } catch (e: Exception) {
            Log.e(TAG, "writeToFile: error", e)
            if (!catchError) {
                throw e
            }
        } finally {
            closeStream(outStream)
        }
    }


    fun readFromFile(file: File): Any? {
        if (!file.exists()) {
            return null
        }
        var inStream: BufferedInputStream? = null
        try {
            inStream = BufferedInputStream(FileInputStream(file))
            val objectStream = ObjectInputStream(inStream)
            return objectStream.readObject()
        } catch (e: Exception) {
            Log.e(TAG, "readFromFile: error", e)
        } finally {
            closeStream(inStream)
        }
        return null
    }
}