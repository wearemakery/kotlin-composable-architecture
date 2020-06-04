package co.makery.tca

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import co.makery.tca.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    val appState: MutableLiveData<AppState> = MutableLiveData()

    init {
        viewModelScope.launch {
            TCApp.store.observe { newState ->
                appState.value = newState
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        val viewModel by viewModels<MainActivityViewModel>()
        binding.vm = viewModel

        startActivity(Intent(baseContext, CounterActivity::class.java))

        lifecycleScope.launch {
            delay(5000L)
            TCApp.store.send(AppAction.Counter(CounterAction.Increment))
        }
    }
}
