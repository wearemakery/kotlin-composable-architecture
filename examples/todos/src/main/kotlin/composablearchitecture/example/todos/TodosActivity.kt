package composablearchitecture.example.todos

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import composablearchitecture.android.ScopedViewModel
import composablearchitecture.example.todos.databinding.TodosActivityBinding
import java.util.UUID

class TodosActivityViewModel : ScopedViewModel<List<Todo>, Pair<UUID, TodoAction>>() {
    val onDescriptionChange: (Todo, String) -> Unit = { todo, description ->
        store.send(todo.id to TodoAction.TextFieldChanged(description))
    }

    val onCompleteChange: (Todo, Boolean) -> Unit = { todo, complete ->
        store.send(todo.id to TodoAction.CheckBoxToggled(complete))
    }
}

class TodosActivity : AppCompatActivity() {

    private val vm by viewModels<TodosActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil
            .setContentView<TodosActivityBinding>(this, R.layout.todos_activity)
        binding.lifecycleOwner = this

        vm.launch(
            TodosApp.store,
            AppState.todos,
            TodoAction.prism
        )
            .invokeOnCompletion { println("TodosActivityViewModel is disposed $it") }

        val todoAdapter = TodoAdapter(
            vm.onDescriptionChange,
            vm.onCompleteChange
        )

        vm.state.observe(this, Observer { todos -> todoAdapter.update(todos) })

        binding.todosRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.todos_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_todo -> {
                TodosApp.store.send(AppAction.AddTodoButtonTapped)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
