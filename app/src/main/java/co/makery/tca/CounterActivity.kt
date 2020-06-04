package co.makery.tca

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import kotlinx.android.synthetic.main.activity_counter.*
import kotlinx.coroutines.launch

class CounterActivity : AppCompatActivity() {
    private lateinit var scopedStore: Store<CounterState, CounterAction>

    init {
        lifecycleScope.launch {
            whenCreated {
                scopedStore = TCApp.store.scope(
                    toLocalState = { globalState -> globalState.counterState },
                    fromLocalAction = { localAction -> AppAction.Counter(localAction) }
                )
                scopedStore.observe { state ->
                    println("[${Thread.currentThread().name}] [scoped store] state=$state")
                    counterTitle.text = "$state"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)

        counterIncrementButton.setOnClickListener {
            scopedStore.send(CounterAction.Increment)
        }
    }
}
