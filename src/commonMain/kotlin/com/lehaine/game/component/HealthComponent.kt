package com.lehaine.game.component

import com.lehaine.kiwi.component.Component

interface HealthComponent : Component {
    val health: Int
    val maxHealth: Int
    val healthRatio: Double
    val isDead: Boolean
    fun damage(amount: Int, fromDir: Int)

}

class HealthComponentDefault(initialHealth: Int) : HealthComponent {

    override var health: Int = initialHealth
        private set

    override val maxHealth: Int = initialHealth

    override val healthRatio: Double
        get() = health / maxHealth.toDouble()

    override val isDead: Boolean
        get() = health <= 0

    override fun damage(amount: Int, fromDir: Int) {
        health -= amount
    }
}