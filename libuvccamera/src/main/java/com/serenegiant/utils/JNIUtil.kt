package com.serenegiant.utils

/**
 */
object JNIUtil {
    /**
     * 都是Y：U：V = 4：1：1但 U与 V顺序相反。变换可逆
     *
     * @param buffer
     * @param width
     * @param height
     */
    fun yV12ToYUV420P(buffer: ByteArray?, width: Int, height: Int) {
        callMethod("YV12ToYUV420P", null, buffer!!, width, height)
    }

    /**
     * 都是Y：U+V = 4：2,但是这两者U、V方向相反。变换可逆
     *
     * @param buffer
     * @param width
     * @param height
     */
    fun nV21To420SP(buffer: ByteArray?, width: Int, height: Int) {
        callMethod("NV21To420SP", null, buffer!!, width, height)
    }

    /**
     * 旋转1个字节为单位的矩阵
     *
     * @param data   要旋转的矩阵
     * @param offset 偏移量
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    fun rotateMatrix(data: ByteArray?, offset: Int, width: Int, height: Int, degree: Int) {
        callMethod("RotateByteMatrix", null, data!!, offset, width, height, degree)
    }

    /**
     * 旋转2个字节为单位的矩阵
     *
     * @param data   要旋转的矩阵
     * @param offset 偏移量
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    fun rotateShortMatrix(data: ByteArray?, offset: Int, width: Int, height: Int, degree: Int) {
        callMethod("RotateShortMatrix", null, data!!, offset, width, height, degree)
    }

    private external fun callMethod(
        methodName: String,
        returnValue: Array<Any>?,
        vararg params: Any
    )

    init {
        System.loadLibrary("Utils")
    }
}