package com.bunkerstock.app.domain

import com.bunkerstock.app.platform.BunkerDate
import java.time.LocalDate

fun LocalDate.toBunkerDate(): BunkerDate = BunkerDate(year, monthValue, dayOfMonth)
fun BunkerDate.toJavaLocalDate(): LocalDate = LocalDate.of(year, month, day)
