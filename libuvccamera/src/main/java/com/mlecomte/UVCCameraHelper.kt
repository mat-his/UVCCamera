package com.mlecomte

import com.mlecomte.utils.TxtOverlay.Companion.install
import com.mlecomte.usb.USBMonitor
import com.mlecomte.usb.common.UVCCameraHandler
import com.mlecomte.usb.USBMonitor.UsbControlBlock
import android.app.Activity
import com.mlecomte.usb.widget.CameraViewInterface
import android.hardware.usb.UsbDevice
import com.mlecomte.usb.USBMonitor.OnDeviceConnectListener
import java.lang.Thread
import java.lang.InterruptedException
import java.lang.NullPointerException
import java.lang.IllegalArgumentException
import com.serenegiant.uvccamera.R
import com.mlecomte.usb.common.AbstractUVCCameraHandler.OnCaptureListener
import java.io.File
import java.util.Objects
import com.mlecomte.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener
import com.mlecomte.usb.encoder.RecordParams
import com.mlecomte.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener
import android.os.Environment
import com.mlecomte.usb.DeviceFilter
import com.mlecomte.usb.Size
import com.mlecomte.usb.UVCCamera

/** UVCCamera Helper class
 *
 * Created by jiangdongguo on 2017/9/30.
 */
class UVCCameraHelper private constructor(cameraWidth: Int, cameraHeight: Int) {
    var previewWidth = 0
        private set
    var previewHeight = 0
        private set
    private var mFrameFormat = FRAME_FORMAT_MJPEG

    // USB Manager
    var uSBMonitor: USBMonitor? = null
        private set

    // Camera Handler
    private var mCameraHandler: UVCCameraHandler? = null
    private var mCtrlBlock: UsbControlBlock? = null
    private var mActivity: Activity? = null
    private var mCamView: CameraViewInterface? = null
    fun closeCamera() {
        if (mCameraHandler != null) {
            mCameraHandler!!.close()
        }
    }

    interface OnMyDevConnectListener {
        fun onAttachDev(device: UsbDevice?)
        fun onDettachDev(device: UsbDevice?)
        fun onConnectDev(device: UsbDevice?, isConnected: Boolean)
        fun onDisConnectDev(device: UsbDevice?)
    }

    fun initUSBMonitor(
        activity: Activity,
        cameraView: CameraViewInterface?,
        listener: OnMyDevConnectListener?
    ) {
        mActivity = activity
        mCamView = cameraView
        uSBMonitor = USBMonitor(activity.applicationContext, object : OnDeviceConnectListener {
            // called by checking usb device
            // do request device permission
            override fun onAttach(device: UsbDevice) {
                listener?.onAttachDev(device)
            }

            // called by taking out usb device
            // do close camera
            override fun onDettach(device: UsbDevice) {
                listener?.onDettachDev(device)
            }

            // called by connect to usb camera
            // do open camera,start previewing
            override fun onConnect(
                device: UsbDevice,
                ctrlBlock: UsbControlBlock,
                createNew: Boolean
            ) {
                mCtrlBlock = ctrlBlock
                openCamera(ctrlBlock)
                Thread { // wait for camera created
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    // start previewing
                    startPreview(mCamView)
                }.start()
                listener?.onConnectDev(device, true)
            }

            // called by disconnect to usb camera
            // do nothing
            override fun onDisconnect(device: UsbDevice, ctrlBlock: UsbControlBlock) {
                listener?.onDisConnectDev(device)
            }

            override fun onCancel(device: UsbDevice) {}
        })
        createUVCCamera()
    }

    fun createUVCCamera() {
        if (mCamView == null) throw NullPointerException("CameraViewInterface cannot be null!")

        // release resources for initializing camera handler
        if (mCameraHandler != null) {
            mCameraHandler!!.release()
            mCameraHandler = null
        }

        // initialize camera handler
        mCamView!!.aspectRatio = (previewWidth / previewHeight.toFloat()).toDouble()
        mCameraHandler = UVCCameraHandler.createHandler(
            mActivity, mCamView, 2,
            previewWidth, previewHeight, mFrameFormat
        )
    }

    fun updateResolution(width: Int, height: Int) {
        if (previewWidth == width && previewHeight == height) {
            return
        }
        previewWidth = width
        previewHeight = height
        if (mCameraHandler != null) {
            mCameraHandler!!.release()
            mCameraHandler = null
        }
        mCamView!!.aspectRatio = (previewWidth / previewHeight.toFloat()).toDouble()
        mCameraHandler = UVCCameraHandler.createHandler(
            mActivity, mCamView, 2,
            previewWidth, previewHeight, mFrameFormat
        )
        openCamera(mCtrlBlock)
        Thread { // wait for camera created
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            // start previewing
            startPreview(mCamView)
        }.start()
    }

    fun registerUSB() {
        if (uSBMonitor != null) {
            uSBMonitor!!.register()
        }
    }

    fun unregisterUSB() {
        if (uSBMonitor != null) {
            uSBMonitor!!.unregister()
        }
    }

    fun checkSupportFlag(flag: Int): Boolean {
        return mCameraHandler != null && mCameraHandler!!.checkSupportFlag(flag.toLong())
    }

    fun getModelValue(flag: Int): Int {
        return if (mCameraHandler != null) mCameraHandler!!.getValue(flag) else 0
    }

    fun setModelValue(flag: Int, value: Int): Int {
        return if (mCameraHandler != null) mCameraHandler!!.setValue(flag, value) else 0
    }

