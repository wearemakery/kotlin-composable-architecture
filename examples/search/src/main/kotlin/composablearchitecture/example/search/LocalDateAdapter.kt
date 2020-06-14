package composablearchitecture.example.search

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateAdapter {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @FromJson
    fun fromJson(json: String): LocalDate = LocalDate.parse(json, formatter)

    @ToJson
    fun toJson(date: LocalDate): String = formatter.format(date)
}
