package com.lehaine.game.component

import com.lehaine.kiwi.component.Component

interface HealthComponent : Component {
    val health: Int
    val isDead: Boolean
    fun damage(amount: Int, fromDir: Int)
}

class HealthComponentDefault(initialHealth: Int) : HealthComponent {

    override var health: Int = initialHealth
        private set

    override val isDead: Boolean
        get() = health <= 0

    override fun damage(amount: Int, fromDir: Int) {
        health -= amount
    }
}