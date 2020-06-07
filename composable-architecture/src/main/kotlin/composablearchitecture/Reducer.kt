package composablearchitecture

import arrow.optics.Lens
import arrow.optics.Prism

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

    // TODO: Implement "IdentifiedArray"
    fun <GlobalState, GlobalAction, GlobalEnvironment, ID> forEach(
        toLocalState: Lens<GlobalState, List<State>>,
        toLocalAction: Prism<GlobalAction, Pair<ID, Action>>,
        toLocalEnvironment: (GlobalEnvironment) -> Environment,
        findById: Lens<State, ID>
    ): Reducer<GlobalState, GlobalAction, GlobalEnvironment> =
        Reducer { globalState, globalAction, globalEnvironment ->
            toLocalAction.getOption(globalAction).fold(
                { ReducerResult(globalState, Effect.none()) },
                { (id, localAction) ->
                    val localState = toLocalState.get(globalState)
                    val index = localState.indexOfFirst { findById.get(it) == id }
                    if (index < 0) {
                        ReducerResult(globalState, Effect.none())
                    } else {
                        val (state, effect) = reducer(
                            localState[index],
                            localAction,
                            toLocalEnvironment(globalEnvironment)
                        )
                        ReducerResult(
                            toLocalState.set(globalState, localState.update(index, state)),
                            effect.map { toLocalAction.reverseGet(id to localAction) }
                        )
                    }
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
