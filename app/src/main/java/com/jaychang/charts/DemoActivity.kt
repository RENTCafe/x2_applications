package com.jaychang.charts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)
        val chart = findViewById<CandlestickChart>(R.id.chart)
        chart.data 