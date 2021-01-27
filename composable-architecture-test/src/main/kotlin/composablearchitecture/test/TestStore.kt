package composablearchitecture.test

import arrow.optics.Lens
import arrow.optics.Prism
import composablearchitecture.Reducer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher

internal sealed class Step<Action, State, Environment> {
    class Send<Action, State, Environment>(
        val action: Action,
        val block: (State) -> State
    ) : Step<Action, State, Environment>()

    class Receive<Action, State, Environment>(
        val action: Action,
        val block: (State) -> State
    ) : Step<Action, State, Environment>()

    class Environment<Action, State, Environment>(
        val block: (Environment) -> Unit
    ) : Step<Action, State, Environment>()

    class Do<Action, State, Environment>(
        val block: () -> Unit
    ) : Step<Action, State, Environment>()
}

class AssertionBuilder<Action, State, Environment>(private val currentState: () -> State) {

    internal val steps: MutableList<Step<Action, State, Environment>> = mutableListOf()

    fun send(action: Action, block: (State) -> State) = steps.add(Step.Send(action, block))

    fun send(action: Action) = steps.add(Step.Send(action, { currentState() }))

    fun receive(action: Action, block: (State) -> State) = steps.add(Step.Receive(action, block))

    fun receive(action: Action) = steps.add(Step.Receive(action, { currentState() }))

    fun environment(block: (Environment) -> Unit) = steps.add(Step.Environment(block))

    fun doBlock(block: () -> Unit) = steps.add(Step.Do(block))
}

class TestStore<State, LocalState, Action : Comparable<Action>, LocalAction, Environment>
private constructor(
    private var state: State,
    private val reducer: Reducer<State, Action, Environment>,
    private val environment: Environment,
    private val toLocalState: Lens<State, LocalState>,
    private val fromLocalAction: Prism<Action, LocalAction>,
    private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) {

    companion object {
        operator fun <State, Action : Comparable<Action>, Environment> invoke(
            state: State,
            reducer: Reducer<State, Action, Environment>,
            environment: Environment,
            testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
        ) =
            TestStore(
                state,
                reducer,
                environment,
                Lens.id(),
                Prism.id(),
                testDispatcher
            )
    }

    fun <S, A> scope(
        toLocalState: Lens<State, S>,
        fromLocalAction: Prism<Action, A>
    ): TestStore<State, S, Action, A, Environment> =
        TestStore(
            state,
            reducer,
            environment,
            toLocalState,
            fromLocalAction,
            testDispatcher
        )

    fun assert(block: AssertionBuilder<LocalAction, LocalState, Environment>.() -> Unit) {
        val assertion = AssertionBuilder<LocalAction, LocalState, Environment> {
            toLocalState.get(state)
        }
        assertion.block()

        val receivedActions: MutableList<Action> = mutableListOf()

        fun runReducer(action: Action) {
            val (newState, effect) = reducer.run(state, action, environment)
            state = newState

            GlobalScope.launch(testDispatcher) {
                try {
                    val actions = effect.sink()
                    receivedActions.addAll(actions)
                } catch (ex: CancellationException) {
                    // ignore
                }
            }
        }

        assertion.steps.forEach { step ->
            var expectedState = toLocalState.get(state)

            when (step) {
                is Step.Send<LocalAction, LocalState, Environment> -> {
                    require(receivedActions.isEmpty()) { "Must handle all actions. Unhandled actions:\n" + receivedActions.joinToString(separator = "\n") }
                    runReducer(fromLocalAction.reverseGet(step.action))
                    expectedState = step.block(expectedState)
                }
                is Step.Receive<LocalAction, LocalState, Environment> -> {
                    require(receivedActions.isNotEmpty()) { "Expected to receive" + receivedActions.joinToString(separator = "\n") + "but received none" }
                    val receivedAction = receivedActions.removeFirst()
                    require(step.action == receivedAction) { "Actual and expected actions do not match" }
                    runReducer(fromLocalAction.reverseGet(step.action))
                    expectedState = step.block(expectedState)
                }
                is Step.Environment<LocalAction, LocalState, Environment> -> {
                    require(receivedActions.isEmpty()) { "Must handle all received actions before performing this work." + receivedActions.joinToString(separator = "\n") + "are not handled" }
                    step.block(environment)
                }
                is Step.Do -> step.block()
            }

            val actualState = toLocalState.get(state)
            require(actualState == expectedState) {
                println(actualState)
                println("---vs---")
                println(expectedState)
                "Actual and expected states do not match"
            }
        }

        require(receivedActions.isEmpty()) { "Must handle all actions. Unhandled actions:\n" + receivedActions.joinToString(separator = "\n") }
    }
}
