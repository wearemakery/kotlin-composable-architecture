package composablearchitecture.example.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.launchInComposition
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.AdapterList
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.height
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.res.stringResource
import androidx.ui.unit.dp
import composablearchitecture.Store
import kotlinx.coroutines.flow.collect

class ComposeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchView(
                store = SearchApp.store,
                readMe = stringResource(R.string.read_me)
            )
        }
    }
}

@Composable
fun SearchView(store: Store<SearchState, SearchAction>, readMe: String) {
    var state by state { store.currentState }

    launchInComposition {
        store.states.collect { state = it }
    }

    MaterialTheme {
        Column {
            Box(padding = 16.dp) {
                Text(text = readMe)
                Spacer(modifier = Modifier.height(16.dp))
                val queryState = state { TextFieldValue("") }
                TextField(
                    value = queryState.value,
                    onValueChange = {
                        queryState.value = it
                        store.send(SearchAction.SearchQueryChanged(it.text))
                    }
                )
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                AdapterList(data = state.locations) { location ->
                    Text(text = location.title)
                }
            }
        }
    }
}
