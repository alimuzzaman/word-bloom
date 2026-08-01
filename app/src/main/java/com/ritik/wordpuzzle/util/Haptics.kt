package com.ritik.wordpuzzle.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Thin haptics wrapper.
 *
 * Goes through [View.performHapticFeedback] rather than the `Vibrator` service on
 * purpose: it needs no `VIBRATE` permission, and it respects the user's system-wide
 * haptics setting automatically, so a player who has turned touch feedback off is
 * not buzzed anyway.
 *
 * The richer constants only exist from API 30, so each call degrades to the closest
 * older equivalent rather than doing nothing on older devices.
 */
class Haptics(private val view: View) {

    /** Grid word accepted — the most substantial confirmation in the game. */
    fun success() {
        perform(
            modern = HapticFeedbackConstants.CONFIRM,
            legacy = HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }

    /** Bonus word — present but lighter than a grid word. */
    fun light() {
        perform(
            modern = HapticFeedbackConstants.CLOCK_TICK,
            legacy = HapticFeedbackConstants.KEYBOARD_TAP,
        )
    }

    /** Word rejected. */
    fun reject() {
        perform(
            modern = HapticFeedbackConstants.REJECT,
            legacy = HapticFeedbackConstants.LONG_PRESS,
        )
    }

    /** Level complete. */
    fun celebrate() {
        perform(
            modern = HapticFeedbackConstants.CONFIRM,
            legacy = HapticFeedbackConstants.LONG_PRESS,
        )
    }

    private fun perform(modern: Int, legacy: Int) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) modern else legacy
        view.performHapticFeedback(constant)
    }
}
