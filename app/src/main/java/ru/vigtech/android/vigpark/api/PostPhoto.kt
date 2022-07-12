package ru.vigtech.android.vigpark.api

import com.google.gson.annotations.SerializedName

data class PostPhoto(
   var RESULT: String,
   @SerializedName("PlateText")
   var palteNumber: String,
   var info: String = ""
)