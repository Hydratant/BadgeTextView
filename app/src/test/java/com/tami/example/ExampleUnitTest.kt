package com.tami.example

import org.junit.Test

import org.junit.Assert.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ"

    @Test
    fun date_test() {
        val now = Date()
        val date = SimpleDateFormat(DATE_PATTERN, Locale.US).format(now)

        println("date Format : $date")

        val left = daysLeft(now)
        val timeZone = TimeZone.getDefault()
        println("now.time  :$${now.time}, left : $left}")
    }

    fun daysLeft(date: Date): Int {
        val now = Date()
        return Integer.max(0, ((date.time - now.time) / (1000 * 60 * 60 * 24)).toInt())
    }


    fun betweenDay(date: Date, date2: Date): Int {
        val day1: Long = TimeUnit.DAYS.convert(date.time, TimeUnit.MILLISECONDS)
        val day2: Long = TimeUnit.DAYS.convert(date2.time, TimeUnit.MILLISECONDS)
        return (day2 - day1).toInt()
    }

}