package composablearchitecture.example.tictactoe

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import composablearchitecture.example.tictactoe.databinding.TictactoeActivityBinding
import kotlinx.coroutines.flow.collect

class TicTacToeActivity : AppCompatActivity() {

    private val store = TicTacToeApp.gameStore

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil
            .setContentView<TictactoeActivityBinding>(this, R.layout.tictactoe_activity)

        binding.playAgainButton.setOnClickListener {
            store.send(GameAction.PlayAgainButtonTapped)
        }

        val cellViews = arrayOf(
            arrayOf(binding.cell00, binding.cell01, binding.cell02),
            arrayOf(binding.cell10, binding.cell11, binding.cell12),
            arrayOf(binding.cell20, binding.cell21, binding.cell22)
        )

        cellViews.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cellView ->
                cellView.setOnClickListener {
                    store.send(GameAction.CellTapped(rowIndex, columnIndex))
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            store.states.collect { state ->
                state.board.matrix.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { columnIndex, cell ->
                        cellViews[rowIndex][columnIndex].text = cell?.label
                    }
                }

                when {
                    state.board.hasWinner() -> {
                        binding.statusText.text = "The winner is: ${state.currentPlayer}"
                        binding.playAgainButton.isVisible = true
                    }
                    state.board.isFilled() -> {
                        binding.statusText.text = "Tied game!"
                        binding.playAgainButton.isVisible = true
                    }
                    else -> {
                        binding.statusText.text =
                            "${state.currentPlayer.name} place your ${state.currentPlayer.label}"
                        binding.playAgainButton.isVisible = false
                    }
                }
            }
        }
    }
}
