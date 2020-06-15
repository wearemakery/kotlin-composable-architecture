package composablearchitecture.example.tictactoe

import android.app.Application
import composablearchitecture.Store

@Suppress("unused")
class TicTacToeApp : Application() {

    companion object {
        val gameStore = Store(
            initialState = GameState(),
            reducer = gameReducer,
            environment = GameEnvironment
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
