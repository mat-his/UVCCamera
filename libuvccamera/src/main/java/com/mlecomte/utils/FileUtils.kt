package com.mlecomte.utils

import java.io.BufferedOutputStream
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.io.IOException

/**
 *
 * Created by jiangdongguo on 2017/10/18.
 */
object FileUtilsManager {
    private var outputStream: BufferedOutputStream? = null
    var ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath + File.separator
    @JvmStatic
    fun createfile(path: String?) {
        val file = path?.let { File(it) }
        if (file != null) {
            if (file.exists()) {
                file.delete()
            }
        }
        try {
            outputStream = BufferedOutputStream(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun releaseFile() {
        try {
            if (outputStream != null) {
                outputStream!!.flush()
                outputStream!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun putFileStream(data: ByteArray?, offset: Int, length: Int) {
        if (outputStream != null) {
            try {
                outputStream!!.write(data, offset, length)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun putFileStream(data: ByteArray?) {
        if (outputStream != null) {
            try {
                outputStream!!.write(data)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}