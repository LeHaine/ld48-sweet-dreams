package com.lehaine.game.view

import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors


inline fun Container.healthBar(
    width: Double,
    height: Double,
    callback: HealthBar.() -> Unit = {}
): HealthBar = HealthBar(width, height).addTo(this).also(callback)

class HealthBar(private val initWidth: Double, private val initHeight: Double) : Container() {

    val base = solidRect(initWidth, initHeight, Colors["#7d0000"])
    val green = solidRect(initWidth, initHeight, Colors["#066d00"]) {
        centerOn(base)
        alignLeftToLeftOf(base)
    }


    fun setHealthRatio(ratio: Double) {
        green.scaledWidth = initWidth * ratio
    }

}