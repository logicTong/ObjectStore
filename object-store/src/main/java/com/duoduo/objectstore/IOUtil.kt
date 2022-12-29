package com.duoduo.objectstore

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 *  author : tianhetong
 *  date : 2022/12/29
 *  description :
 */
object IOUtil {

    private const val TAG = "IOUtil"

    fun objectToBuffer(obj: Serializable): ByteArray? {
        val bufferStream = ByteArrayOutputStream()
        try {
            val objectOutStream = ObjectOutputStream(bufferStream)
            objectOutStream.writeObject(obj)
            return bufferStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "objectToBuffer: error", e)
        } finally {
            closeStream(bufferStream)
        }
        return null
    }



    fun closeStream(stream: Closeable?) {
        stream?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "closeStream: stream=${it}", e)
            }
        }
    }
}