package composablearchitecture.example.tictactoe

import composablearchitecture.test.TestStore
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test

class TicTacToeTest {

    @Test
    fun `Player X wins a game`() {
        val testDispatcher = TestCoroutineDispatcher()

        val store = TestStore(
            GameState(),
            gameReducer,
            GameEnvironment,
            testDispatcher
        )

        store.assert {
            send(GameAction.CellTapped(0, 0)) { state ->
                state.update(0, 0, Player.X, Player.O)
            }
            send(GameAction.CellTapped(2, 1)) { state ->
                state.update(2, 1, Player.O, Player.X)
            }
            send(GameAction.CellTapped(1, 0)) { state ->
                state.update(1, 0, Player.X, Player.O)
            }
            send(GameAction.CellTapped(1, 1)) { state ->
                state.update(1, 1, Player.O, Player.X)
            }
            send(GameAction.CellTapped(2, 0)) { state ->
                state.update(2, 0, Player.X, Player.X)
            }
        }
    }

    @Test
    fun `Player X and Player O play a tie game`() {
        val testDispatcher = TestCoroutineDispatcher()

        val store = TestStore(
            GameState(),
            gameReducer,
            GameEnvironment,
            testDispatcher
        )

        store.assert {
            send(GameAction.CellTapped(0, 0)) { state ->
                state.update(0, 0, Player.X, Player.O)
            }
            send(GameAction.CellTapped(1, 1)) { state ->
                state.update(1, 1, Player.O, Player.X)
            }
            send(GameAction.CellTapped(1, 0)) { state ->
                state.update(1, 0, Player.X, Player.O)
            }
            send(GameAction.CellTapped(2, 0)) { state ->
                state.update(2, 0, Player.O, Player.X)
            }
            send(GameAction.CellTapped(0, 2)) { state ->
                state.update(0, 2, Player.X, Player.O)
            }
            send(GameAction.CellTapped(0, 1)) { state ->
                state.update(0, 1, Player.O, Player.X)
            }
            send(GameAction.CellTapped(2, 1)) { state ->
                state.update(2, 1, Player.X, Player.O)
            }
            send(GameAction.CellTapped(1, 2)) { state ->
                state.update(1, 2, Player.O, Player.X)
            }
            send(GameAction.CellTapped(2, 2)) { state ->
                state.update(2, 2, Player.X, Player.O)
            }
        }
    }
}

private fun GameState.update(row: Int, column: Int, current: Player, next: Player): GameState {
    val newMatrix = board.matrix.copy()
    newMatrix[row][column] = current
    return copy(
        board = board.copy(matrix = newMatrix),
        currentPlayer = next
    )
}
