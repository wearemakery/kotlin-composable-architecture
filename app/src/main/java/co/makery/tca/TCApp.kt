package co.makery.tca

import android.app.Application

class TCApp : Application() {

    companion object {
        lateinit var store: Store<AppState, AppAction>
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(
            initialState = AppState(),
            reducer = appReducer,
            environment = AppEnvironment()
        )
    }
}
