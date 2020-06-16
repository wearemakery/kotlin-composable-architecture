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
                val newMatrix = state.board.matrix.copy()
                newMatrix[0][0] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(2, 1)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[2][1] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(1, 0)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[1][0] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(1, 1)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[1][1] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(2, 0)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[2][0] = Player.X
                state.copy(board = state.board.copy(matrix = newMatrix))
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
                val newMatrix = state.board.matrix.copy()
                newMatrix[0][0] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(1, 1)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[1][1] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(1, 0)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[1][0] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(2, 0)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[2][0] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(0, 2)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[0][2] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(0, 1)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[0][1] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(2, 1)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[2][1] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
            send(GameAction.CellTapped(1, 2)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[1][2] = Player.O
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.X
                )
            }
            send(GameAction.CellTapped(2, 2)) { state ->
                val newMatrix = state.board.matrix.copy()
                newMatrix[2][2] = Player.X
                state.copy(
                    board = state.board.copy(matrix = newMatrix),
                    currentPlayer = Player.O
                )
            }
        }
    }
}
