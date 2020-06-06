package composablearchitecture.example.todos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TodosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_todos)
        binding.lifecycleOwner = this

        val viewModel by viewModels<MainActivityViewModel>()
        binding.vm = viewModel

        startActivity(Intent(baseContext, CounterActivity::class.java))

        lifecycleScope.launch {
            delay(5000L)
            Todos.store.send(AppAction.Counter(CounterAction.Increment))
        }
        */
    }
}
