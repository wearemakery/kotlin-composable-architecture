package composablearchitecture.example.todos

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import composablearchitecture.Store
import composablearchitecture.example.todos.databinding.TodosActivityBinding
import kotlinx.coroutines.launch
import java.util.UUID

class TodosActivityViewModel : ViewModel()

class TodosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil
            .setContentView<TodosActivityBinding>(this, R.layout.todos_activity)
        binding.lifecycleOwner = this

        lifecycleScope.launch {
            val scopedStore: Store<List<Todo>, Pair<UUID, TodoAction>> = TodosApp.store.scope(
                toLocalState = AppState.todos,
                fromLocalAction = TodoAction.prism
            )
            val todoAdapter = TodoAdapter(scopedStore, lifecycleScope)
            binding.todosRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = todoAdapter
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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
