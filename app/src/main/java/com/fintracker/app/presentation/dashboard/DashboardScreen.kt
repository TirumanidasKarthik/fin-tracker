package com.fintracker.app.presentation.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Builds the dashboard independently from the Activity that hosts it. */
class DashboardScreen(private val context: Context) {
    fun createView(): View {
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
            setBackgroundColor(Color.rgb(245, 246, 241))
        }

        page.addView(text("FIN TRACKER", 12f, Color.rgb(37, 104, 83), Typeface.BOLD))
        page.addView(
            text("Your money,\nwith a plan.", 34f, Color.rgb(25, 39, 33), Typeface.BOLD),
            margins(top = 28),
        )
        page.addView(
            text(
                "A simple way to divide your income and keep an eye on everyday spending.",
                16f,
                Color.rgb(91, 105, 98),
                Typeface.NORMAL,
            ),
            margins(top = 12),
        )

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = roundedCard()
        }
        card.addView(text("YOUR MONTHLY PLAN", 11f, Color.rgb(37, 104, 83), Typeface.BOLD))
        card.addView(
            text("No buckets yet", 22f, Color.rgb(25, 39, 33), Typeface.BOLD),
            margins(top = 18),
        )
        card.addView(
            text(
                "Start with your monthly income. Then choose the buckets that matter to you.",
                15f,
                Color.rgb(91, 105, 98),
                Typeface.NORMAL,
            ),
            margins(top = 8),
        )
        page.addView(card, margins(top = 28))

        page.addView(
            text(
                "Your plan stays on this device. No account or internet connection is needed.",
                13f,
                Color.rgb(91, 105, 98),
                Typeface.NORMAL,
            ),
            margins(top = 20),
        )

        return ScrollView(context).apply {
            isFillViewport = true
            addView(page)
        }
    }

    private fun text(value: String, sizeSp: Float, color: Int, style: Int) = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        typeface = Typeface.create("sans-serif", style)
        includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun roundedCard() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(24).toFloat()
    }

    private fun margins(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(top)
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}

