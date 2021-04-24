package com.lehaine.game.entity

import com.lehaine.game.LevelMark
import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.kiwi.component.LevelEntity
import com.lehaine.kiwi.component.addTo
import com.lehaine.kiwi.component.addToLevel
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


    override fun update(dt: TimeSpan) {
        super.update(dt)

        if (!cd.has(SPAWN_CD)) {
            spawn()
            cd(SPAWN_CD, (2..5).random().seconds)
        }
    }

    fun spawn() {
        level.camera.content.apply {
            val spawn = level.spawnPoints.random()
            val rng = Random.nextFloat()
            if (rng > 0.5) {
                longArm(spawn.cx, spawn.cy, level)
            } else {
                sheep(spawn.cx, spawn.cy, level)
            }
        }
    }

}