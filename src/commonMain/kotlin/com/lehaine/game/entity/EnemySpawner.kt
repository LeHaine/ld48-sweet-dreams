package com.lehaine.game.entity

import com.lehaine.game.LevelMark
import com.lehaine.game.SleepState
import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.kiwi.component.LevelEntity
import com.lehaine.kiwi.component.addTo
import com.lehaine.kiwi.component.addToLevel
import com.lehaine.kiwi.random
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import kotlin.random.Random

fun Container.enemySpawner(level: GenericGameLevelComponent<LevelMark>): EnemySpawner =
    EnemySpawner(level).addTo(this).addToLevel()

class EnemySpawner(override val level: GenericGameLevelComponent<LevelMark>) : LevelEntity(level) {

    companion object {
        const val SPAWN_CD = "spawnCD"
    }

    var shouldSpawn = true

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

        level.camera.content.apply {
            val spawn = level.spawnPoints.random()
            val rng = Random.nextFloat()

            when (level.sleepState) {
                SleepState.VeryLightSleep -> {
                    when {
                        rng > 0.5 -> {
                            sheep(spawn.cx, spawn.cy, level)
                        }
                        else -> {
                            dustBunny(spawn.cx, spawn.cy, level)
                        }
                    }
                    cd(SPAWN_CD, (1..2).random().seconds)
                }
                SleepState.LightSleep -> {
                    when {
                        rng > 0.8 -> {
                            sheep(spawn.cx, spawn.cy, level, 2.0, 2.0)
                        }
                        else -> {
                            dustBunny(spawn.cx, spawn.cy, level, 2.0, 2.0)
                        }
                    }
                    cd(SPAWN_CD, (1.0..1.5).random().seconds)
                }
                SleepState.MediumSleep -> {
                    when {
                        rng > 0.6 -> {
                            dustBunny(spawn.cx, spawn.cy, level, 2.25, 2.25)
                        }
                        else -> {
                            ghoul(spawn.cx, spawn.cy, level)
                        }
                    }
                    cd(SPAWN_CD, (1.5..4.0).random().seconds)
                }
                SleepState.DeeperSleep -> {
                    when {
                        rng > 0.8 -> {
                            dustBunny(spawn.cx, spawn.cy, level, 2.5, 2.5)
                        }
                        rng > 0.4 -> {
                            ghoul(spawn.cx, spawn.cy, level)
                        }
                        else -> {
                            longArm(spawn.cx, spawn.cy, level)
                        }
                    }
                    cd(SPAWN_CD, (1.0..3.5).random().seconds)
                }
                SleepState.EvenDeeperSleep -> {
                    when {
                        rng > 0.5 -> {
                            ghoul(spawn.cx, spawn.cy, level, 1.25, 1.25)
                        }
                        else -> {
                            longArm(spawn.cx, spawn.cy, level, 1.25, 1.25)
                        }
                    }
                    cd(SPAWN_CD, (1.0..2.5).random().seconds)
                }
                SleepState.DeepestSleep -> {
                    boss(spawn.cx, spawn.cy, level)
                    destroy()
                }
            }
        }
    }

}