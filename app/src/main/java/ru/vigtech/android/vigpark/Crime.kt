package ru.vigtech.android.vigpark

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Crime(@PrimaryKey val id: UUID = UUID.randomUUID(),
                 var title: String = "",
                 var date: Date = Date(),
                 var isSolved: Boolean = false,
                 var suspect: String = "",
                 var img_path: String = "",
                 var img_path_full: String = "",
                 var send: Boolean = false,
                 var found: Boolean = false)