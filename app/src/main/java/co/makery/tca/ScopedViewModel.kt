package co.makery.tca

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.optics.Lens
import arrow.optics.Prism
import kotlinx.coroutines.launch

open class ScopedViewModel<GlobalState, GlobalAction, LocalState, LocalAction>(
    private val lens: Lens<GlobalState, LocalState>,
    private val prism: Prism<GlobalAction, LocalAction>
) : ViewModel() {

    val state: MutableLiveData<LocalState> = MutableLiveData()

    protected lateinit var store: Store<LocalState, LocalAction>

    fun start(withStore: Store<GlobalState, GlobalAction>) {
        viewModelScope.launch {
            store = withStore.scope(lens, prism)
            store.observe { state.value = it }
        }
    }
}
