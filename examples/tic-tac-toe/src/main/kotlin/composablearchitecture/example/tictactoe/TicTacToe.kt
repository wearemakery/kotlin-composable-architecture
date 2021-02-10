package composablearchitecture.example.tictactoe

import arrow.optics.optics
import composablearchitecture.Reducer
import composablearchitecture.debug
import composablearchitecture.withNoEffect

enum class Player(val label: String) {
    X("X"), O("O");

    fun toggle(): Player =
        when (this) {
            X -> O
            O -> X
        }
}

sealed class GameAction: Comparable<GameAction> {
    data class CellTapped(val row: Int, val column: Int) : GameAction()
    object PlayAgainButtonTapped : GameAction()

    override fun compareTo(other: GameAction): Int = this.compareTo(other)
}

object GameEnvironment

@optics
data class GameState(
    val board: Board = Board(),
    val currentPlayer: Player = Player.X,
    val oPlayerName: String = "",
    val xPlayerName: String = ""
) {
    companion object
}

val gameReducer = Reducer<GameState, GameAction, GameEnvironment> { state, action, _ ->
    when (action) {
        is GameAction.CellTapped -> {
            if (state.board.matrix[action.row][action.column] != null || state.board.hasWinner()) {
                state.withNoEffect()
            } else {
                val newMatrix = state.board.matrix.copy()
                newMatrix[action.row][action.column] = state.currentPlayer

                val newBoard = state.board.copy(matrix = newMatrix)

                val newPlayer = if (!newBoard.hasWinner()) {
                    state.currentPlayer.toggle()
                } else {
                    state.currentPlayer
                }

                state
                    .copy(
                        board = newBoard,
                        currentPlayer = newPlayer
                    )
                    .withNoEffect()
            }
        }
        is GameAction.PlayAgainButtonTapped -> {
            GameState().withNoEffect()
        }
    }
}
    .debug()

data class Board(
    val matrix: Array<Array<Player?>> = arrayOf(arrayOfNulls(3), arrayOfNulls(3), arrayOfNulls(3))
) {

    private fun hasWin(player: Player): Boolean {
        val winConditions = arrayOf(
            arrayOf(0, 1, 2), arrayOf(3, 4, 5), arrayOf(6, 7, 8),
            arrayOf(0, 3, 6), arrayOf(1, 4, 7), arrayOf(2, 5, 8),
            arrayOf(0, 4, 8), arrayOf(6, 4, 2)
        )

        winConditions.forEach { condition ->
            val matchCount = condition.map { matrix[it % 3][it / 3] }
                .filter { it == player }
                .size

            if (matchCount == 3) {
                return true
            }
        }
        return false
    }

    fun hasWinner() = hasWin(Player.X) || hasWin(Player.O)

    fun isFilled() = matrix.all { row -> row.all { it != null } }

    override fun toString(): String {
        return """
            |
            |[${matrix[0][0] ?: " "}][${matrix[0][1] ?: " "}][${matrix[0][2] ?: " "}]
            |[${matrix[1][0] ?: " "}][${matrix[1][1] ?: " "}][${matrix[1][2] ?: " "}]
            |[${matrix[2][0] ?: " "}][${matrix[2][1] ?: " "}][${matrix[2][2] ?: " "}]
            |
    """.trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Board

        if (!matrix.contentDeepEquals(other.matrix)) return false

        return true
    }

    override fun hashCode(): Int {
        return matrix.contentDeepHashCode()
    }
}
