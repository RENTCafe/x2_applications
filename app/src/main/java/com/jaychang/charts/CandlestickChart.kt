
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

    // Time scale
    private val timeScaleTextPaint = Paint()
    private val timeScaleMonthFormatter = DecimalFormat("00")
    private var timeScaleTextWidth = 0f
    private var timeScaleTextHeight = 0f

    // Current price
    private val currentPriceLineUpPaint = Paint()
    private val currentPriceLineDownPaint = Paint()
    private val currentPriceBoxUpPaint = Paint()
    private val currentPriceBoxDownPaint = Paint()
    private val currentPriceTextPaint = Paint()

    // Candlestick
    private var _candleWidth = 0f
    private val candleWidth: Float
        get() = _candleWidth * candleScale
    private var _candleSpace = 0f
    private val candleSpace: Float
        get() = _candleSpace * candleScale
    private val candlestickUpPaint = Paint()
    private val candlestickDownPaint = Paint()
    private var candleScale = 0f

    // Crosshair
    private var crosshairX = 0f
    private var crosshairY = 0f
    private val crosshairLinePaint = Paint()
    private val crosshairPriceBoxPaint = Paint()
    private val crosshairPriceTextPaint = Paint()
    private var isCrosshairVisible = false

    private val gestureDetectionMediator = GestureDetectionMediator(context, this)

    init {
        init()
    }

    private fun init(config: Config = Config()) {
        initLayout(config)
        initTimeScale(config)
        initPriceScale(config)
        initCurrentPrice(config)
        initCandlestick(config)
        initCrosshair(config)
    }

    private fun initLayout(config: Config) {
        layoutPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = config.layoutLineColor