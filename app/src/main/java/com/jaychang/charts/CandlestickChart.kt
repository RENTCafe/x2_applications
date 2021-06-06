
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
            crosshairX = x.coerceIn(0f, graphWidth)
            crosshairY = y.coerceIn(0f, graphHeight)
            invalidate()
        }
    }

    override fun onPressMoving(x: Float, y: Float) {
        if (isCrosshairVisible) {
            crosshairX = x.coerceIn(0f, graphWidth)
            crosshairY = y.coerceIn(0f, graphHeight)
            invalidate()
        }
    }

    override fun onPressEnd() {
        if (isCrosshairVisible) {
            isCrosshairVisible = false
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetectionMediator.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLayout()
        canvas.drawTimeScale()
        canvas.drawPriceScale()
        canvas.drawCurrentPrice()
        canvas.drawCandlesticks()
        canvas.drawCrosshair()
    }

    // todo line overlap the view y=0, color is lighter.
    private fun Canvas.drawLayout() {
        val rowCount = config.layoutRowCount
        val rowInterval = graphHeight / rowCount
        val stopX = graphWidth + priceScalePadding
        val points = (0..rowCount).map {
            listOf(0f, rowInterval * it, stopX, rowInterval * it)
        }.flatten().toFloatArray()
        drawLines(points, layoutPaint)

        // Draw vertical line to separate price scale
        val verticalLineX = stopX - priceScalePadding
        drawLine(verticalLineX, 0f, verticalLineX, graphHeight, layoutPaint)
    }

    private fun Canvas.drawTimeScale() {
        fun drawTime(date: LocalDate, xOffset: Float) {
            val x = xOffset + candleWidth / 2
            drawLine(x, 0f, x, graphHeight, layoutPaint)

            drawText(
                date.formatted(),
                // align center with above candle
                x - timeScaleTextWidth / 2,
                height.toFloat(),
                timeScaleTextPaint
            )
        }

        fun isFirstMonth(data: BarData): Boolean {
            val group = dataGroupedByMonth[data.time.formatted()] ?: return false
            return group.first() == data
        }

        var xOffset = 0f
        for (data in dataWithinGraph) {
            if (isFirstMonth(data)) {
                drawTime(data.time, xOffset)
            }
            xOffset += candleWidth + candleSpace
        }
    }

    private fun Canvas.drawPriceScale() {
        val maxPrice = maxPrice
        val minPrice = minPrice
        val priceInterval = (maxPrice - minPrice) / config.layoutRowCount
        val textX = graphWidth + priceScalePadding
        val textY = priceScaleTextHeight
        val rowCount = config.layoutRowCount
        val rowHeight = graphHeight / config.layoutRowCount
        for (i in (0..rowCount)) {
            val yOffset = if (i == 0) {
                0f        // top
            } else if (i == rowCount) {
                textY     // bottom
            } else {
                textY / 2 // center
            }
            val text = priceScaleFormatter.format(maxPrice - priceInterval * i)
            drawText(text, textX, textY + rowHeight * i - yOffset, priceScaleTextPaint)
        }
    }

    private fun Canvas.drawCandlesticks() {
        fun drawCandlestick(data: BarData, xOffset: Float) {
            val paint = if (data.close >= data.open) candlestickUpPaint else candlestickDownPaint

            val maxPrice = maxPrice
            val minPrice = minPrice

            // Draw up wicks
            drawLine(
                xOffset + candleWidth / 2,
                y(max(data.open, data.close), maxPrice, minPrice),
                xOffset + candleWidth / 2,
                y(data.high, maxPrice, minPrice),
                paint
            )

            // Draw down wicks
            drawLine(
                xOffset + candleWidth / 2,
                y(min(data.open, data.close), maxPrice, minPrice),
                xOffset + candleWidth / 2,
                y(data.low, maxPrice, minPrice),
                paint
            )

            // Draw border
            // Draw 1 px line if open = close instead of empty border
            val borderOffset = if (data.open == data.close) 1f else 0f
            drawRect(
                xOffset,
                y(max(data.open, data.close), maxPrice, minPrice),
                xOffset + candleWidth,
                y(min(data.open, data.close), maxPrice, minPrice) + borderOffset,
                paint
            )
        }

        var xOffset = 0f
        for (data in dataWithinGraph) {
            drawCandlestick(data, xOffset)
            xOffset += candleWidth + candleSpace
        }
    }

    private fun y(price: Float, maxPrice: Float, minPrice: Float): Float {
        return (maxPrice - price) / (maxPrice - minPrice) * graphHeight
    }

    private fun updateData(data: List<BarData>) {
        dataStartIndex = (data.size - availableCandlesCount).coerceAtLeast(0)
        dataGroupedByMonth = data.groupBy { it.time.formatted() }
    }

    private fun LocalDate.formatted() = "$year/${timeScaleMonthFormatter.format(monthNumber)}"

    private fun Canvas.drawCurrentPrice() {
        val price = currentPrice ?: return
        val lastCandle = data.lastOrNull() ?: return

        // Draw price box
        val priceText = priceScaleFormatter.format(price)
        val textBound = priceText.textBound(priceScaleTextPaint)
        val textX = width.toFloat() - textBound.width() - priceScalePadding * 2
        val textY = y(price, maxPrice, minPrice)
        val currentPriceBoxPaint = if (price > lastCandle.open) currentPriceBoxUpPaint else currentPriceBoxDownPaint
        val topPadding = priceScalePadding / 2
        drawRect(
            textX,
            textY - textBound.height() - topPadding,
            textX + textBound.width() + priceScalePadding * 2,