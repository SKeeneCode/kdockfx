package kdockfx

import javafx.scene.shape.*
import kotlin.math.max

open class TabDropHint {

    private var tabPos: Double = -1.0
    private var width: Double = 0.0
    private var height: Double = 0.0
    private var startX: Double = 0.0
    private var startY: Double = 0.0
    val path: Path = Path().apply {
        styleClass.add("drop-path")
    }

    fun refresh(startX: Double, startY: Double, width: Double, height: Double) {
        val shouldRegenerate = this.tabPos != -1.0
                || this.width != width
                || this.height != height
                || this.startX != startX
                || this.startY != startY
        this.tabPos = -1.0
        this.width = width
        this.height = height
        this.startX = startX
        this.startY = startY
        if (shouldRegenerate) {
           generateAdjacentPath(path, startX + 2, startY + 2, width - 4, height - 4)
        }
    }

    fun refresh(tabPos: Double, width: Double, height: Double) {
        val shouldRegenerate = this.tabPos != tabPos
                || this.width != width
                || this.height != height
        this.tabPos = tabPos
        this.width = width
        this.height = height
        startX = 0.0
        startY = 0.0
        if (shouldRegenerate) {
            generateInsertionPath(path, tabPos, width - 2, height - 2)
        }
    }


    private fun generateAdjacentPath(path: Path, startX: Double, startY: Double, width: Double, height: Double) {
        val moveTo = MoveTo().apply {
            x = startX
            y = startY
        }
        with(path.elements) {
            clear()
            add(moveTo)
            add(HLineTo(startX + width))
            add(VLineTo(startY + height))
            add(HLineTo(startX))
            add(VLineTo(startY))
        }
    }

    private fun generateInsertionPath(path: Path, tabPos: Double, width: Double, height: Double) {
        val tabHeight = 28.0
        val start = 2.0
        val tabPos = max(tabPos, start)
        val moveTo = MoveTo().apply {
            x = start
            y = tabHeight
        }

        with(path.elements) {
            clear()
            add(moveTo)
            add(HLineTo(width))
            add(VLineTo(height))
            add(HLineTo(start))
            add(VLineTo(tabHeight))
            if (tabPos > 20) {
                add(MoveTo(tabPos, tabHeight + 5))
                add(LineTo(max(start, tabPos - 10), tabHeight + 15))
                add(HLineTo(tabPos + 10))
                add(LineTo(tabPos, tabHeight + 5))
            } else {
                val tip = max(tabPos, start + 5)
                add(MoveTo(tip, tabHeight + 5))
                add(LineTo(tip + 10, tabHeight + 5))
                add(LineTo(tip, tabHeight + 15))
                add(VLineTo(tabHeight + 5))
            }
        }



    }


}