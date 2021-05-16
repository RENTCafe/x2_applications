
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

    // Data
    private var dataStartIndex = 0
    private var dataGroupedByMonth = mapOf<String, List<BarData>>()
    private val dataWithinGraph: List<BarData>
        get() {
            val seriesEnd = (dataStartIndex + availableCandlesCount).coerceAtMost(data.size)
            return data.subList(dataStartIndex, seriesEnd)
        }
    private val availableCandlesCount: Int
        get() = (graphWidth / (candleWidth + candleSpace)).toInt()
    private val graphWidth: Float
        get() {
            val maxPrice = data.maxOf { it.high }
            val textBound = priceScaleFormatter.format(maxPrice).textBound(priceScaleTextPaint)
            return width.toFloat() - (textBound.width() + priceScalePadding * 2)
        }
    private val graphHeight: Float
        get() = height.toFloat() - timeScaleTextHeight
    private val maxPrice: Float
        get() = dataWithinGraph.maxOf { it.high }
    private val minPrice: Float
        get() = dataWithinGraph.minOf { it.low }

    // Layout
    private val layoutPaint = Paint()

    // Price scale
    private val priceScaleTextPaint = Paint()
    private val priceScaleFormatter = DecimalFormat("0.00")
    private var priceScaleTextHeight = 0f
    private var priceScalePadding = 0