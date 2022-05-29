package com.bignerdranch.android.criminalintent.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    const val BASE_URL = "http://"

    val apiClient: PostInterface by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return@lazy retrofit.create(PostInterface::class.java)
    }



}