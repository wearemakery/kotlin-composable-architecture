package composablearchitecture.example.todos

import arrow.core.left
import arrow.core.right
import arrow.optics.Prism
import arrow.optics.optics
import composablearchitecture.Reducer
import composablearchitecture.cancellable
import composablearchitecture.debug
import composablearchitecture.withEffect
import composablearchitecture.withNoEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

@optics
data class Todo(
    var description: String = "",
    val id: UUID,
    var isComplete: Boolean = false
) {
    companion object
}

sealed class TodoAction {
    class CheckBoxToggled(val checked: Boolean) : TodoAction()
    class TextFieldChanged(val text: String) : TodoAction()

    companion object {
        val prism: Prism<AppAction, Pair<UUID, TodoAction>> = Prism(
            getOrModify = { appAction ->
                when (appAction) {
                    is AppAction.Todo -> (appAction.id to appAction.action).right()
                    else -> appAction.left()
                }
            },
            reverseGet = { (id, action) -> AppAction.Todo(id, action) }
        )
    }
}

object TodoEnvironment

val todoReducer = Reducer<Todo, TodoAction, TodoEnvironment> { todo, action, _ ->
    when (action) {
        is TodoAction.TextFieldChanged -> {
            Todo.description
                .set(todo, action.text)
                .withNoEffect()
        }
        is TodoAction.CheckBoxToggled ->
            Todo.isComplete
                .set(todo, action.checked)
                .withNoEffect()
    }
}

enum class EditMode {
    active, inactive
}

enum class Filter {
    All,
    Active,
    Completed
}

@optics
data class AppState(
    val editMode: EditMode = EditMode.inactive,
    val filter: Filter = Filter.All,
    val todos: List<Todo> = emptyList()
) {
    companion object

    val filteredTodos: List<Todo>
        get() = when (filter) {
            Filter.All -> todos
            Filter.Active -> todos.filter { !it.isComplete }
            Filter.Completed -> todos.filter { it.isComplete }
        }

    fun sortCompleted(): List<Todo> =
        todos
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<Todo>> { it.value.isComplete }.thenBy { it.index }
            )
            .map { it.value }
}

sealed class AppAction : Comparable<AppAction> {
    object AddTodoButtonTapped : AppAction()
    object ClearCompletedButtonTapped : AppAction()
    object SortCompletedTodos : AppAction()
    class Todo(val id: UUID, val action: TodoAction) : AppAction()

    override fun compareTo(other: AppAction): Int = this.compareTo(other)
}

class AppEnvironment(
    var asyncDispatcher: CoroutineDispatcher = Dispatchers.Default,
    var uuid: () -> UUID
)

val appReducer = Reducer
    .combine(
        Reducer<AppState, AppAction, AppEnvironment> { state, action, environment ->
            when (action) {
                is AppAction.AddTodoButtonTapped -> {
                    AppState.todos
                        .set(
                            state,
                            state.todos.plus(Todo(id = environment.uuid()))
                        )
                        .withNoEffect()
                }
                AppAction.SortCompletedTodos -> {
                    AppState.todos
                        .set(
                            state,
                            state.sortCompleted()
                        )
                        .withNoEffect()
                }
                is AppAction.Todo -> {
                    if (action.action is TodoAction.CheckBoxToggled) {
                        state
                            .withEffect<AppState, AppAction> {
                                withContext(environment.asyncDispatcher) { delay(1000L) }
                                emit(AppAction.SortCompletedTodos)
                            }
                            .cancellable("TodoCompletionId", cancelInFlight = true)
                    } else {
                        state.withNoEffect()
                    }
                }
                else -> state.withNoEffect()
            }
        },
        todoReducer.forEach(
            toLocalState = AppState.todos,
            toLocalAction = TodoAction.prism,
            toLocalEnvironment = { TodoEnvironment },
            idGetter = Todo.id.asGetter()
        )
    )
    .debug()
