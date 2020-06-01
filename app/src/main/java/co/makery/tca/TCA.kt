package co.makery.tca

import arrow.core.left
import arrow.core.right
import arrow.optics.Lens
import arrow.optics.Prism
import arrow.optics.optics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

typealias ReducerResult<State, Action> = Pair<State, Effect<Action, Nothing>>

typealias ReducerFn<State, Action, Environment> = (State, Action, Environment) -> ReducerResult<State, Action>
typealias ReducerNoEnvFn<State, Action> = (State, Action) -> ReducerResult<State, Action>

class Reducer<State, Action, Environment>(
    val reducer: ReducerFn<State, Action, Environment>
) {
    companion object {
        operator fun <State, Action, Environment> invoke(
            reducer: ReducerFn<State, Action, Environment>
        ): Reducer<State, Action, Environment> = Reducer(reducer)

        fun <State, Action, Environment> combine(vararg reducers: Reducer<State, Action, Environment>) =
            Reducer<State, Action, Environment> { value, action, environment ->
                reducers.fold(Pair(value, Effect.none())) { result, reducer ->
                    val (currentValue, currentEffect) = result
                    val (newValue, newEffect) = reducer.run(currentValue, action, environment)
                    // TODO: merge newEffect into currentEffect
                    Pair(newValue, currentEffect)
                }
            }
    }

    fun run(
        state: State,
        action: Action,
        environment: Environment
    ): ReducerResult<State, Action> = reducer(state, action, environment)

    fun combine(other: Reducer<State, Action, Environment>) = combine(this, other)

    fun <GlobalState, GlobalAction, GlobalEnvironment> pullback(
        toLocalState: Lens<GlobalState, State>,
        toLocalAction: Prism<GlobalAction, Action>,
        toLocalEnvironment: (GlobalEnvironment) -> Environment
    ): Reducer<GlobalState, GlobalAction, GlobalEnvironment> =
        Reducer { globalState, globalAction, globalEnvironment ->
            toLocalAction.getOption(globalAction).fold(
                { Pair(globalState, Effect.none()) },
                { localAction ->
                    val (state, effect) = reducer(
                        toLocalState.get(globalState),
                        localAction,
                        toLocalEnvironment(globalEnvironment)
                    )
                    Pair(
                        toLocalState.set(globalState, state),
                        effect.map(toLocalAction::reverseGet)
                    )
                }
            )
        }
}

class Effect<Output, Failure : Throwable> {
    companion object {
        fun <Output> none() = Effect<Output, Nothing>()
    }

    fun <T> map(transform: (Output) -> T): Effect<T, Failure> {
        // TODO: dummy implementation
        return this as Effect<T, Failure>
    }
}

fun <State, Output> State.withNoEffect() = Pair(this, Effect.none<Output>())

class Store<State, Action>(
    initialState: State,
    val reducer: ReducerNoEnvFn<State, Action>
) {
    private val mutableState = MutableStateFlow(initialState)

    companion object {
        operator fun <State, Action, Environment> invoke(
            initialState: State,
            reducer: Reducer<State, Action, Environment>,
            environment: Environment
        ): Store<State, Action> =
            Store(
                initialState,
                { state, action: Action -> reducer.run(state, action, environment) }
            )
    }

    suspend fun <LocalState, LocalAction> scope(
        toLocalState: (State) -> LocalState,
        fromLocalAction: (LocalAction) -> Action
    ): Store<LocalState, LocalAction> {
        val localStore = Store<LocalState, LocalAction>(
            initialState = toLocalState(mutableState.value),
            reducer = { _, localAction ->
                send(fromLocalAction(localAction))
                toLocalState(mutableState.value).withNoEffect()
            }
        )
        GlobalScope.launch(Dispatchers.Unconfined) {
            mutableState.collect { newValue ->
                localStore.mutableState.value = toLocalState(newValue)
            }
        }
        return localStore
    }

    fun send(action: Action) {
        val (newState, effect) = reducer(mutableState.value, action)
        mutableState.value = newState
    }

    suspend fun observe(observer: (State) -> Unit) = coroutineScope {
        launch(Dispatchers.Unconfined) {
            mutableState.collect { state -> observer(state) }
        }
    }
}

class AppEnvironment

sealed class AppAction {
    object Increment : AppAction()
    object Decrement : AppAction()
}

sealed class CounterAction {
    object Increment : CounterAction()

    companion object {
        val prism: Prism<AppAction, CounterAction> = Prism(
            getOrModify = { appAction ->
                when (appAction) {
                    AppAction.Increment -> Increment.right()
                    else -> appAction.left()
                }
            },
            reverseGet = { counterAction ->
                when (counterAction) {
                    Increment -> AppAction.Increment
                }
            }
        )
    }
}

@optics
data class NestedState(var text: String = "") {
    companion object
}

@optics
data class AppState(val counter: Int = 0, val nested: NestedState = NestedState()) {
    companion object
}

@optics
data class CounterState(val counter: Int, val text: String) {
    companion object {
        val lens: Lens<AppState, CounterState> = Lens(
            get = { appState -> CounterState(appState.counter, appState.nested.text) },
            set = { appState, counterState ->
                // TODO: ergonomics
                var state = appState
                state = AppState.counter.set(state, counterState.counter)
                state = AppState.nested.text.set(state, counterState.text)
                state
            }
        )
    }
}

val counterReducer =
    Reducer<CounterState, CounterAction, AppEnvironment> { state, action, environment ->
        when (action) {
            CounterAction.Increment -> CounterState.counter.set(state, state.counter + 1)
                .withNoEffect()
        }
    }

val debugReducer = Reducer<AppState, AppAction, AppEnvironment> { state, action, environment ->
    println("[${Thread.currentThread().name}] [debug reducer] state=$state")
    state.withNoEffect()
}

val appReducer: Reducer<AppState, AppAction, AppEnvironment> =
    Reducer.combine(
        counterReducer.pullback(
            toLocalState = CounterState.lens,
            toLocalAction = CounterAction.prism,
            toLocalEnvironment = { env -> env }
        ),
        debugReducer
    )

fun main() {
    runBlocking {
        val store = Store(
            initialState = AppState(),
            reducer = appReducer,
            environment = AppEnvironment()
        )

        launch {
            store.observe { println("[${Thread.currentThread().name}] [global store] state=$it") }
        }

        val scopedStore = store.scope<Int, CounterAction>(
            toLocalState = { globalState -> globalState.counter },
            fromLocalAction = { localAction ->
                when (localAction) {
                    CounterAction.Increment -> AppAction.Increment
                }
            }
        )

        launch {
            scopedStore.observe { println("[${Thread.currentThread().name}] [scoped store] state=$it") }
        }

        delay(500L)

        store.send(AppAction.Increment)
        store.send(AppAction.Decrement)
        scopedStore.send(CounterAction.Increment)

        println("✅")
    }
}
