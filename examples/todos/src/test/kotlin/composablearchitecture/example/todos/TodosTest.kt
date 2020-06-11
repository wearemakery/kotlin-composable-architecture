package composablearchitecture.example.todos

import composablearchitecture.test.TestStore
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test
import java.util.UUID

class TodosTest {

    @Test
    fun `When add todo is tapped new item is showing up`() {
        val testDispatcher = TestCoroutineDispatcher()

        val store = TestStore(
            AppState(),
            appReducer,
            AppEnvironment(
                asyncDispatcher = testDispatcher,
                uuid = { UUID.randomUUID() }
            ),
            testDispatcher
        )

        store.assert {
            environment {
                it.uuid = { UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF") }
            }
            send(AppAction.AddTodoButtonTapped) {
                AppState(
                    todos = listOf(
                        Todo(id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"))
                    )
                )
            }
            environment {
                it.uuid = { UUID.fromString("00000000-0000-0000-0000-000000000000") }
            }
            send(AppAction.AddTodoButtonTapped) {
                AppState(
                    todos = listOf(
                        Todo(id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF")),
                        Todo(id = UUID.fromString("00000000-0000-0000-0000-000000000000"))
                    )
                )
            }
            send(
                AppAction.Todo(
                    UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"),
                    TodoAction.CheckBoxToggled(true)
                )
            ) {
                AppState(
                    todos = listOf(
                        Todo(
                            id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"),
                            isComplete = true
                        ),
                        Todo(id = UUID.fromString("00000000-0000-0000-0000-000000000000"))
                    )
                )
            }
            doBlock {
                testDispatcher.advanceTimeBy(1000L)
            }
            receive(AppAction.SortCompletedTodos) {
                AppState(
                    todos = listOf(
                        Todo(id = UUID.fromString("00000000-0000-0000-0000-000000000000")),
                        Todo(
                            id = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEDDEADBEEF"),
                            isComplete = true
                        )
                    )
                )
            }
        }
    }
}
