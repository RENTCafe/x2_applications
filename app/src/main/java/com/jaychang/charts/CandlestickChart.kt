
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
            textY + topPadding,
            currentPriceBoxPaint
        )
        drawText(priceText, textX + priceScalePadding, textY, currentPriceTextPaint)

        // Draw line
        val currentPriceLinePaint = if (price > lastCandle.close) currentPriceLineUpPaint else currentPriceLineDownPaint
        val lineY = textY - textBound.height() / 2
        drawLine(0f, lineY, textX, lineY, currentPriceLinePaint)
    }

    private fun Canvas.drawCrosshair() {
        fun price(y: Float, maxPrice: Float, minPrice: Float, height: Float): Float {
            return maxPrice - y * (maxPrice - minPrice) / height
        }

        if (!isCrosshairVisible) return

        // Draw cross lines
        drawLine(0f, crosshairY, width.toFloat(), crosshairY, crosshairLinePaint)
        drawLine(crosshairX, 0f, crosshairX, graphHeight, crosshairLinePaint)

        // Draw price box
        val maxPrice = maxPrice
        val minPrice = minPrice
        val price = price(crosshairY, maxPrice, minPrice, graphHeight)
        val priceText = priceScaleFormatter.format(price)
        val textBound = priceText.textBound(priceScaleTextPaint)
        val textX = graphWidth
        val textY = y(price, maxPrice, minPrice) + textBound.height() / 2
        val topPadding = priceScalePadding / 2
        drawRect(
            textX,
            textY - textBound.height() - topPadding,
            textX + textBound.width() + priceScalePadding * 2,
            textY + topPadding,
            crosshairPriceBoxPaint
        )
        drawText(priceText, textX + priceScalePadding, textY, crosshairPriceTextPaint)
    }

    data class Config(
        val layoutBackgroundColor: Int = Color.WHITE,
        val layoutRowCount: Int = 4,
        val layoutLineColor: Int = Color.GRAY,
        val priceScaleTextColor: Int = Color.GRAY,
        val timeScaleTextColor: Int = Color.GRAY,
        val upColor: Int = Color.GREEN,
        val downColor: Int = Color.RED,
        val candleDefaultScale: Float = 5f,
        val candleMinScale: Float = 2f,
        val candleMaxScale: Float = 10f,
        val crosshairColor: Int = Color.GRAY,
        val crosshairPriceColor: Int = Color.WHITE
    )
}

// Move event is too sensitive that it also be dispatched to scroll detector when the scaling ends,
// which causes after scaling ends the chart will be suddenly moved. To workaround this, we threshold
// the interval time between scale end event and next scroll event, so the scroll action will
// be ignored if too fast between these two actions.
private class GestureDetectionMediator(context: Context, listener: Listener) {
    private val scaleGestureDetector = ChartScaleGestureDetector(context, object: ChartScaleGestureDetector.Listener {
        override fun onScale(detector: ChartScaleGestureDetector, factor: Float) {
            listener.onScale(factor)
        }
    })

    private val scrollGestureDetector = ChartScrollGestureDetector(object : ChartScrollGestureDetector.Listener {
        override fun getMinScrollDistance(): Float = listener.getMinScrollDistance()
        override fun onScroll(detector: ChartScrollGestureDetector, distance: Float) {
            val isScaling = scaleGestureDetector.isInProgress || !detector.isScrolling
            val isTooFastBetweenScaleAndScroll = abs(scaleGestureDetector.scaleEventTime - detector.scrollEventTime) <= SCROLL_SCALE_INTERVAL_THRESHOLD
            if (isScaling || isTooFastBetweenScaleAndScroll) {
                return
            }
            listener.onScroll(distance)
        }
    })

