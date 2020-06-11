package composablearchitecture

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.optics.Lens
import arrow.optics.Optional
import arrow.optics.dsl.index
import arrow.optics.typeclasses.Index

private fun <A> listIndex(): Index<List<A>, Int, A> = object : Index<List<A>, Int, A> {
    override fun index(i: Int): Optional<List<A>, A> = object : Optional<List<A>, A> {
        override fun getOrModify(source: List<A>): Either<List<A>, A> =
            source.getOrNull(i)?.right() ?: source.left()

        override fun set(source: List<A>, focus: A): List<A> =
            source.update(i, focus)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T, S, A> Lens<T, S>.listIndex(i: Int): Optional<T, A> where S : List<A> {
    val index: Index<S, Int, A> = listIndex<A>() as Index<S, Int, A>
    return index(index, i)
}
