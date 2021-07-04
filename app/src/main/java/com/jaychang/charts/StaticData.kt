
package com.jaychang.charts

import kotlinx.datetime.LocalDate

fun barDataList(): List<BarData> {
    return listOf(
        BarData(
            time = LocalDate.parse("2018-10-19"),
            open = 180.34f,
            high = 180.99f,
            low = 178.57f,
            close = 179.85f