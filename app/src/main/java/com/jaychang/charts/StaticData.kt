
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
        ),
        BarData(
            time = LocalDate.parse("2018-10-22"),
            open = 180.82f,
            high = 181.40f,
            low = 177.56f,
            close = 178.75f
        ),
        BarData(
            time = LocalDate.parse("2018-10-23"),
            open = 175.77f,
            high = 179.49f,
            low = 175.44f,
            close = 178.53f
        ),
        BarData(
            time = LocalDate.parse("2018-10-24"),
            open = 178.58f,
            high = 182.37f,
            low = 176.31f,
            close = 176.97f