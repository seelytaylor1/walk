package com.wanderingledger.core.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.wanderingledger.core.designsystem.R

class TopBar(
    context: Context,
    private val title: String,
    private val subtitle: String? = null,
    private val onBack: (() -> Unit)? = null,
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        setPadding(32, 32, 32, 16)
        setBackgroundColor(ContextCompat.getColor(context, R.color.primary_container_light))
    }

    fun render(title: String, subtitle: String?, showBack: Boolean) {
        removeAllViews()

        if (showBack && onBack != null) {
            val backButton = TextView(context).apply {
                text = "← Back"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.primary_light))
                setOnClickListener { onBack.invoke() }
            }
            addView(backButton)
        }

        val titleText = TextView(context).apply {
            text = title
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.on_primary_container_light))
        }
        addView(titleText)

        subtitle?.let {
            val subtitleText = TextView(context).apply {
                text = it
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.on_primary_container_light))
                alpha = 0.8f
            }
            addView(subtitleText)
        }
    }
}