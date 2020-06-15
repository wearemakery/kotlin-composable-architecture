package composablearchitecture.example.tictactoe

inline fun <reified T> Array<Array<T>>.copy() = map { it.clone() }.toTypedArray()
