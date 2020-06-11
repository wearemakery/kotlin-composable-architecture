package composablearchitecture.test

import composablearchitecture.Reducer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher

internal sealed class Step<Action, State, Environment> {
    class Send<Action, State, Environment>(
        val action: Action,
        val block: () -> State
    ) : Step<Action, State, Environment>()

    class Receive<Action, State, Environment>(
        val action: Action,
        val block: () -> State
    ) : Step<Action, State, Environment>()

    class Environment<Action, State, Environment>(
        val block: (Environment) -> Unit
    ) : Step<Action, State, Environment>()

    class Do<Action, State, Environment>(
        val block: () -> Unit
    ) : Step<Action, State, Environment>()
}

class AssertionBuilder<Action, State, Environment> {
    internal val steps: MutableList<Step<Action, State, Environment>> = mutableListOf()

    fun send(action: Action, block: () -> State) = steps.add(Step.Send(action, block))

    fun receive(action: Action, block: () -> State) = steps.add(Step.Receive(action, block))

    fun environment(block: (Environment) -> Unit) = steps.add(Step.Environment(block))

    fun doBlock(block: () -> Unit) = steps.add(Step.Do(block))
}

class TestStore<State, Action : Comparable<Action>, Environment>(
    private var state: State,
    private val reducer: Reducer<State, Action, Environment>,
    private val environment: Environment,
    private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) {

    fun assert(block: AssertionBuilder<Action, State, Environment>.() -> Unit) {
        val assertion = AssertionBuilder<Action, State, Environment>()
        assertion.block()

        val receivedActions: MutableList<Action> = mutableListOf()

        fun runReducer(action: Action) {
            val (newState, effect) = reducer.run(state, action, environment)
            state = newState

            GlobalScope.launch(testDispatcher) {
                try {
                    val actions = effect.sink()
                    receivedActions.addAll(actions)
                } catch (ex: Exception) {
                    println("Executing effects failed: ${ex.message}")
                }
            }
        }

        assertion.steps.forEach { step ->
            var expectedState = state

            when (step) {
                is Step.Send<Action, State, Environment> -> {
                    require(receivedActions.isEmpty()) { "Must handle all actions" }
                    runReducer(step.action)
                    expectedState = step.block()
                }
                is Step.Receive<Action, State, Environment> -> {
                    require(receivedActions.isNotEmpty()) { "Expected to receive an action, but received none" }
                    val receivedAction = receivedActions.removeFirst()
                    require(step.action == receivedAction) { "Actual and expected actions do not match" }
                    runReducer(step.action)
                    expectedState = step.block()
                }
                is Step.Environment<Action, State, Environment> -> {
                    require(receivedActions.isEmpty()) { "Must handle all received actions before performing this work" }
                    step.block(environment)
                }
                is Step.Do -> step.block()
            }

            require(state == expectedState) {
                println(state)
                println("---vs---")
                println(expectedState)
                "Actual and expected states do not match"
            }
        }

        require(receivedActions.isEmpty()) { "Must handle all actions" }
    }
}
