package composablearchitecture.example.search

import composablearchitecture.Store
import composablearchitecture.test.TestExecutorService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LiveStoreTests {

    @Test
    fun `Test store with live weather client`() {
        val dispatcher = TestCoroutineDispatcher()

        val environment = SearchEnvironment(weatherClient = LiveWeatherClient(TestExecutorService()))

        val store = Store(
            SearchState(),
            searchReducer,
            environment,
            mainDispatcher = dispatcher
        )

        runBlocking {
            store.send(SearchAction.SearchQueryChanged("San"))

            dispatcher.advanceTimeBy(300L)

            store.send(SearchAction.LocationTapped(store.currentState.locations[0]))

            println("weather=${store.currentState.locationWeather}")
            assertNotEquals(store.currentState.locationWeather, null)

            store.send(SearchAction.SearchQueryChanged("Sa"))
            store.send(SearchAction.SearchQueryChanged("S"))
            store.send(SearchAction.SearchQueryChanged(""))

            dispatcher.advanceTimeBy(300L)

            assertEquals(store.currentState.locations, emptyList<Location>())
            assertEquals(store.currentState.locationWeather, null)

            (environment.weatherClient as? LiveWeatherClient)?.shutdown()

            println("✅")
        }
    }
}
