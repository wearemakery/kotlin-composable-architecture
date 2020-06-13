package composablearchitecture.example.search

import arrow.core.Either
import java.time.LocalDate

val mockLocations = listOf(
    Location(1, "Brooklyn"),
    Location(2, "Los Angeles"),
    Location(3, "San Francisco")
)

val mockWeather = listOf(
    ConsolidatedWeather(
        LocalDate.now(),
        90.0,
        70.0,
        80.0,
        "Clear"
    ),
    ConsolidatedWeather(
        LocalDate.now().plusDays(1),
        70.0,
        50.0,
        60.0,
        "Rain"
    ),
    ConsolidatedWeather(
        LocalDate.now().plusDays(2),
        100.0,
        80.0,
        90.0,
        "Cloudy"
    )
)

class MockWeatherClient : WeatherClient {

    override var searchLocation: suspend (String) -> Either<Throwable, List<Location>> = {
        Either.right(mockLocations)
    }

    override var weather: suspend (Int) -> Either<Throwable, LocationWeather> = {
        Either.right(LocationWeather(mockWeather, 1))
    }
}
