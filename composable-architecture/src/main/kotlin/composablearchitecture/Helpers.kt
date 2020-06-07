package composablearchitecture

fun <E> Iterable<E>.update(index: Int, elem: E) =
    mapIndexed { i, existing -> if (i == index) elem else existing }
