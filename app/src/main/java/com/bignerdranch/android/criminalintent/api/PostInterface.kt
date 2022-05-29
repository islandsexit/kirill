package com.bignerdranch.android.criminalintent.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface PostInterface {
    @FormUrlEncoded
    @POST("")
    fun  postPlate(@Field("img64") img64: String?): Call<PostPhoto?>?
}