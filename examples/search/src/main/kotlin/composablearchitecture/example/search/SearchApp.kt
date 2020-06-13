package composablearchitecture.example.search

import android.app.Application
import composablearchitecture.Store

@Suppress("unused")
class SearchApp : Application() {

    companion object {
        val store = Store(
            SearchState(),
            searchReducer,
            SearchEnvironment()
        )
    }
}
