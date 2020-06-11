package composablearchitecture.example.todos

import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import composablearchitecture.example.todos.databinding.TodoItemBinding

private class TodoDiffCallback(
    private val old: List<Todo>,
    private val new: List<Todo>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].id == new[newItemPosition].id

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition] == new[newItemPosition]
}

class TodoViewHolder(
    private val binding: TodoItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(newTodo: Todo) {
        if (binding.todo != newTodo) {
            binding.todo = newTodo
            binding.executePendingBindings()
        }
    }
}

class TodoAdapter(
    private val onDescriptionChange: (Todo, String) -> Unit,
    private val onCompleteChange: (Todo, Boolean) -> Unit
) : RecyclerView.Adapter<TodoViewHolder>() {

    private var todos: MutableList<Todo> = mutableListOf()

    fun update(newTodos: List<Todo>) {
        val results = DiffUtil.calculateDiff(TodoDiffCallback(todos, newTodos))
        todos.clear()
        todos.addAll(newTodos)
        results.dispatchUpdatesTo(this@TodoAdapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TodoItemBinding.inflate(inflater, parent, false)
        binding.adapter = this
        return TodoViewHolder(binding)
    }

    override fun getItemCount(): Int = todos.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    fun descriptionChanged(todo: Todo, editable: Editable) {
        val description = editable.toString()
        todo.description = description
        onDescriptionChange(todo, description)
    }

    fun completeChanged(todo: Todo, checked: Boolean) {
        todo.isComplete = checked
        onCompleteChange(todo, checked)
    }
}
