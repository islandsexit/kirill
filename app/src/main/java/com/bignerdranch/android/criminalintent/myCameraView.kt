package com.bignerdranch.android.criminalintent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.os.Environment
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import com.bignerdranch.android.criminalintent.api.ApiClient
import org.opencv.android.JavaCameraView
import org.opencv.core.Rect
import java.io.*
import java.util.*

class myCameraView(context: Context?, attrs: AttributeSet?) :
    JavaCameraView(context, attrs), PictureCallback {
    private var mPictureFileName: String? = null
    var img64: String? = null
    var img64_full: String? = null
    private lateinit var face_array: Array<Rect>



//    var POST: Post_class = Post_class()
    val effectList: List<String>
        get() = mCamera.parameters.supportedColorEffects

    fun setFace_array(face_arrayset: Array<Rect>) {
        face_array = face_arrayset
    }

    val isEffectSupported: Boolean
        get() = mCamera.parameters.colorEffect != null
    var effect: String?
        get() = mCamera.parameters.colorEffect
        set(effect) {
            val params = mCamera.parameters
            params.colorEffect = effect
            mCamera.parameters = params
        }
    val resolutionList: List<Camera.Size>
        get() = mCamera.parameters.supportedPreviewSizes
    var resolution: Camera.Size
        get() = mCamera.parameters.previewSize
        set(resolution) {
            disconnectCamera()
            mMaxHeight = resolution.height
            mMaxWidth = resolution.width
            connectCamera(width, height)
        }

    fun takePicture(fileName: String?) {
        Log.i(TAG, "Taking picture")
        mPictureFileName = fileName
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null)

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this)
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        Log.i(TAG, "Saving a bitmap to file")
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview()
        mCamera.setPreviewCallback(this)

        // Write the image in a file (in jpeg format)
        try {

            var path_to_image = context.filesDir

            val mFile2 = File(path_to_image, "$mPictureFileName.jpg")
            if (mFile2.exists()) {
                mFile2.delete();
            }
            try {
                val fos: OutputStream = FileOutputStream(mFile2.path)
                fos.write(data);
                fos.close();
            }
            catch (e: IOException) {
                Log.e("PictureDemo", "Exception in photoCallback", e)
            }

            val fos = FileOutputStream(mFile2)
            fos.write(data)
            fos.close()
            val bOut2 = ByteArrayOutputStream()
            var bm = BitmapFactory.decodeFile(mFile2.path)
            bm = getResizedBitmap(bm, 720, 480)


//            Log.i("APP_LOG", img64_full.toString())



            val mat = Matrix()
            mat.postRotate(90f)
            bm = Bitmap.createBitmap(
                bm, 0, 0,
                bm.width, bm.height,
                mat, true
            )

            bm.compress(Bitmap.CompressFormat.JPEG, 50, bOut2)
            img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
//            Log.i(
//                "APP_LOG", """width:${bm.width}
//                         height:${bm.height}
//                         x:${face_array[0].x}
//                          y:${face_array[0].y}
//                         width:${face_array[0].width}
//                         height:${face_array[0].height}"""
//            )
//            if(face_array.isNullOrEmpty()) {
//                bm = Bitmap.createBitmap(
//                    bm,
//                    face_array[0].x,
//                    face_array[0].y,
//                    face_array[0].width,
//                    face_array[0].height
//                )
//            }
            val mFile3 = File(
                path_to_image,
                UUID.randomUUID().toString() + "_" + ".jpg"
            )
            var fos2: FileOutputStream? = null
            fos2 = FileOutputStream(mFile3)
            Log.i("APP_LOG", mFile3.absolutePath)


            bm.compress(Bitmap.CompressFormat.JPEG, 100, fos2)
            val bOut = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut)
            img64 = Base64.encodeToString(bOut.toByteArray(), Base64.DEFAULT)
            ApiClient.POST_img64(img64.toString(), img64_full.toString(), "http://192.168.48.174:8080/",img_path = mFile3.path, img_plate_path = mFile2.path )
//            CrimeRepository.get().addCrime(Crime(title = "test", img_path = mFile3.path, img_path_full = mFile2.path))
        } catch (e: IOException) {
            Log.e("APP_LOG", "Exception in photoCallback", e)
        }
    }

    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
    }

    fun rotate_array(
        face_array: Array<Rect?>?,
        height: Int,
        width: Int
    ): Array<Rect?> {
        return arrayOfNulls(3)
    }

    suspend fun createPhoto(){

    }

    companion object {
        private const val TAG = "myCameraView"
        private lateinit var face_array: Array<Rect>
    }
}