    private val pressDetector = ChartPressGestureDetector(context, object : ChartPressGestureDetector.Listener {
        override fun onPressBegin(detector: ChartPressGestureDetector, x: Float, y: Float) {
            listener.onPressBegin(x, y)
        }

        override fun onPressMoving(detector: ChartPressGestureDetector, x: Float, y: Float) {
            listener.onPressMoving(x, y)
        }

        override fun onPressEnd(detector: ChartPressGestureDetector) {
            listener.onPressEnd()
        }
    })

    fun onTouchEvent(event: MotionEvent) {
        pressDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress && !pressDetector.isPressing) {
            scrollGestureDetector.onTouchEvent(event)
        }
    }

    interface Listener {
        fun onScale(factor: Float)
        fun getMinScrollDistance(): Float
        fun onScroll(distance: Float)
        fun onPressBegin(x: Float, y: Float)
        fun onPressMoving(x: Float, y: Float)
        fun onPressEnd()
    }

    private companion object {
        private const val SCROLL_SCALE_INTERVAL_THRESHOLD = 500L
    }
}

private class ChartPressGestureDetector(context: Context, private val listener: Listener) {
    private var _isPressing = false
    val isPressing: Boolean
        get() = _isPressing

    private var isLongPressed = false

    private val pressDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            isLongPressed = true
            _isPressing = true
            listener.onPressBegin(this@ChartPressGestureDetector, e.x, e.y)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            listener.onPressMoving(this@ChartPressGestureDetector, e2.x, e2.y)
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent) {
        pressDetector.onTouchEvent(event)
        // As long press block the subsequent onScroll event, to workaround it,
        // we set a cancel event to it so we get the onScroll event.
        // See https://stackoverflow.com/a/56545079
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (isLongPressed) {
                    isLongPressed = false
                    val cancel = MotionEvent.obtain(event)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    pressDetector.onTouchEvent(cancel)
                }
            }
            MotionEvent.ACTION_UP -> {
                _isPressing = false
                listener.onPressEnd(this@ChartPressGestureDetector)
            }
        }
    }

    interface Listener {
        fun onPressBegin(detector: ChartPressGestureDetector, x: Float, y: Float)
        fun onPressMoving(detector: ChartPressGestureDetector, x: Float, y: Float)
        fun onPressEnd(detector: ChartPressGestureDetector)
    }
}

private class ChartScaleGestureDetector(context: Context, listener: Listener) {
    private var _scaleEventTime = 0L
    val scaleEventTime: Long
        get() = _scaleEventTime

    val isInProgress: Boolean
        get() = scaleDetector.isInProgress

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            _scaleEventTime = 0
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            _scaleEventTime = detector.eventTime
            listener.onScale(this@ChartScaleGestureDetector, detector.scaleFactor)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            _scaleEventTime = detector.eventTime
        }
    })

    fun onTouchEvent(event: MotionEvent) {
        scaleDetector.onTouchEvent(event)
    }

    interface Listener {
        fun onScale(detector: ChartScaleGestureDetector, factor: Float)
    }
}

private class ChartScrollGestureDetector(private val listener: Listener) {
    private var lastX = 0f

    private var _scrollEventTime = 0L
    val scrollEventTime: Long
        get() = _scrollEventTime

    private var _isScrolling = false
    val isScrolling: Boolean
        get() = _isScrolling


    fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                _scrollEventTime = event.downTime
                lastX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                _scrollEventTime = event.eventTime
                _isScrolling = event.pointerCount == 1
                val dx = event.x - lastX
                val minMoveDistance = listener.getMinScrollDistance()
                if (abs(dx) >= minMoveDistance) {
                    listener.onScroll(this, dx)
                    lastX = event.x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                _scrollEventTime = 0
            }
        }
    }

    interface Listener {
        fun getMinScrollDistance(): Float
        fun onScroll(detector: ChartScrollGestureDetector, distance: Float)
    }
}

private fun String.textBound(paint: Paint): Rect {
    val rect = Rect()
    paint.getTextBounds(this, 0, this.length, rect)
    return rect
}