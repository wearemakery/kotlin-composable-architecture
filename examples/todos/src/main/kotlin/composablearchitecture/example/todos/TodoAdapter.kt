package composablearchitecture.example.todos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import composablearchitecture.Store
import composablearchitecture.example.todos.databinding.TodoItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

private class TodoDiffCallback(
    private val old: List<Todo>,
    private val new: List<Todo>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].id == new[newItemPosition].id

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].description == new[newItemPosition].description
}

class TodoAdapter(
    private val store: Store<List<Todo>, Pair<UUID, TodoAction>>,
    lifecycleScope: CoroutineScope
) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(private val binding: TodoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.todoItemEditText.doAfterTextChanged { editable ->
                val todo = binding.todo
                if (editable != null && todo != null) {
                    store.send(todo.id to TodoAction.TextFieldChanged(editable.toString()))
                }
            }
        }

        fun bind(todo: Todo) {
            binding.todo = todo
        }
    }

    private var todos: List<Todo> = emptyList()

    init {
        lifecycleScope.launch {
            store.observe { newTodos ->
                val results = DiffUtil.calculateDiff(TodoDiffCallback(todos, newTodos))
                todos = newTodos
                results.dispatchUpdatesTo(this@TodoAdapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TodoItemBinding.inflate(inflater, parent, false)
        return TodoViewHolder(binding)
    }

    override fun getItemCount(): Int = todos.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }
}
