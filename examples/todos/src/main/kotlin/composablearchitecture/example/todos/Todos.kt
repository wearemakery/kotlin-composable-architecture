package composablearchitecture.example.todos

import composablearchitecture.Reducer
import composablearchitecture.withNoEffect
import java.util.UUID

data class Todo(
    val description: String = "",
    val id: UUID,
    val isComplete: Boolean = false
)

sealed class TodoAction {
    object CheckBoxToggled : TodoAction()
    class TextFieldChanged(val text: String) : TodoAction()
}

object TodoEnvironment

val todoReducer = Reducer<Todo, TodoAction, TodoEnvironment> { todo, action, _ ->
    todo.withNoEffect()
}

enum class EditMode {
    active, inactive
}

enum class Filter {
    All,
    Active,
    Completed
}

data class AppState(
    val editMode: EditMode = EditMode.inactive,
    val filter: Filter = Filter.All,
    val todos: List<Todo> = emptyList()
) {
    val filteredTodos: List<Todo>
        get() = when (filter) {
            Filter.All -> todos
            Filter.Active -> todos.filter { !it.isComplete }
            Filter.Completed -> todos.filter { it.isComplete }
        }
}

sealed class AppAction {
    object AddTodoButtonTapped
    object ClearCompletedButtonTapped
    class Todo(val id: UUID, val action: TodoAction)
}

class AppEnvironment(
    val uuid: () -> UUID
)

val appReducer = Reducer.combine(
    Reducer<AppState, AppAction, AppEnvironment> { todo, action, _ ->
        todo.withNoEffect()
    }
)
