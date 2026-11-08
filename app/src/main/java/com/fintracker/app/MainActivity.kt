package com.fintracker.app

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.fintracker.app.data.local.BudgetDatabase
import com.fintracker.app.data.repository.SqliteBudgetRepository
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.presentation.budget.BudgetCommand
import com.fintracker.app.presentation.budget.BudgetController
import com.fintracker.app.presentation.budget.BudgetScreen
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var executor: ExecutorService
    private lateinit var controller: BudgetController
    private val expandedBucketIds = mutableSetOf("expenses")
    private var content: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = Executors.newSingleThreadExecutor()
        controller = BudgetController(SqliteBudgetRepository(BudgetDatabase(this)))
        content = FrameLayout(this).apply {
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
                insets
            }
        }
        setContentView(content)
        content?.requestApplyInsets()
        showLoading()
        loadPlan()
    }

    override fun onDestroy() {
        if (::executor.isInitialized) executor.shutdown()
        super.onDestroy()
    }

    private fun loadPlan() {
        executor.execute {
            runCatching { controller.loadPlan() }
                .onSuccess { plan -> runOnUiThread { render(plan) } }
                .onFailure { error -> runOnUiThread { showError(error) } }
        }
    }

    private fun execute(command: BudgetCommand) {
        executor.execute {
            runCatching { controller.execute(command) }
                .onSuccess { plan -> runOnUiThread { render(plan) } }
                .onFailure { error -> runOnUiThread {
                    Toast.makeText(this, error.message ?: "Could not save budget changes.", Toast.LENGTH_LONG).show()
                    loadPlan()
                } }
        }
    }

    private fun render(plan: BudgetPlan) {
        if (isFinishing || isDestroyed) return
        content?.removeAllViews()
        content?.addView(BudgetScreen(this, plan, expandedBucketIds, ::execute).createView())
    }

    private fun showLoading() {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(android.graphics.Color.rgb(245, 244, 239))
        frame.addView(ProgressBar(this), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        ))
        content?.addView(frame)
    }

    private fun showError(error: Throwable) {
        if (isFinishing || isDestroyed) return
        content?.removeAllViews()
        val message = TextView(this).apply {
            text = "Could not open your local budget.\n${error.message ?: "Please try again."}"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(48, 32, 48, 32)
        }
        content?.addView(message, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
    }
}
