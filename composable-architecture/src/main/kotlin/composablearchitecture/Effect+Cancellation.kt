package composablearchitecture

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutex = Mutex()
private val cancellationJobs: MutableMap<Any, MutableSet<Job>> = mutableMapOf()

fun <Output> Effect<Output>.cancellable(id: Any, cancelInFlight: Boolean = false): Effect<Output> =
    Effect(flow {
        if (cancelInFlight) {
            mutex.withLock {
                cancellationJobs[id]?.forEach { it.cancel() }
                cancellationJobs.remove(id)
            }
        }
        val outputs = coroutineScope {
            val deferred = async(
                Dispatchers.Unconfined,
                start = CoroutineStart.LAZY
            ) { this@cancellable.flow.toList() }
            mutex.withLock {
                @Suppress("RemoveExplicitTypeArguments")
                cancellationJobs.getOrPut(id) { mutableSetOf<Job>() }.add(deferred)
            }
            try {
                deferred.start()
                deferred.await()
            } finally {
                mutex.withLock {
                    val jobs = cancellationJobs[id]
                    jobs?.remove(deferred)
                    if (jobs.isNullOrEmpty()) {
                        cancellationJobs.remove(id)
                    }
                }
            }
        }
        outputs.forEach { emit(it) }
    })

fun <Output> Effect.Companion.cancel(id: Any): Effect<Output> = Effect(flow {
    mutex.withLock {
        cancellationJobs[id]?.forEach { it.cancel() }
        cancellationJobs.remove(id)
    }
})

fun <State, Output> State.cancel(id: Any): Result<State, Output> =
    Result(this, Effect.cancel(id))

fun <State, Output> Result<State, Output>.cancellable(
    id: Any,
    cancelInFlight: Boolean = false
): Result<State, Output> =
    Result(state, effect.cancellable(id, cancelInFlight))
