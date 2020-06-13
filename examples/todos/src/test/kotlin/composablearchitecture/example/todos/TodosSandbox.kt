package composablearchitecture.example.todos

import composablearchitecture.Store
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import java.util.UUID

val todos = listOf(
    Todo(id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"))
)

fun main() {
    runBlocking {
        val dispatcher = TestCoroutineDispatcher()

        val store = Store(
            initialState = AppState(todos = todos),
            reducer = appReducer,
            environment = AppEnvironment(uuid = { UUID.randomUUID() }),
            mainDispatcher = dispatcher
        )

        val job = launch(dispatcher) { store.states.collect { println(it) } }

        store.send(
            AppAction.Todo(
                UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"),
                TodoAction.TextFieldChanged("Buy milk")
            )
        )

        job.cancel()
        println("✅")
    }
}
