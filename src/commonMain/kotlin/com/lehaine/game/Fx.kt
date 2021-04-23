package com.lehaine.game

import com.lehaine.kiwi.korge.getRandomByPrefix
import com.lehaine.kiwi.korge.particle.Particle
import com.lehaine.kiwi.korge.particle.ParticleSimulator
import com.lehaine.kiwi.random
import com.lehaine.kiwi.randomd
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.RGBA
import kotlin.math.PI

class Fx(val level: GameLevel, private val particleContainer: FastSpriteContainer) {

    private val particleSimulator = ParticleSimulator(2048)
    private var frame = 0

    private fun alloc(slice: BmpSlice, x: Double, y: Double) = particleSimulator.alloc(particleContainer, slice, x, y)

    fun update(dt: TimeSpan) {
        particleSimulator.simulate(dt)
        frame++
    }

    fun gutsSplatter(x: Double, y: Double, dir: Int) {
        create(50) {
            val p = alloc(Assets.tiles.getRandomByPrefix("fxDot"), x, y)
            p.color = RGBA((111..255).random(), 0, 0, (0..255).random())
            p.xDelta = dir * (3..7).randomd()
            p.yDelta = (-1..0).randomd()
            p.gravityY = (0.07..0.1).random()
            p.rotation = (0.0..PI * 2).random()
            p.friction = (0.92..0.96).random()
            p.rotation = (0.0..PI * 2).random()
            p.scale(0.7)
            p.life = (3..10).random().seconds
            p.onUpdate = ::bloodPhysics
        }
    }

    private fun bloodPhysics(particle: Particle) {
        if (particle.isColliding() && particle.data0 != 1) {
            particle.data0 = 1
            particle.xDelta *= 0.4
            particle.yDelta = 0.0
            particle.gravityY = (0.0..0.001).random()
            particle.friction = (0.5..0.7).random()
            particle.scaleDeltaY = (0.0..0.001).random()
            particle.rotation = 0.0
            particle.rotationDelta = 0.0
            if (particle.isColliding(-5) || particle.isColliding(5)) {
                particle.scaleY *= (1.0..1.25).random()
            }
            if (particle.isColliding(offsetY = -5) || particle.isColliding(offsetY = 5)) {
                particle.scaleX *= (1.0..1.25).random()
            }
        }
    }

    private fun create(num: Int, createParticle: (index: Int) -> Unit) {
        for (i in 0..num) {
            createParticle(i)
        }
    }

    private fun Particle.isColliding(offsetX: Int = 0, offsetY: Int = 0) =
        level.hasCollision(((x + offsetX) / GRID_SIZE).toInt(), ((y + offsetY) / GRID_SIZE).toInt())
}