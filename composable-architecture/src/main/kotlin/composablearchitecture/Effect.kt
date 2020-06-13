package composablearchitecture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class Effect<Output>(internal var flow: Flow<Output>) {

    companion object {
        operator fun <Output> invoke(
            block: suspend FlowCollector<Output>.() -> Unit
        ): Effect<Output> = Effect(flow(block))

        fun <Output> none() = Effect<Output>(emptyFlow())
    }

    fun <T> map(transform: (Output) -> T): Effect<T> = Effect(flow.map { transform(it) })

    fun concatenate(vararg effects: Effect<Output>) {
        flow = flowOf(flow, *effects.map { it.flow }.toTypedArray()).flattenConcat()
    }

    fun merge(vararg effects: Effect<Output>) {
        flow = flowOf(flow, *effects.map { it.flow }.toTypedArray()).flattenMerge()
    }

    suspend fun sink(): List<Output> {
        val outputs = mutableListOf<Output>()
        flow.toList(outputs)
        return outputs
    }
}

fun <State, Output> State.withNoEffect(): Result<State, Output> =
    Result(this, Effect.none())

fun <State, Output> State.withEffect(
    block: suspend FlowCollector<Output>.() -> Unit
): Result<State, Output> =
    Result(this, Effect(flow(block)))
