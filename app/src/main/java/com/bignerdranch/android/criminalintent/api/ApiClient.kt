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


        val baseUrl = "http://192.168.48.174:8080/"

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


    fun POST_img64(img64_full: String, URL: String?, img_path: String, img_plate_path:String) {
        val gsonBuilder = GsonBuilder()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .build()
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64_full)
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
                            CrimeRepository.get().addCrime(Crime(title = msg, img_path = img_path, img_path_full = img_plate_path, send = true, found = true))
                        } else {
                            CrimeRepository.get().addCrime(Crime(title = "Не распознан номер", img_path = img_path, img_path_full = img_plate_path, send = true, found = false))
                        }
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        CrimeRepository.get().addCrime(Crime(title = "Не распознан номер", img_path = img_path, img_path_full = img_plate_path, send = false))
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    CrimeRepository.get().addCrime(Crime(title = "Не распознан номер", img_path = img_path, img_path_full = img_plate_path, send = false))

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                CrimeRepository.get().addCrime(Crime(title = "Не распознан номер", img_path = img_path, img_path_full = img_plate_path, send = false))

            }
        })


    }

    fun POST_img64(img64: String,img64_full: String, crime: Crime) {
        val gsonBuilder = GsonBuilder()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .build()
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64)
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
                            crime.found = true
                            CrimeRepository.get().updateCrime(crime)
                        } else {
                            crime.send = true
                            crime.title = "Не распознан номер"
                            crime.found = false
                            CrimeRepository.get().updateCrime(crime)
                        }
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.send = false
                        CrimeRepository.get().updateCrime(crime)
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    crime.send = false
                    CrimeRepository.get().updateCrime(crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)


            }
        })


    }


    fun POST_img64_with_edited_text(img64: String,img64_full: String, crime: Crime) {
        val gsonBuilder = GsonBuilder()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .build()
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlateEdited(img64, crime.title)
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        Log.w("POST_img64_with_edited_text", "onResponse| response: Result: $RESULT msg: $msg")
                        if (RESULT == "SUCCESS") {
                            crime.send = true
                            crime.found = true
                            if(crime.title == ""){
                                crime.title = msg
                            }
                            CrimeRepository.get().updateCrime(crime)
                        } else {
                            if(crime.title == "") {
                                crime.send = true
                                crime.found = false
                                crime.title = "Не распознан номер"
                            }
                            else{
                                crime.send = true
                                crime.found = false
                            }
                            CrimeRepository.get().updateCrime(crime)
                        }
                    } else {
                        crime.send = false
                        if(crime.title != "") {
                            crime.title  = "Сервер недоступен"
                        }
                        CrimeRepository.get().updateCrime(crime)
                        Log.e("POST_img64_with_edited_text", "onResponse | status: $statusCode")

                    }
                } catch (e: Exception) {
                    Log.e("POST_img64_with_edited_text", "onResponse | exception", e)
                    crime.send = false
                    if(crime.title != "") {
                        crime.title  = "Сервер недоступен"
                    }
                    CrimeRepository.get().updateCrime(crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST_img64_with_edited_text", "onFailure", t)
                crime.send = false
                if(crime.title != "") {
                    crime.title  = "Сервер недоступен"
                }
                CrimeRepository.get().updateCrime(crime)

            }
        })


    }




}



