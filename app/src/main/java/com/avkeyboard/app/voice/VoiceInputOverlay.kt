package com.avkeyboard.app.voice

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class VoiceInputOverlay(private val context: Context) {

    private var overlayView: FrameLayout? = null
    private var pulseAnimator: ObjectAnimator? = null

    fun show(parent: ViewGroup, state: VoiceInputController.State) {
        hide()
        if (state == VoiceInputController.State.IDLE) return

        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            isClickable = true
            isFocusable = true
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        when (state) {
            VoiceInputController.State.RECORDING -> {
                val dot = View(context).apply {
                    val size = dp(20)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(12)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.RED)
                    }
                }
                container.addView(dot)

                pulseAnimator = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.3f).apply {
                    duration = 600
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }

                val text = TextView(context).apply {
                    this.text = "🎤 Запись..."
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    gravity = Gravity.CENTER
                }
                container.addView(text)

                val hint = TextView(context).apply {
                    this.text = "Нажмите 🎤 для остановки"
                    setTextColor(Color.argb(180, 255, 255, 255))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = dp(8)
                    layoutParams = lp
                }
                container.addView(hint)
            }

            VoiceInputController.State.PROCESSING -> {
                val progress = ProgressBar(context).apply {
                    isIndeterminate = true
                    val lp = LinearLayout.LayoutParams(dp(48), dp(48))
                    lp.gravity = Gravity.CENTER_HORIZONTAL
                    lp.bottomMargin = dp(12)
                    layoutParams = lp
                }
                container.addView(progress)

                val text = TextView(context).apply {
                    this.text = "⏳ Обработка..."
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    gravity = Gravity.CENTER
                }
                container.addView(text)
            }

            else -> {}
        }

        overlay.addView(container)
        parent.addView(overlay)
        overlayView = overlay
    }

    fun hide() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        overlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        overlayView = null
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
