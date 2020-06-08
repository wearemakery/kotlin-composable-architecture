package composablearchitecture

import arrow.optics.Lens
import arrow.optics.Prism
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Store<State, Action>(
    initialState: State,
    val reducer: ReducerNoEnvFn<State, Action>,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val mutableState = MutableStateFlow(initialState)

    companion object {
        operator fun <State, Action, Environment> invoke(
            initialState: State,
            reducer: Reducer<State, Action, Environment>,
            environment: Environment,
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main
        ): Store<State, Action> =
            Store(
                initialState,
                { state, action: Action -> reducer.run(state, action, environment) },
                mainDispatcher
            )
    }

    suspend fun <LocalState, LocalAction> scope(
        toLocalState: Lens<State, LocalState>,
        fromLocalAction: Prism<Action, LocalAction>
    ): Store<LocalState, LocalAction> {
        val localStore = Store<LocalState, LocalAction>(
            initialState = toLocalState.get(mutableState.value),
            reducer = { _, localAction ->
                send(fromLocalAction.reverseGet(localAction))
                toLocalState.get(mutableState.value).withNoEffect()
            },
            mainDispatcher = mainDispatcher
        )
        GlobalScope.launch(Dispatchers.Unconfined) {
            mutableState.collect { newValue ->
                localStore.mutableState.value = toLocalState.get(newValue)
            }
        }
        return localStore
    }

    fun send(action: Action) {
        val (newState, effect) = reducer(mutableState.value, action)
        mutableState.value = newState

        GlobalScope.launch(Dispatchers.Unconfined) {
            try {
                val actions = effect.sink()
                if (actions.isNotEmpty()) {
                    withContext(mainDispatcher) {
                        actions.forEach { send(it) }
                    }
                }
            } catch (ex: Exception) {
                println("Executing effects failed: ${ex.message}")
            }
        }
    }

    suspend fun observe(observer: (State) -> Unit) =
        mutableState.collect { state -> observer(state) }
}
