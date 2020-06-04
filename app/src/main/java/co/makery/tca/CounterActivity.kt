package co.makery.tca

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.makery.tca.databinding.ActivityCounterBinding
import kotlinx.coroutines.launch

class CounterActivityViewModel : ViewModel() {

    val state: MutableLiveData<CounterState> = MutableLiveData()

    private lateinit var scopedStore: Store<CounterState, CounterAction>

    init {
        viewModelScope.launch {
            scopedStore = TCApp.store.scope(
                toLocalState = AppState.counterState,
                fromLocalAction = CounterAction.prism
            )
            scopedStore.observe { newState ->
                println("[${Thread.currentThread().name}] [scoped store] state=$newState")
                state.value = newState
            }
        }
    }

    fun onIncrementTap() {
        scopedStore.send(CounterAction.Increment)
    }
}

class CounterActivity : AppCompatActivity() {

    private val viewModel by viewModels<CounterActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding =
            DataBindingUtil.setContentView<ActivityCounterBinding>(this, R.layout.activity_counter)

        binding.lifecycleOwner = this
        binding.vm = viewModel
    }
}
