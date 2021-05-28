
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
        }
    }

    private fun initTimeScale(config: Config) {
        timeScaleTextPaint.apply {
            isAntiAlias = true
            textSize = resources.getDimension(R.dimen.time_scale_text_size)
            color = config.timeScaleTextColor
        }
        val timeScaleText = "2022/05".textBound(timeScaleTextPaint)
        timeScaleTextWidth = timeScaleText.width().toFloat()
        timeScaleTextHeight = timeScaleText.height().toFloat()
    }

    private fun initPriceScale(config: Config) {
        priceScaleTextPaint.apply {
            isAntiAlias = true
            textSize = resources.getDimension(R.dimen.price_scale_text_size)
            color = config.priceScaleTextColor
        }
        priceScaleTextHeight = priceScaleFormatter.format(100f).textBound(priceScaleTextPaint).height().toFloat()
        priceScalePadding = resources.getDimensionPixelSize(R.dimen.price_scale_padding)
    }

    private fun initCurrentPrice(config: Config) {
        fun Paint.applyCommon() {
            isAntiAlias = true
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f, 4f, 4f), 0f)
            strokeWidth = 2f
        }
        currentPriceLineUpPaint.apply {
            applyCommon()
            color = config.upColor
        }
        currentPriceLineDownPaint.apply {
            applyCommon()
            color = config.downColor
        }
        currentPriceTextPaint.apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = resources.getDimensionPixelSize(R.dimen.price_scale_text_size).toFloat()
        }
        currentPriceBoxUpPaint.apply {
            color = config.upColor
        }
        currentPriceBoxDownPaint.apply {
            color = config.downColor
        }
    }

    private fun initCandlestick(config: Config) {
        _candleWidth = context.resources.getDimension(R.dimen.candle_width)
        _candleSpace = context.resources.getDimension(R.dimen.candle_space)
        candleScale = config.candleDefaultScale
        candlestickUpPaint.apply {
            isAntiAlias = true
            color = config.upColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        candlestickDownPaint.apply {
            isAntiAlias = true
            color = config.downColor
            style = Paint.Style.FILL
        }
    }

    private fun initCrosshair(config: Config) {
        crosshairLinePaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f, 4f, 4f), 0f)
            strokeWidth = 2f
            color = config.crosshairColor
        }
        crosshairPriceBoxPaint.apply {
            color = config.crosshairColor
        }
        crosshairPriceTextPaint.apply {
            isAntiAlias = true
            textSize = resources.getDimensionPixelSize(R.dimen.price_scale_text_size).toFloat()
            color = config.crosshairPriceColor
        }
    }

    override fun onScale(factor: Float) {
        candleScale *= factor
        candleScale = candleScale.coerceIn(config.candleMinScale, config.candleMaxScale)
        invalidate()
    }

    override fun getMinScrollDistance(): Float = candleWidth + candleSpace

    override fun onScroll(distance: Float) {
        val minMoveDistance = getMinScrollDistance()
        val count = (-distance / minMoveDistance).toInt()
        if (abs(count) >= 1) {
            dataStartIndex += count
            dataStartIndex = dataStartIndex.coerceIn(0, data.size - availableCandlesCount)
            invalidate()
        }
    }

    override fun onPressBegin(x: Float, y: Float) {
        if (!isCrosshairVisible) {
            isCrosshairVisible = true