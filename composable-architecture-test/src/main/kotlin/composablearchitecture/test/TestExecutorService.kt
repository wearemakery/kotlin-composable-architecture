package composablearchitecture.test

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class TestExecutorService : AbstractExecutorService() {

    @Volatile
    private var terminated: Boolean = false

    override fun isTerminated(): Boolean = terminated

    override fun execute(command: Runnable) {
        command.run()
    }

    override fun shutdown() {
        terminated = true
    }

    override fun shutdownNow(): MutableList<Runnable> = mutableListOf()

    override fun isShutdown(): Boolean = terminated

    override fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean {
        shutdown()
        return terminated
    }
}
