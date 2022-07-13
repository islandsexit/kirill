package ru.vigtech.android.vigpark.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*
import kotlin.collections.ArrayList

@Entity
data class Crime(@PrimaryKey var id: UUID = UUID.randomUUID(),
                 var title: String = "",
                 var date: Date = Date(),
                 var isSolved: Boolean = false,
                 var suspect: String = "",
                 var img_path: String = "",
                 var img_path_full: String = "",
                 var send: Boolean = false,
                 var found: Boolean = false,
                 var Zone: Int = 1,
                 var lon: Double =0.0,
                 var lat:Double = 0.0,
                 var info: String = "",
                 var Rect: ArrayList<String?>? = ArrayList<String?>()){
    override fun toString(): String {


        return "Crime ID:${id}, title:$title, date:$date, img_path:$img_path, img_path_full:$img_path_full, send:$send, found:$found, Zone:$Zone,  long:$lon, lat:$lat"
    }
}

