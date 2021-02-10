package composablearchitecture.example.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val state = remember { mutableStateOf(store.currentState) }

    LaunchedEffect("Store") {
        store.states.collect {
            state.value = it
        }
    }

    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            Column {
                Text(text = readMe)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = state.value.searchQuery,
                    onValueChange = { store.send(SearchAction.SearchQueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(state.value.locations) { location ->
                        Text(
                            text = location.title,
                            modifier = Modifier.clickable {
                                store.send(SearchAction.LocationTapped(location))
                            })
                    }
                }
            }
        }
    }
}
