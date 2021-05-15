package com.jaychang.charts

import kotlinx.datetime.LocalDate

data class BarData(
    val time: LocalDate,
    val open: Float,
    val high: Float,
    val low