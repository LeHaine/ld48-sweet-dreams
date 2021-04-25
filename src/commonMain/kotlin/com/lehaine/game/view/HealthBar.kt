package com.lehaine.game.view

import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA


inline fun Container.healthBar(
    width: Double,
    height: Double,
    greenColor: RGBA = Colors["#066d00"],
    callback: HealthBar.() -> Unit = {}
): HealthBar = HealthBar(width, height, greenColor).addTo(this).also(callback)

class HealthBar(
    private val initWidth: Double,
    private val initHeight: Double,
    private val greenColor: RGBA = Colors["#066d00"]
) : Container() {

    val base = solidRect(initWidth, initHeight, Colors["#7d0000"])
    val green = solidRect(initWidth, initHeight, greenColor) {
        centerOn(base)
        alignLeftToLeftOf(base)
    }


    fun setHealthRatio(ratio: Double) {
        green.scaledWidth = initWidth * ratio
    }

}