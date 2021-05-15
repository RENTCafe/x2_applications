
package com.jaychang.charts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.doOnLayout
import kotlinx.datetime.LocalDate
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CandlestickChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttrs: Int = 0
) : View(context, attrs, defStyleAttrs), GestureDetectionMediator.Listener {
    var config: Config = Config()
        set(value) {
            field = value
            init(value)
        }

    var data: List<BarData> = emptyList()
        set(value) {
            field = value
            doOnLayout {
                updateData(value)
                invalidate()
            }
        }

    var currentPrice: Float? = null
        set(value) {
            field = value
            invalidate()
        }