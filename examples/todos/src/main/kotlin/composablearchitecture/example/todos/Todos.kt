package composablearchitecture.example.todos

import arrow.core.left
import arrow.core.right
import arrow.optics.Prism
import arrow.optics.optics
import composablearchitecture.Reducer
import composablearchitecture.debug
import composablearchitecture.withNoEffect
import java.util.UUID

@optics
data class Todo(
    var description: String = "",
    val id: UUID,
    val isComplete: Boolean = false
) {
    companion object
}

sealed class TodoAction {
    object CheckBoxToggled : TodoAction()
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
            var newTodo = todo
            newTodo = Todo.description.set(newTodo, action.text)
            newTodo.withNoEffect()
        }
        else -> todo.withNoEffect()
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
}

sealed class AppAction {
    object AddTodoButtonTapped : AppAction()
    object ClearCompletedButtonTapped : AppAction()
    class Todo(val id: UUID, val action: TodoAction) : AppAction()
}

class AppEnvironment(
    val uuid: () -> UUID
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
                else -> state.withNoEffect()
            }
        },
        todoReducer.forEach(
            toLocalState = AppState.todos,
            toLocalAction = TodoAction.prism,
            toLocalEnvironment = { TodoEnvironment },
            findById = Todo.id
        )
    )
    .debug()
