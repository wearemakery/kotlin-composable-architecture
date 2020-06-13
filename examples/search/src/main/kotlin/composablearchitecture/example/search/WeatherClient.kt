package composablearchitecture.example.search

import arrow.core.Either
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.util.concurrent.ExecutorService

@Serializable
data class Location(
    @SerialName("woeid") val id: Int,
    val title: String
)

@Serializable
data class LocationWeather(
    @SerialName("consolidated_weather") var consolidatedWeather: List<ConsolidatedWeather>,
    @SerialName("woeid") var id: Int
)

@Serializable
data class ConsolidatedWeather(
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("applicable_date") val applicableDate: LocalDate,
    @SerialName("max_temp") val maxTemp: Double,
    @SerialName("min_temp") val minTemp: Double,
    @SerialName("the_temp") val theTemp: Double,
    @SerialName("weather_state_name") val weatherStateName: String?
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

    private val contentType = "application/json".toMediaType()

    private val retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl("https://www.metaweather.com/api/")
        .addConverterFactory(
            Json(JsonConfiguration.Default.copy(ignoreUnknownKeys = true)).asConverterFactory(contentType)
        )
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
