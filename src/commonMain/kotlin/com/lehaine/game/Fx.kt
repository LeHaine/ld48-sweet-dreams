package com.lehaine.game

import com.lehaine.kiwi.korge.getByPrefix
import com.lehaine.kiwi.korge.getRandomByPrefix
import com.lehaine.kiwi.korge.particle.Particle
import com.lehaine.kiwi.korge.particle.ParticleSimulator
import com.lehaine.kiwi.random
import com.lehaine.kiwi.randomd
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.kmem.umod
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import kotlin.math.PI

class Fx(val game: Game, private val particleContainer: FastSpriteContainer) {

    private val particleSimulator = ParticleSimulator(2048)
    private var frame = 0

    private fun alloc(slice: BmpSlice, x: Double, y: Double) = particleSimulator.alloc(particleContainer, slice, x, y)

    fun update(dt: TimeSpan) {
        particleSimulator.simulate(dt, game.tmod)
        frame++
    }

    fun sleepyZs(x: Double, y: Double) {
        create(1) {
            val p = alloc(Assets.tiles.getByPrefix("fxZ"), (x - 2..x + 2).random(), y)
            p.scale((0.2..0.4).random())
            p.yDelta = -0.25
            p.life = 0.5.seconds
            p.color = Colors["#ffe358"]
            p.scaleDelta = (0.004..0.007).random()
        }
    }

    fun broomDust(x: Double, y: Double, dir: Int) {
        create(15) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), x, y)
            p.scale((0.1..0.3).random())
            p.color = RGBA((227..247).random(), (224..244).random(), (40..60).random(), (0..255).random())
            p.xDelta = (0.15..0.25).random() * if (it umod 2 == 0) 1 else -1
            p.yDelta = (-1..1).randomd()
            p.life = (0.25..0.35).random().seconds
            p.gravityY = (0.025..0.04).random()
            p.onUpdate = ::hardPhysics
        }
    }

    fun poofDust(x: Double, y: Double, color: RGBA = Colors.RED) {
        create(50) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), ((x - 4)..(x + 4)).random(), y)
            p.scale((0.3..0.5).random())
            p.color = RGBA(
                (color.r - 10..color.r + 10).random(),
                (color.g - 10..color.g + 10).random(),
                (color.b - 10..color.b + 10).random(),
                (0..255).random()
            )
            p.yDelta = (-0.25..0.0).random()
            p.xDelta = (0.25..1.25).random() * if (it umod 2 == 0) 1 else -1
            p.frictionX = 0.9
            p.rotationDelta = (0.0..(PI * 2)).random()
            p.scaleDelta = -(0.0025..0.004).random()
            p.life = (1.0..1.25).random().seconds
        }
    }

    fun runDust(x: Double, y: Double, dir: Int) {
        create(5) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), x, y)
            p.scale((0.05..0.15).random())
            p.color = Colors["#efddc0"]
            p.xDelta = (0.25..0.75).random() * dir
            p.yDelta = -(0.05..0.15).random()
            p.life = (0.05..0.15).random().seconds
            p.scaleDelta = (0.005..0.015).random()
        }
    }

    fun bossDeath(x: Double, y: Double) {
        val color = Colors["#3d3a45"]
        create(100) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), ((x - 4)..(x + 4)).random(), y)
            p.scale((0.5..0.7).random())
            p.color = RGBA(
                (color.r - 10..color.r + 10).random(),
                (color.g - 10..color.g + 10).random(),
                (color.b - 10..color.b + 10).random(),
                (200..255).random()
            )
            p.yDelta = (-0.85..0.0).random()
            p.xDelta = (0.45..1.45).random() * if (it umod 2 == 0) 1 else -1
            p.frictionX = 0.95
            p.rotationDelta = (0.0..(PI * 2)).random()
            p.scaleDelta = -(0.0025..0.004).random()
            p.life = (1.5..2.0).random().seconds
        }
    }


    fun bossDust(x: Double, y: Double) {
        val color = Colors["#3d3a45"]
        create(10) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), ((x - 4)..(x + 4)).random(), y)
            p.scale((0.5..0.7).random())
            p.color = RGBA(
                (color.r - 10..color.r + 10).random(),
                (color.g - 10..color.g + 10).random(),
                (color.b - 10..color.b + 10).random(),
                (200..255).random()
            )
            p.yDelta = (-0.85..0.0).random()
            p.xDelta = (0.45..1.45).random() * if (it umod 2 == 0) 1 else -1
            p.frictionX = 0.95
            p.rotationDelta = (0.0..(PI * 2)).random()
            p.scaleDelta = -(0.0025..0.004).random()
            p.life = (1.5..2.0).random().seconds
        }
    }

    fun dust(x: Double, y: Double) {
        create(5) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), ((x - 4)..(x + 4)).random(), y)
            p.scale((0.1..0.3).random())
            p.color = RGBA((198..218).random(), (157..177).random(), (136..156).random(), (225..255).random())
            p.yDelta = (0.025..0.075).random()
            p.xDelta = (0.125..0.2).random() * if (it umod 2 == 0) 1 else -1
            p.life = (0.5..1.5).random().seconds
            p.onUpdate = ::hardPhysics
        }
    }

    fun dustExplosion(x: Double, y: Double) {
        create(15) {
            val p = alloc(Assets.tiles.getByPrefix("fxSmallCircle"), ((x - 4)..(x + 4)).random(), y)
            p.scale((0.1..0.3).random())
            p.color = RGBA((198..218).random(), (157..177).random(), (136..156).random(), (225..255).random())
            p.yDelta = (-1..1).randomd()
            p.xDelta = (1.0..2.0).random() * if (it umod 2 == 0) 1 else -1
            p.gravityY = (0.07..0.1).random()
            p.friction = (0.92..0.96).random()
            p.rotationDelta = (0.0..(PI * 2)).random()
            p.life = (2.5..4.5).random().seconds
            p.onUpdate = ::hardPhysics
        }
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

    private fun hardPhysics(particle: Particle) {
        if (particle.isColliding() && particle.data0 != 1) {
            particle.data0 = 1
            particle.xDelta *= 0.5
            particle.yDelta = 0.0
            particle.gravityY = (0.0..0.001).random()
            particle.rotationDelta = 0.0
            particle.friction = 0.8
            particle.rotation *= 0.03
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
        for (i in 0 until num) {
            createParticle(i)
        }
    }

    private fun Particle.isColliding(offsetX: Int = 0, offsetY: Int = 0) =
        game.level.hasCollision(((x + offsetX) / GRID_SIZE).toInt(), ((y + offsetY) / GRID_SIZE).toInt())
}