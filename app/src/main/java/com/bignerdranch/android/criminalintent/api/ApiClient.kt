package com.bignerdranch.android.criminalintent.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.bignerdranch.android.criminalintent.Crime
import com.bignerdranch.android.criminalintent.CrimeRepository
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {


        val baseUrl = "https://192.168.48.132/"

        fun getRetroInstance(): Retrofit {

            val logging = HttpLoggingInterceptor()
            logging.level = (HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
            client.addInterceptor(logging)

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }


    fun POST_img64(img64: String,img64_full: String, URL: String?, img_path: String, img_plate_path:String) {
        val gsonBuilder = GsonBuilder()
        val retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .build()
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64_full)
        Log.e("POST", "img64:" + img64[2]+ "img64:" + img64_full[2] )
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        Log.w("POST", "onResponse| response: Result: $RESULT msg: $msg")
                        if (RESULT == "SUCCESS") {
                            CrimeRepository.get().addCrime(Crime(title = msg, img_path = img_path, img_path_full = img_plate_path, send = true))
                        } else {
                            CrimeRepository.get().addCrime(Crime(title = "NULL", img_path = img_path, img_path_full = img_plate_path, send = false))
                        }
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        CrimeRepository.get().addCrime(Crime(title = "NULL", img_path = img_path, img_path_full = img_plate_path, send = false))
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    CrimeRepository.get().addCrime(Crime(title = "NULL", img_path = img_path, img_path_full = img_plate_path, send = false))

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                CrimeRepository.get().addCrime(Crime(title = "NULL", img_path = img_path, img_path_full = img_plate_path, send = false))

            }
        })


    }

    fun POST_img64(img64: String,img64_full: String, URL: String?, crime: Crime) {
        val gsonBuilder = GsonBuilder()
        val retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .build()
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64)
        Log.e("POST", "img64:" + img64[2]+ "img64:" + img64_full[2] )
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        Log.w("POST", "onResponse| response: Result: $RESULT msg: $msg")
                        if (RESULT == "SUCCESS") {
                            crime.send = true
                            crime.title = msg
                            CrimeRepository.get().updateCrime(crime)
                        } else {

                        }
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")

                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)


                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)


            }
        })


    }



    }



