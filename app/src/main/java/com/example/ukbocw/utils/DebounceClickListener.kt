package com.example.ukbocw.utils

import android.os.SystemClock
import android.view.View

class DebounceClickListener(private val delayMills: Long = 500L, val action: (View?) -> Unit) :
    View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(view: View?) {
        val now = SystemClock.elapsedRealtime()

        if (now - lastClickTime < delayMills) {
            return
        }

        lastClickTime = now
        action(view)
    }
}