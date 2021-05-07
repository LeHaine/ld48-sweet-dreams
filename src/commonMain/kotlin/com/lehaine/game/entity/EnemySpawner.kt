package com.lehaine.game.entity

import com.lehaine.game.Game
import com.lehaine.kiwi.component.LevelEntity
import com.lehaine.kiwi.component.addTo
import com.lehaine.kiwi.component.addToGame
import com.lehaine.kiwi.random
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import kotlin.random.Random

fun Container.enemySpawner(game: Game): EnemySpawner =
    EnemySpawner(game).addTo(this).addToGame()

class EnemySpawner(override val game: Game) : LevelEntity(game) {

    companion object {
        const val SPAWN_CD = "spawnCD"
    }

    var shouldSpawn = true

    val camera get() = game.camera

    init {
        cd(SPAWN_CD, 10.seconds)
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)

        if (!cd.has(SPAWN_CD)) {
            spawn()
        }
    }

    fun spawn() {
        if (!shouldSpawn) return

        camera.content.apply {
            val spawn = game.spawnPoints.random()
            val rng = Random.nextFloat()

            when (game.sleepState) {
                Game.SleepState.VeryLightSleep -> {
                    when {
                        rng > 0.5 -> {
                            sheep(spawn.cx, spawn.cy, game)
                        }
                        else -> {
                            dustBunny(spawn.cx, spawn.cy, game)
                        }
                    }
                    cd(SPAWN_CD, (1..2).random().seconds)
                }
                Game.SleepState.LightSleep -> {
                    when {
                        rng > 0.8 -> {
                            sheep(spawn.cx, spawn.cy, game, 2.0, 2.0)
                        }
                        else -> {
                            dustBunny(spawn.cx, spawn.cy, game, 2.0, 2.0)
                        }
                    }
                    cd(SPAWN_CD, (1.0..1.5).random().seconds)
                }
                Game.SleepState.MediumSleep -> {
                    when {
                        rng > 0.6 -> {
                            dustBunny(spawn.cx, spawn.cy, game, 2.25, 2.25)
                        }
                        else -> {
                            ghoul(spawn.cx, spawn.cy, game)
                        }
                    }
                    cd(SPAWN_CD, (1.5..4.0).random().seconds)
                }
                Game.SleepState.DeeperSleep -> {
                    when {
                        rng > 0.8 -> {
                            dustBunny(spawn.cx, spawn.cy, game, 2.5, 2.5)
                        }
                        rng > 0.4 -> {
                            ghoul(spawn.cx, spawn.cy, game)
                        }
                        else -> {
                            longArm(spawn.cx, spawn.cy, game)
                        }
                    }
                    cd(SPAWN_CD, (2.0..3.5).random().seconds)
                }
                Game.SleepState.EvenDeeperSleep -> {
                    when {
                        rng > 0.5 -> {
                            ghoul(spawn.cx, spawn.cy, game, 1.25, 1.25)
                        }
                        else -> {
                            longArm(spawn.cx, spawn.cy, game, 1.25, 1.25)
                        }
                    }
                    cd(SPAWN_CD, (2.0..3.5).random().seconds)
                }
                Game.SleepState.DeepestSleep -> {
                    boss(spawn.cx, spawn.cy, game)
                    destroy()
                }
            }
        }
    }

}