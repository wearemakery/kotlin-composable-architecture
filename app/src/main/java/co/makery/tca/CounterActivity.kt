package co.makery.tca

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import co.makery.tca.databinding.ActivityCounterBinding

class CounterActivityViewModel : ScopedViewModel<AppState, AppAction, CounterState, CounterAction>(
    AppState.counterState,
    CounterAction.prism
) {
    fun onIncrementTap() {
        store.send(CounterAction.Increment)
    }
}

class CounterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding =
            DataBindingUtil.setContentView<ActivityCounterBinding>(this, R.layout.activity_counter)
        binding.lifecycleOwner = this

        val viewModel by viewModels<CounterActivityViewModel>()
        viewModel.start(TCApp.store)

        binding.vm = viewModel
    }
}
