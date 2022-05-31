package com.bignerdranch.android.criminalintent.api

import com.google.gson.annotations.SerializedName

data class PostPhoto(
   var RESULT: String,
   @SerializedName("PlateText")
   var palteNumber: String
)