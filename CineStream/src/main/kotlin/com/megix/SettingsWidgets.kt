package com.megix

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.megix.SettingsTheme.dp

/**
 * Small, reusable UI building blocks shared across all Settings card files.
 */
internal object SettingsWidgets {

    // ── Pill button ──────────────────────────────────────────

    fun pillBtn(
        context: Context, label: String,
        textColor: Int, bgColor: Int, borderColor: Int,
        onClick: () -> Unit
    ) = TextView(context).apply {
        text = label; textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(textColor)
        setPadding(12.dp(context), 6.dp(context), 12.dp(context), 6.dp(context))
        background = GradientDrawable().apply {
            cornerRadius = 99f; setColor(bgColor); setStroke(1, borderColor)
        }
        isClickable = true; isFocusable = true; isFocusableInTouchMode = false
        setOnClickListener {
            animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
            onClick()
        }
    }

    // ── Divider ──────────────────────────────────────────────

    fun divider(context: Context) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            .also { it.setMargins(20.dp(context), 0, 20.dp(context), 0) }
        setBackgroundColor(SettingsTheme.DIVIDER_COLOR)
    }

    // ── Horizontal spacer ────────────────────────────────────

    fun hSpacer(context: Context, widthDp: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(widthDp.dp(context), 1)
    }

    // ── Expand / collapse animation ──────────────────────────

    fun animateExpand(view: View, expand: Boolean, onCollapsed: (() -> Unit)? = null) {
        if (expand) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(220).start()
        } else {
            view.animate().alpha(0f).setDuration(160).withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
                onCollapsed?.invoke()
            }.start()
        }
    }

    // ── Entrance animation for cards ─────────────────────────

    fun fadeInSlide(view: View) {
        view.alpha       = 0f
        view.translationY = 20f
        view.animate()
            .alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
    }

    // ── Accent bar (left colour strip used in card headers) ──

    fun accentBar(context: Context, top: Int, bottom: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
            .also { it.marginEnd = 12.dp(context) }
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom))
            .apply { cornerRadius = 99f }
    }

    // ── Danger pill shortcut ─────────────────────────────────

    fun dangerBtn(context: Context, label: String, onClick: () -> Unit) = pillBtn(
        context, label,
        SettingsTheme.DANGER_COLOR,
        Color.parseColor("#1A0A0D"),
        Color.parseColor("#3A1520"),
        onClick
    )
}