    fun resetModelValue(flag: Int): Int {
        return if (mCameraHandler != null) mCameraHandler!!.resetValue(flag) else 0
    }

    fun requestPermission(index: Int) {
        val devList = usbDeviceList
        if (devList == null || devList.isEmpty()) {
            return
        }
        val count = devList.size
        if (index >= count) throw IllegalArgumentException("index illegal,should be < devList.size()")
        if (uSBMonitor != null) {
            uSBMonitor!!.requestPermission(usbDeviceList!![index])
        }
    }

    val usbDeviceCount: Int
        get() {
            val devList = usbDeviceList
            return if (devList == null || devList.isEmpty()) {
                0
            } else devList.size
        }

    //            throw new NullPointerException("mUSBMonitor ="+mUSBMonitor+"deviceFilters=;"+deviceFilters);
    // matching all of filter devices
    val usbDeviceList: List<UsbDevice>?
        get() {
            val deviceFilters = DeviceFilter
                .getDeviceFilters(mActivity!!.applicationContext, R.xml.device_filter)
            return if (uSBMonitor == null || deviceFilters == null) null else uSBMonitor!!.getDeviceList(
                deviceFilters
            )
            // matching all of filter devices
        }

    fun capturePicture(savePath: String?, listener: OnCaptureListener?) {
        if (mCameraHandler != null && mCameraHandler!!.isOpened) {
            val file = savePath?.let { File(it) }
            if (file != null) {
                if (!Objects.requireNonNull(file.parentFile).exists()) {
                    file.parentFile?.mkdirs()
                }
            }
            mCameraHandler!!.captureStill(savePath, listener)
        }
    }

    fun startPusher(listener: OnEncodeResultListener?) {
        if (mCameraHandler != null && !isPushing) {
            mCameraHandler!!.startRecording(null, listener)
        }
    }

    fun startPusher(params: RecordParams, listener: OnEncodeResultListener?) {
        if (mCameraHandler != null && !isPushing) {
            if (params.isSupportOverlay) {
                install(mActivity!!.applicationContext)
            }
            mCameraHandler!!.startRecording(params, listener)
        }
    }

    fun stopPusher() {
        if (mCameraHandler != null && isPushing) {
            mCameraHandler!!.stopRecording()
        }
    }

    val isPushing: Boolean
        get() = if (mCameraHandler != null) {
            mCameraHandler!!.isRecording
        } else false
    val isCameraOpened: Boolean
        get() = if (mCameraHandler != null) {
            mCameraHandler!!.isOpened
        } else false

    fun release() {
        if (mCameraHandler != null) {
            mCameraHandler!!.release()
            mCameraHandler = null
        }
        if (uSBMonitor != null) {
            uSBMonitor!!.destroy()
            uSBMonitor = null
        }
    }

    fun setOnPreviewFrameListener(listener: OnPreViewResultListener?) {
        if (mCameraHandler != null) {
            mCameraHandler!!.setOnPreViewResultListener(listener)
        }
    }

    private fun openCamera(ctrlBlock: UsbControlBlock?) {
        if (mCameraHandler != null) {
            mCameraHandler!!.open(ctrlBlock)
        }
    }

    fun startPreview(cameraView: CameraViewInterface?) {
        val st = cameraView!!.surfaceTexture
        if (mCameraHandler != null) {
            mCameraHandler!!.startPreview(st)
        }
    }

    fun stopPreview() {
        if (mCameraHandler != null) {
            mCameraHandler!!.stopPreview()
        }
    }

    fun startCameraFoucs() {
        if (mCameraHandler != null) {
            mCameraHandler!!.startCameraFoucs()
        }
    }

    val supportedPreviewSizes: List<Size>?
        get() = if (mCameraHandler == null) null else mCameraHandler!!.supportedPreviewSizes

    fun setDefaultPreviewSize(defaultWidth: Int, defaultHeight: Int) {
        check(uSBMonitor == null) { "setDefaultPreviewSize should be call before initMonitor" }
        previewWidth = defaultWidth
        previewHeight = defaultHeight
    }

    fun setDefaultFrameFormat(format: Int) {
        check(uSBMonitor == null) { "setDefaultFrameFormat should be call before initMonitor" }
        mFrameFormat = format
    }

    companion object {
        @JvmField
        val ROOT_PATH = (Environment.getExternalStorageDirectory().absolutePath
                + File.separator)
        const val SUFFIX_JPEG = ".jpg"
        const val SUFFIX_MP4 = ".mp4"
        private const val TAG = "UVCCameraHelper"
        const val FRAME_FORMAT_YUYV = UVCCamera.FRAME_FORMAT_YUYV

        // Default using MJPEG
        // if your device is connected,but have no images
        // please try to change it to FRAME_FORMAT_YUYV
        const val FRAME_FORMAT_MJPEG = UVCCamera.FRAME_FORMAT_MJPEG
        const val MODE_BRIGHTNESS = UVCCamera.PU_BRIGHTNESS
        const val MODE_CONTRAST = UVCCamera.PU_CONTRAST
        private var mCameraHelper: UVCCameraHelper? = null
        @JvmStatic
        fun getInstance(cameraWidth: Int, cameraHeight: Int): UVCCameraHelper? {
            if (mCameraHelper == null) {
                mCameraHelper = UVCCameraHelper(cameraWidth, cameraHeight)
            }
            return mCameraHelper
        }
    }

    init {
        previewWidth = cameraWidth
        previewHeight = cameraHeight
    }
}