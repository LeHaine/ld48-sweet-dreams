package com.lehaine.game.component

import com.lehaine.kiwi.component.Component

interface DangerousComponent : Component {
    val damage: Int
    fun attack(other: HealthComponent, fromDir: Int)
}

class DangerousComponentDefault(override val damage: Int) : DangerousComponent {
    override fun attack(other: HealthComponent, fromDir: Int) {
        other.damage(damage, fromDir)
    }
}