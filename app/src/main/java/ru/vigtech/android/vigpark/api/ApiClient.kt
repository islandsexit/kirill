package ru.vigtech.android.vigpark.api

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.vigtech.android.vigpark.Crime
import ru.vigtech.android.vigpark.CrimeRepository
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext


object ApiClient {
    var baseUrl = "http://95.182.74.37:1234/"
    var retrofit: Retrofit = getRetroInstance(baseUrl)





        private fun getRetroInstance(baseUrl: String): Retrofit {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
            val gsonBuilder = GsonBuilder()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .build()
        }

    fun reBuildRetrofit(ip: String){
        retrofit = getRetroInstance(ip)
    }



    fun POST_img64( img64_full: String, img_path: String, img_plate_path:String, zone:Int) {
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val crime = Crime(title = "Отправка на сервер", img_path = img_path, img_path_full = img_plate_path, send = true, found = true, Zone=zone)
        CrimeRepository.get().addCrime(crime)
        val call: Call<PostPhoto> = post_api.postPlate(img64_full,zone)
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        Log.w("POST", "onResponse| response: Result: $RESULT msg: $msg, -- , $zone")
                        if (RESULT == "SUCCESS") {
                            crime.title = msg
                            crime.send = true
                            crime.found = true
                            CrimeRepository.get().updateCrime(crime)
                        } else {
                            crime.title = "Не распознан номер"
                            crime.send = true
                            crime.found = false
                            CrimeRepository.get().updateCrime(crime)
                        }
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.title = "Не распознан номер"
                        crime.send = false
                        crime.found = false
                        CrimeRepository.get().updateCrime(crime)
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    crime.title = "Не распознан номер"
                    crime.send = false
                    crime.found = false
                    CrimeRepository.get().updateCrime(crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                crime.title = "Не распознан номер"
                crime.send = false
                crime.found = false
                CrimeRepository.get().updateCrime(crime)

            }
        })


    }

    fun POST_img64(img64: String, crime: Crime) {
        crime.title = "Отправка на сервер"
        CrimeRepository.get().updateCrime(crime)
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64,crime.Zone)
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
                crime.send = false
                CrimeRepository.get().updateCrime(crime)

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
        val call: Call<PostPhoto> = post_api.postPlateEdited(img64, crime.title,crime.Zone)
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



