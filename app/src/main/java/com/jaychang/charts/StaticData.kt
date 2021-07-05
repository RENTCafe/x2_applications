
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
        ),
        BarData(
            time = LocalDate.parse("2018-10-25"),
            open = 177.52f,
            high = 180.50f,
            low = 176.83f,
            close = 179.07f
        ),
        BarData(
            time = LocalDate.parse("2018-10-26"),
            open = 176.88f,
            high = 177.34f,
            low = 170.91f,
            close = 172.23f
        ),
        BarData(
            time = LocalDate.parse("2018-10-29"),
            open = 173.74f,
            high = 175.99f,
            low = 170.95f,
            close = 173.20f
        ),