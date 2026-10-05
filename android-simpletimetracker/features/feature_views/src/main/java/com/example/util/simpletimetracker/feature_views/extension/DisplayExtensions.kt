package com.example.util.simpletimetracker.feature_views.extension

import android.content.res.Resources
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.COMPLEX_UNIT_SP
import kotlin.math.roundToInt

fun Float.dpToPx(): Int = TypedValue.applyDimension(COMPLEX_UNIT_DIP, this, getDisplayMetrics()).roundToInt()

fun Int.dpToPx(): Int = this.toFloat().dpToPx()

fun Float.pxToDp(): Int = (this / getDisplayMetrics().density).roundToInt()

fun Int.pxToDp(): Int = this.toFloat().pxToDp()

fun Float.spToPx(): Int = TypedValue.applyDimension(COMPLEX_UNIT_SP, this, getDisplayMetrics()).roundToInt()

fun Float.pxToSp(): Float = this / getDisplayMetrics().scaledDensity

fun Int.spToPx(): Int = this.toFloat().spToPx()

private fun getDisplayMetrics() = Resources.getSystem().displayMetrics