package composablearchitecture.example.todos

import android.app.Application
import composablearchitecture.Store
import java.util.UUID

@Suppress("unused")
class TodosApp : Application() {

    companion object {
        lateinit var store: Store<AppState, AppAction>
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(
            initialState = AppState(),
            reducer = appReducer,
            environment = AppEnvironment(uuid = { UUID.randomUUID() })
        )
    }
}
