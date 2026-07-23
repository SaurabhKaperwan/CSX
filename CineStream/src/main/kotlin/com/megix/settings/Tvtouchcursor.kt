package com.megix.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import com.megix.settings.SettingsTheme.dp

/**
 * A movable, D-pad controlled on-screen touch cursor.
 *
 * Android TV remotes only produce DPAD key events, so touch-only elements
 * (most notably Cloudflare CAPTCHA checkboxes) inside a WebView can never
 * be interacted with. This overlays a small circular "ball" on top of the
 * WebView that can be nudged around with the D-pad and "tapped" with
 * OK/DPAD_CENTER, which synthesizes a real ACTION_DOWN/ACTION_UP touch
 * event at the cursor's position and dispatches it straight to the
 * WebView — effectively emulating a finger tap.
 */
internal class TvTouchCursor(
    private val context: Context,
    private val overlay: FrameLayout,   // must share the same coordinate space as [webView]
    private val webView: WebView
) {
    var isActive = false
        private set

    private val cursorSize = 28.dp(context)
    private val stepSlow = 10.dp(context)
    private val stepFast = 28.dp(context)

    private var x = 0f
    private var y = 0f

    private val cursorView: View = View(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#8022C55E"))
            setStroke(2.dp(context), Color.parseColor("#22C55E"))
        }
        layoutParams = FrameLayout.LayoutParams(cursorSize, cursorSize)
        visibility = View.GONE
    }

    init {
        overlay.addView(cursorView)
    }

    fun toggle() {
        if (isActive) hide() else show()
    }

    fun show() {
        isActive = true
        ensureInitialPosition()
        cursorView.visibility = View.VISIBLE
        cursorView.bringToFront()
        overlay.invalidate()
        applyPosition()
    }

    fun hide() {
        isActive = false
        cursorView.visibility = View.GONE
    }

    private fun ensureInitialPosition() {
        if (x == 0f && y == 0f) {
            val w = overlay.width
            val h = overlay.height
            x = if (w > 0) w / 2f else 300f
            y = if (h > 0) h / 2f else 300f
        }
    }

    private fun applyPosition() {
        val maxX = (overlay.width - cursorSize).coerceAtLeast(0).toFloat()
        val maxY = (overlay.height - cursorSize).coerceAtLeast(0).toFloat()
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
        cursorView.translationX = x
        cursorView.translationY = y
    }

    /** Returns true if the event was handled (i.e. should be consumed and not propagate). */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isActive) return false
        if (event.keyCode !in HANDLED_KEYS) return false

        // Consume both DOWN and UP for handled keys so the WebView/dialog never
        // sees them and tries to do its own focus navigation / dismissal.
        if (event.action != KeyEvent.ACTION_DOWN) return true

        val step = if (event.repeatCount > 2) stepFast else stepSlow
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { y -= step; applyPosition() }
            KeyEvent.KEYCODE_DPAD_DOWN -> { y += step; applyPosition() }
            KeyEvent.KEYCODE_DPAD_LEFT -> { x -= step; applyPosition() }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { x += step; applyPosition() }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> performTap()
        }
        return true
    }

    private fun performTap() {
        val tapX = x + cursorSize / 2f
        val tapY = y + cursorSize / 2f
        val downTime = SystemClock.uptimeMillis()

        cursorView.animate().scaleX(0.65f).scaleY(0.65f).setDuration(60)
            .withEndAction { cursorView.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
            .start()

        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, tapX, tapY, 0)
        val up = MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, tapX, tapY, 0)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    fun destroy() {
        overlay.removeView(cursorView)
    }

    companion object {
        private val HANDLED_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER
        )
    }
}
