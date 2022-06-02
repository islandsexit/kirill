package com.bignerdranch.android.criminalintent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object PicturesUtils {


    fun getScaledBitmap(path: String, destWidth:Int, destHeight:Int): Bitmap {

        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        var inSampleSize = 1
        if ( srcHeight> destHeight || srcWidth > destWidth){
            val heightScale = srcHeight /destHeight
            val widthScale = srcWidth / destWidth

            val sampleScale = if (heightScale > widthScale){
                heightScale
            }
            else{
                widthScale
            }
            inSampleSize = Math.round(sampleScale)

        }

        options = BitmapFactory.Options()
        options.inSampleSize = inSampleSize

        return BitmapFactory.decodeFile(path, options)
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


    fun getImg_64(crime: Crime):String{
        //todo удалить строку, на которую нажал и коорая не отправилась
        if(File(crime.img_path).exists() && crime.img_path != "") {
            val bOut2 = ByteArrayOutputStream()
            var bm = BitmapFactory.decodeFile(crime.img_path)
            bm = getResizedBitmap(bm, 720, 480)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            var img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
            return img64_full
        }
        return ""
    }
}