package composablearchitecture.example.todos

import composablearchitecture.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

val todos = listOf(
    Todo(id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"))
)

fun main() {
    runBlocking {
        val store = Store(
            initialState = AppState(todos = todos),
            reducer = appReducer,
            environment = AppEnvironment(uuid = { UUID.randomUUID() }),
            mainDispatcher = Dispatchers.Unconfined
        )

        launch(Dispatchers.Unconfined) {
            store.observe { println(it) }
        }

        delay(500L)

        store.send(
            AppAction.Todo(
                UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"),
                TodoAction.TextFieldChanged("Buy milk")
            )
        )

        println("✅")
    }
}
