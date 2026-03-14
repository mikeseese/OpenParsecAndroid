package com.aigch.openparsec.parsec

/**
 * Cursor position helper for coordinate conversion between client and host.
 * Ported from iOS CursorPositionHelper class.
 */
object CursorPositionHelper {
    fun toHost(xp: Int, yp: Int): Pair<Int, Int> {
        val xh = CParsec.hostWidth
        val yh = CParsec.hostHeight
        val xc = CParsec.clientWidth
        val yc = CParsec.clientHeight

        val tc = yc / xc
        val th = yh / xh

        val xa: Float
        val ya: Float
        if (th < tc) {
            xa = xp.toFloat() * xh / xc
            ya = (yp.toFloat() - 0.5f * (yc - xc * th)) * xh / xc
        } else {
            ya = yp.toFloat() * yh / yc
            xa = (xp.toFloat() - 0.5f * (xc - yc / th)) * yh / yc
        }

        return Pair(
            ParsecSDKBridge.clamp(xa, 0f, CParsec.hostWidth).toInt(),
            ParsecSDKBridge.clamp(ya, 0f, CParsec.hostHeight).toInt()
        )
    }

    fun toClient(xa: Int, ya: Int): Pair<Int, Int> {
        val xh = CParsec.hostWidth
        val yh = CParsec.hostHeight
        val xc = CParsec.clientWidth
        val yc = CParsec.clientHeight

        val tc = yc / xc
        val th = yh / xh

        val xp: Float
        val yp: Float
        if (th < tc) {
            xp = xa.toFloat() * xc / xh
            yp = ya.toFloat() * xc / xh + 0.5f * (yc - xc * th)
        } else {
            yp = ya.toFloat() * yc / yh
            xp = xa.toFloat() * yc / yh + 0.5f * (xc - yc / th)
        }

        return Pair(
            ParsecSDKBridge.clamp(xp, 0f, CParsec.clientWidth).toInt(),
            ParsecSDKBridge.clamp(yp, 0f, CParsec.clientHeight).toInt()
        )
    }
}
