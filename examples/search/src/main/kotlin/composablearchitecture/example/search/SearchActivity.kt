package composablearchitecture.example.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import composablearchitecture.example.search.databinding.SearchActivityBinding
import kotlinx.coroutines.flow.collect

class SearchActivity : AppCompatActivity() {

    private val store = SearchApp.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil
            .setContentView<SearchActivityBinding>(this, R.layout.search_activity)
        binding.lifecycleOwner = this

        binding.searchInput.doAfterTextChanged { text ->
            store.send(SearchAction.SearchQueryChanged(text.toString()))
        }

        val searchAdapter = SearchAdapter(onLocationTap = { store.send(SearchAction.LocationTapped(it)) })
        binding.searchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.searchCreditButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.metaweather.com/")))
        }

        lifecycleScope.launchWhenCreated {
            store.states.collect { state -> searchAdapter.update(state) }
        }
    }
}
