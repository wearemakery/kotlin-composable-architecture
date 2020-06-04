package co.makery.tca

import arrow.core.left
import arrow.core.right
import arrow.optics.Lens
import arrow.optics.Prism
import arrow.optics.optics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val mutex = Mutex()
private val cancellationJobs: MutableMap<Any, MutableList<Job>> = mutableMapOf()

private suspend fun cancelJobs(id: Any?, current: Job? = null) {
    mutex.withLock {
        val jobs = cancellationJobs[id]
        if (jobs != null) {
            jobs.removeIf {
                val isCurrent = it == current
                if (!isCurrent) {
                    it.cancel()
                }
                !isCurrent
            }
            if (jobs.isEmpty()) {
                cancellationJobs.remove(id)
            }
        }
    }
}

private suspend fun removeJob(id: Any?, job: Job?) {
    mutex.withLock {
        val jobs = cancellationJobs[id]
        if (jobs != null) {
            jobs.remove(job)
            if (jobs.isEmpty()) {
                cancellationJobs.remove(id)
            }
        }
    }
}

data class ReducerResult<out State, Action>(val state: State, val effect: Effect<Action>)

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
                reducers.fold(ReducerResult(value, Effect.none())) { result, reducer ->
                    val (currentValue, currentEffect) = result
                    val (newValue, newEffect) = reducer.run(currentValue, action, environment)
                    currentEffect.merge(newEffect)
                    ReducerResult(newValue, currentEffect)
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
                { ReducerResult(globalState, Effect.none()) },
                { localAction ->
                    val (state, effect) = reducer(
                        toLocalState.get(globalState),
                        localAction,
                        toLocalEnvironment(globalEnvironment)
                    )
                    ReducerResult(
                        toLocalState.set(globalState, state),
                        effect.map(toLocalAction::reverseGet)
                    )
                }
            )
        }

    fun optional(): Reducer<State?, Action, Environment> = Reducer { state, action, environment ->
        if (state == null) {
            ReducerResult(state, Effect.none())
        } else {
            reducer(state, action, environment)
        }
    }
}


class Effect<Output>(private var flow: Flow<Output>) {

    companion object {
        fun <Output> none() = Effect<Output>(emptyFlow())

        fun <Output> cancel(id: Any) = Effect<Output>(flow { cancelJobs(id) })
    }

    var cancellationId: Any? = null
        private set

    var cancelInFlight: Boolean = false
        private set

    fun <T> map(transform: (Output) -> T): Effect<T> {
        val effect = Effect(flow.map { transform(it) })
        matchCancellation(effect)
        return effect
    }

    fun concatenate(vararg effects: Effect<Output>) {
        matchSelfCancellation(effects)
        flow = flowOf(flow, *effects.map { it.flow }.toTypedArray()).flattenConcat()
    }

    fun merge(vararg effects: Effect<Output>) {
        matchSelfCancellation(effects)
        flow = flowOf(flow, *effects.map { it.flow }.toTypedArray()).flattenMerge()
    }

    fun cancellable(id: Any, inFlight: Boolean = false): Effect<Output> = apply {
        cancellationId = id
        cancelInFlight = inFlight
    }

    suspend fun sink(): List<Output> {
        val outputs = mutableListOf<Output>()
        flow.toList(outputs)
        return outputs
    }

    private fun matchCancellation(effect: Effect<*>) {
        if (cancellationId != null) {
            effect.cancellationId = cancellationId
            effect.cancelInFlight = cancelInFlight
        }
    }

    private fun matchSelfCancellation(effects: Array<out Effect<*>>) {
        if (cancellationId == null) {
            val cancellableEffect = effects.find { it.cancellationId != null }
            cancellationId = cancellableEffect?.cancellationId
            cancelInFlight = cancellableEffect?.cancelInFlight ?: false
        }
    }
}

fun <State, Output> State.cancel(id: Any) =
    ReducerResult(this, Effect.cancel<Output>(id))

fun <State, Output> State.withNoEffect() =
    ReducerResult(this, Effect.none<Output>())

fun <State, Output> State.withEffect(block: suspend FlowCollector<Output>.() -> Unit) =
    ReducerResult(this, Effect(flow(block)))

fun <State, Output> ReducerResult<State, Output>.cancellable(id: Any, inFlight: Boolean = false) =
    apply { effect.cancellable(id, inFlight) }

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
        val cancellationId = effect.cancellationId

        var job: Job? = null
        job = GlobalScope.launch(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
            try {
                if (effect.cancelInFlight) {
                    cancelJobs(cancellationId, job)
                }
                val actions = effect.sink()
                if (actions.isNotEmpty()) {
                    withContext(mainDispatcher) {
                        actions.forEach { send(it) }
                    }
                }
            } catch (ex: Exception) {
                println("Executing effects failed: ${ex.message}")
            } finally {
                if (cancellationId != null) {
                    removeJob(cancellationId, job)
                }
            }
        }
        if (cancellationId != null) {
            cancellationJobs.getOrPut(cancellationId) { mutableListOf() }.add(job)
        }
        job.start()

        mutableState.value = newState
    }

    suspend fun observe(observer: (State) -> Unit) =
        mutableState.collect { state -> observer(state) }
}

class AppEnvironment

sealed class AppAction {
    class Counter(val action: CounterAction) : AppAction()
}

sealed class CounterAction {
    object Increment : CounterAction()
    object Noop : CounterAction()
    object Cancel : CounterAction()

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
            CounterAction.Increment -> {
                CounterState.counter.set(state, state.counter + 1)
                    .withEffect<CounterState, CounterAction> {
                        delay(2000L)
                        emit(CounterAction.Noop)
                    }
                    .cancellable("1", inFlight = true)
            }
            CounterAction.Cancel -> state.cancel("1")
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
            mainDispatcher = Dispatchers.Unconfined
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
        store.send(AppAction.Counter(CounterAction.Increment))
        store.send(AppAction.Counter(CounterAction.Increment))
        delay(1000L)
        scopedStore.send(CounterAction.Cancel)

        println("✅")
    }
}
