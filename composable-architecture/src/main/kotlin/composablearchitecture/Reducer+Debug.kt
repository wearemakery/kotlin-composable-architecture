package composablearchitecture

@Suppress("unused")
fun <State, Action, Environment> Reducer<State, Action, Environment>.debug(): Reducer<State, Action, Environment> =
    Reducer { state, action, environment ->
        val result = run(state, action, environment)
        println("state=${result.state}, action=$action")
        result
    }
