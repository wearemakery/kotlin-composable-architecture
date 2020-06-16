package composablearchitecture.example.casestudies

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import composablearchitecture.Reducer
import composablearchitecture.Result
import composablearchitecture.Store
import composablearchitecture.example.casestudies.databinding.MainActivityBinding
import composablearchitecture.withNoEffect
import kotlinx.coroutines.flow.collect

data class EventWrapper(
    val x: Float = 0F,
    val y: Float = 0F,
    val action: Int = 0
)

data class State(
    val dx: Float = 0F,
    val dy: Float = 0F,
    val event: EventWrapper = EventWrapper(),
    val animationX: SpringAnimation? = null,
    val animationY: SpringAnimation? = null
)

sealed class Action {
    data class Touch(val view: View, val event: MotionEvent) : Action()
    data class Layout(val view: View) : Action()
}

private val reducer = Reducer<State, Action, Unit> { state, action, _ ->
    when (action) {
        is Action.Layout -> action.handle(state)
        is Action.Touch -> action.handle(state)
    }
}

private fun Action.Layout.handle(state: State): Result<State, Action> {
    val animationX = createAnimation(view, view.x, SpringAnimation.X)
    val animationY = createAnimation(view, view.y, SpringAnimation.Y)
    return state
        .copy(animationX = animationX, animationY = animationY)
        .withNoEffect()
}

private fun Action.Touch.handle(state: State): Result<State, Action> =
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            state.animationX?.cancel()
            state.animationY?.cancel()
            state
                .copy(
                    dx = view.x - event.rawX,
                    dy = view.y - event.rawY,
                    event = event.wrap()
                )
                .withNoEffect()
        }
        MotionEvent.ACTION_MOVE ->
            state
                .copy(event = event.wrap())
                .withNoEffect()
        MotionEvent.ACTION_UP -> {
            state.animationX?.start()
            state.animationY?.start()
            state.withNoEffect()
        }
        else -> state.withNoEffect()
    }

private val store = Store(State(), reducer, Unit)

@SuppressLint("ClickableViewAccessibility")
class AnimationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.main_activity)
        val imageView = binding.animationImageView

        imageView.setOnTouchListener { view, event ->
            store.send(Action.Touch(view, event))
            true
        }

        imageView.doOnLayout { view ->
            store.send(Action.Layout(view))
        }

        lifecycleScope.launchWhenCreated {
            store.states.collect { state ->
                when (state.event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        imageView.animate()
                            .x(state.event.x + state.dx)
                            .y(state.event.y + state.dy)
                            .setDuration(0)
                            .start()
                    }
                }
            }
        }
    }
}

private fun MotionEvent.wrap(): EventWrapper = EventWrapper(rawX, rawY, actionMasked)

private fun createAnimation(view: View, position: Float, property: DynamicAnimation.ViewProperty): SpringAnimation =
    SpringAnimation(view, property).apply {
        val force = SpringForce(position).apply {
            stiffness = SpringForce.STIFFNESS_MEDIUM
            dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        }
        spring = force
    }
