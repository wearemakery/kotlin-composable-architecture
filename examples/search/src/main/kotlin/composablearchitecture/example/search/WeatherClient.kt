package composablearchitecture.example.search

import arrow.core.Either
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.util.concurrent.ExecutorService

data class Location(
    @field:Json(name = "woeid") val id: Int,
    val title: String
)

data class LocationWeather(
    @field:Json(name = "consolidated_weather") var consolidatedWeather: List<ConsolidatedWeather>,
    @field:Json(name = "woeid") var id: Int
)

data class ConsolidatedWeather(
    @field:Json(name = "applicable_date") val applicableDate: LocalDate,
    @field:Json(name = "max_temp") val maxTemp: Double,
    @field:Json(name = "min_temp") val minTemp: Double,
    @field:Json(name = "the_temp") val theTemp: Double,
    @field:Json(name = "weather_state_name") val weatherStateName: String?
)

private interface WeatherClientApi {

    @GET("location/search/")
    suspend fun search(@Query("query") query: String): List<Location>

    @GET("location/{id}/")
    suspend fun weather(@Path("id") id: Int): LocationWeather
}

interface WeatherClient {

    var searchLocation: suspend (String) -> Either<Throwable, List<Location>>

    var weather: suspend (Int) -> Either<Throwable, LocationWeather>
}

class LiveWeatherClient(executor: ExecutorService? = null) : WeatherClient {

    private val httpClient = OkHttpClient.Builder()
        .apply { executor?.let { dispatcher(Dispatcher(it)) } }
        .build()

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl("https://www.metaweather.com/api/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val weatherClientApi = retrofit.create(WeatherClientApi::class.java)

    fun shutdown() {
        httpClient.dispatcher.executorService.shutdown()
    }

    override var searchLocation: suspend (String) -> Either<Throwable, List<Location>> = { query ->
        Either.catch {
            weatherClientApi.search(query)
        }
    }

    override var weather: suspend (Int) -> Either<Throwable, LocationWeather> = { id ->
        Either.catch {
            weatherClientApi.weather(id)
        }
    }
}

fun main() {
    runBlocking {
        val client = LiveWeatherClient()
        client.searchLocation("San").fold(
            { error -> println(error.message) },
            { locations ->
                val weather = client.weather(locations[0].id)
                println(weather)
            }
        )
        client.shutdown()
    }
}
