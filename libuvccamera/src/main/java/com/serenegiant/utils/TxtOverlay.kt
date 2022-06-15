package com.serenegiant.utils

import android.content.Context
import java.io.IOException

/**
 * Created by jiangdg on 2020/1/11.
 */
class TxtOverlay private constructor(private val context: Context) {
    companion object {
        private var instance: TxtOverlay? = null
        @JvmStatic
        fun getInstance(): TxtOverlay? {
            requireNotNull(instance) { "please call install in your application!" }
            return instance
        }

        @JvmStatic
        fun install(context: Context) {
            if (instance == null) {
                instance = TxtOverlay(context.applicationContext)
                val youyuan = context.getFileStreamPath("SIMYOU.ttf")
                if (!youyuan.exists()) {
                    val am = context.assets
                    try {
                        val `is` = am.open("zk/SIMYOU.ttf")
                        val os = context.openFileOutput("SIMYOU.ttf", Context.MODE_PRIVATE)
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (`is`.read(buffer).also { len = it } != -1) {
                            os.write(buffer, 0, len)
                        }
                        os.close()
                        `is`.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private external fun txtOverlayInit(width: Int, height: Int, fonts: String): Long
        private external fun txtOverlay(ctx: Long, data: ByteArray, txt: String)
        private external fun txtOverlayRelease(ctx: Long)

        init {
            System.loadLibrary("TxtOverlay")
        }
    }

    private var ctx: Long = 0
    fun init(width: Int, height: Int) {
        val youyuan = context.getFileStreamPath("SIMYOU.ttf")
        require(youyuan.exists()) { "the font file must be exists,please call install before!" }
        ctx = txtOverlayInit(width, height, youyuan.absolutePath)
    }

    fun overlay(
        data: ByteArray,
        txt: String
    ) {
//        txt = "drawtext=fontfile="+context.getFileStreamPath("SIMYOU.ttf")+": text='EasyPusher 2017':x=(w-text_w)/2:y=H-60 :fontcolor=white :box=1:boxcolor=0x00000000@0.3";
//        txt = "movie=/sdcard/qrcode.png [logo];[in][logo] "
//                + "overlay=" + 0 + ":" + 0
//                + " [out]";
//        if (ctx == 0) throw new RuntimeException("init should be called at first!");
        if (ctx == 0L) return
        txtOverlay(ctx, data, txt)
    }

    fun release() {
        if (ctx == 0L) return
        txtOverlayRelease(ctx)
        ctx = 0
    }
}