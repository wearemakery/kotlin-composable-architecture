package co.makery.tca

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val store = TCApp.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startActivity(Intent(baseContext, CounterActivity::class.java))

        lifecycleScope.launch {
            delay(5000L)
            store.send(AppAction.Counter(CounterAction.Increment))
        }
    }
}
