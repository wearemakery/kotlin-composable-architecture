package composablearchitecture

import arrow.core.left
import arrow.core.right
import arrow.optics.PPrism
import arrow.optics.Prism
import arrow.optics.optics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@optics
data class NestedState(var text: String = "") {
    companion object
}

@optics
data class CounterState(val counter: Int = 0, val nestedState: NestedState = NestedState()) {
    companion object
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

object CounterEnvironment

val counterReducer =
    Reducer<CounterState, CounterAction, CounterEnvironment> { state, action, _ ->
        when (action) {
            CounterAction.Increment -> {
                CounterState.counter.set(state, state.counter + 1)
                    .withEffect<CounterState, CounterAction> {
                        delay(2000L)
                        emit(CounterAction.Noop)
                    }
                    .cancellable("1", cancelInFlight = true)
            }
            CounterAction.Cancel -> state.cancel("1")
            else -> state.withNoEffect()
        }
    }

@optics
data class AppState(val counterState: CounterState = CounterState()) {
    companion object
}

sealed class AppAction {
    class Counter(val action: CounterAction) : AppAction()
}

class AppEnvironment

val debugReducer = Reducer<AppState, AppAction, AppEnvironment> { state, action, _ ->
    println("[${Thread.currentThread().name}] [debug reducer] action=${action::class.simpleName} state=$state")
    state.withNoEffect()
}

val appReducer: Reducer<AppState, AppAction, AppEnvironment> =
    Reducer.combine(
        counterReducer.pullback(
            toLocalState = AppState.counterState,
            toLocalAction = CounterAction.prism,
            toLocalEnvironment = { CounterEnvironment }
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
        // scopedStore.send(CounterAction.Cancel)

        println("✅")
    }
}
