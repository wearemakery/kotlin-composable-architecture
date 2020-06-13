package composablearchitecture

import arrow.optics.Lens
import arrow.optics.Prism
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Store<State, Action> private constructor(
    initialState: State,
    private val reducer: (State, Action) -> Result<State, Action>,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val mutableState = MutableStateFlow(initialState)

    private var scopeCollectionJob: Job? = null

    val states: Flow<State> = mutableState

    val currentState: State
        get() = mutableState.value

    companion object {
        operator fun <State, Action, Environment> invoke(
            initialState: State,
            reducer: Reducer<State, Action, Environment>,
            environment: Environment,
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main
        ): Store<State, Action> =
            Store(
                initialState,
                { state, action -> reducer.run(state, action, environment) },
                mainDispatcher
            )
    }

    fun <LocalState, LocalAction> scope(
        toLocalState: Lens<State, LocalState>,
        fromLocalAction: Prism<Action, LocalAction>,
        coroutineScope: CoroutineScope
    ): Store<LocalState, LocalAction> {
        val localStore = Store<LocalState, LocalAction>(
            initialState = toLocalState.get(mutableState.value),
            reducer = { _, localAction ->
                send(fromLocalAction.reverseGet(localAction))
                toLocalState.get(mutableState.value).withNoEffect()
            },
            mainDispatcher = mainDispatcher
        )
        localStore.scopeCollectionJob = coroutineScope.launch(Dispatchers.Unconfined) {
            mutableState.collect { newValue ->
                localStore.mutableState.value = toLocalState.get(newValue)
            }
        }
        return localStore
    }

    fun send(action: Action) {
        require(Thread.currentThread().name.startsWith("main")) {
            "Sending actions from background threads is not allowed"
        }

        val (newState, effect) = reducer(mutableState.value, action)
        mutableState.value = newState

        GlobalScope.launch(mainDispatcher) {
            try {
                val actions = effect.sink()
                if (actions.isNotEmpty()) {
                    withContext(mainDispatcher) {
                        actions.forEach { send(it) }
                    }
                }
            } catch (ex: CancellationException) {
                // Ignore
            }
        }
    }

    fun cancel() {
        scopeCollectionJob?.cancel()
    }
}
