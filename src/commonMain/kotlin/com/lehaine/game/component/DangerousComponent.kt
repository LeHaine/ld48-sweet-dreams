package com.lehaine.game.component

import com.lehaine.kiwi.component.Component

interface DangerousComponent : Component {
    val damage: Int
    fun attack(other: HealthComponent, fromDir: Int, multiplier: Double = 1.0)
}

class DangerousComponentDefault(override val damage: Int) : DangerousComponent {
    override fun attack(other: HealthComponent, fromDir: Int, multiplier: Double) {
        other.damage((damage * multiplier).toInt(), fromDir)
    }
}