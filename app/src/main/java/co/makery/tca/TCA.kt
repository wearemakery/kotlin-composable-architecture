package co.makery.tca

import arrow.core.left
import arrow.core.right
import arrow.optics.Lens
import arrow.optics.Prism
import arrow.optics.optics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
                    currentEffect.merge(newEffect)
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

    fun optional(): Reducer<State?, Action, Environment> = Reducer { state, action, environment ->
        if (state == null) {
            Pair(state, Effect.none())
        } else {
            reducer(state, action, environment)
        }
    }
}

class Effect<Output, Failure : Throwable>(private var flow: Flow<Output>) {

    companion object {
        fun <Output> none() = Effect<Output, Nothing>(emptyFlow())
    }

    fun <T> map(transform: (Output) -> T): Effect<T, Failure> {
        return Effect(flow.map { transform(it) })
    }

    fun merge(effect: Effect<Output, Failure>) {
        flow = flowOf(flow, effect.flow).flattenConcat()
    }

    suspend fun sink(): List<Output> = coroutineScope {
        val outputs = mutableListOf<Output>()
        flow.toList(outputs)
        outputs
    }
}

fun <State, Output> State.withNoEffect() = Pair(this, Effect.none<Output>())

fun <State, Output, Failure : Throwable> State.withEffect(block: suspend FlowCollector<Output>.() -> Unit) =
    Pair(this, Effect<Output, Failure>(flow(block)))

class Store<State, Action>(
    initialState: State,
    val reducer: ReducerNoEnvFn<State, Action>,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
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
        GlobalScope.launch {
            try {
                val actions = effect.sink()
                withContext(mainDispatcher) {
                    actions.forEach { send(it) }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
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
    class Counter(val action: CounterAction) : AppAction()
}

sealed class CounterAction {
    object Increment : CounterAction()
    object Noop : CounterAction()

    companion object {
        val prism: Prism<AppAction, CounterAction> = Prism(
            getOrModify = { appAction ->
                when (appAction) {
                    is AppAction.Counter -> appAction.action.right()
                    else -> appAction.left()
                }
            },
            reverseGet = { counterAction -> AppAction.Counter(counterAction) }
        )
    }
}

@optics
data class NestedState(var text: String = "") {
    companion object
}

@optics
data class CounterState(val counter: Int = 0, val nestedState: NestedState = NestedState()) {
    companion object
}

@optics
data class AppState(val counterState: CounterState = CounterState()) {
    companion object
}

val counterReducer =
    Reducer<CounterState, CounterAction, AppEnvironment> { state, action, environment ->
        when (action) {
            CounterAction.Increment -> CounterState.counter.set(state, state.counter + 1)
                .withEffect {
                    delay(1000L)
                    emit(CounterAction.Noop)
                }
            else -> state.withNoEffect()
        }
    }

val debugReducer = Reducer<AppState, AppAction, AppEnvironment> { state, action, environment ->
    println("[${Thread.currentThread().name}] [debug reducer] action=${action::class.simpleName} state=$state")
    state.withNoEffect()
}

val appReducer: Reducer<AppState, AppAction, AppEnvironment> =
    Reducer.combine(
        counterReducer.pullback(
            toLocalState = AppState.counterState,
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
            environment = AppEnvironment(),
            mainDispatcher = Dispatchers.Default
        )

        launch {
            store.observe { println("[${Thread.currentThread().name}] [global store] state=$it") }
        }

        val scopedStore = store.scope(
            toLocalState = AppState.counterState,
            fromLocalAction = CounterAction.prism
        )

        launch {
            scopedStore.observe { println("[${Thread.currentThread().name}] [scoped store] state=$it") }
        }

        delay(500L)

        store.send(AppAction.Counter(CounterAction.Increment))
        store.send(AppAction.Counter(CounterAction.Noop))
        scopedStore.send(CounterAction.Increment)

        println("✅")
    }
}
