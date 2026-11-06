package com.example.workinyourspace.presentation

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle

object WidgetUtils {
    /**
     * Calculates grid column and row spans for an AppWidgetProviderInfo,
     * obeying Android 12+ (API 31) targetCellWidth / targetCellHeight standards
     * and fallback minWidth / minHeight formulas.
     */
    fun calculateWidgetSpan(
        info: AppWidgetProviderInfo,
        maxCols: Int = 4,
        maxRows: Int = 6
    ): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (info.targetCellWidth > 0 && info.targetCellHeight > 0) {
                return Pair(
                    info.targetCellWidth.coerceIn(1, maxCols),
                    info.targetCellHeight.coerceIn(1, maxRows)
                )
            }
        }

        // Standard Android span conversion formula from minWidth / minHeight (dp):
        // N = (dp + 30) / 70
        val spanX = ((info.minWidth + 30) / 70).coerceIn(1, maxCols)
        val spanY = ((info.minHeight + 30) / 70).coerceIn(1, maxRows)
        return Pair(spanX, spanY)
    }

    /**
     * Updates AppWidgetManager options bundle with standard size bounds in dp.
     */
    fun updateWidgetSizeOptions(
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        spanX: Int,
        spanY: Int,
        cellWidthPx: Float,
        cellHeightPx: Float,
        density: Float
    ) {
        if (appWidgetManager == null || appWidgetId == -1 || density <= 0f) return
        try {
            val widthDp = ((spanX * cellWidthPx) / density).toInt()
            val heightDp = ((spanY * cellHeightPx) / density).toInt()

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
            appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
