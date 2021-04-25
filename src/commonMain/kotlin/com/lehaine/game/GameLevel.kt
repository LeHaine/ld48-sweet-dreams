package com.lehaine.game

import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.game.entity.Hero
import com.lehaine.kiwi.component.Entity
import com.lehaine.kiwi.korge.view.CameraContainer
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.kmem.clamp

class GameLevel(val level: World.WorldLevel) : GenericGameLevelComponent<LevelMark> {
    var _camera: CameraContainer? = null
    var _fx: Fx? = null
    var _hero: Hero? = null

    override var gameFinshed: Boolean = false
    override var slingShotCDRemaining: TimeSpan = TimeSpan.ZERO
    override var sleepState: SleepState = SleepState.VeryLightSleep

    override val camera get() = _camera!!
    override val fx get() = _fx!!
    override val hero get() = _hero!!

    override val entities: ArrayList<Entity> = arrayListOf()
    override val staticEntities: ArrayList<Entity> = arrayListOf()
    override val spawnPoints: ArrayList<World.EntitySpawners> = arrayListOf()

    override val levelWidth get() = level.layerCollisions.cWidth
    override val levelHeight get() = level.layerCollisions.cHeight

    private val marks = mutableMapOf<LevelMark, MutableMap<Int, Int>>()

    // a list of collision layers indices from LDtk world
    private val collisionLayers = intArrayOf(1)
    private val collisionLayer = level.layerCollisions

    init {
        createLevelMarks()
    }

    override fun isValid(cx: Int, cy: Int) = collisionLayer.isCoordValid(cx, cy)
    override fun getCoordId(cx: Int, cy: Int) = collisionLayer.getCoordId(cx, cy)

    override fun hasCollision(cx: Int, cy: Int): Boolean {
        return if (isValid(cx, cy)) {
            collisionLayers.contains(collisionLayer.getInt(cx, cy))
        } else {
            true
        }
    }

    override fun hasMark(cx: Int, cy: Int, mark: LevelMark, dir: Int): Boolean {
        return marks[mark]?.get(getCoordId(cx, cy)) == dir && isValid(cx, cy)
    }

    override fun setMarks(cx: Int, cy: Int, marks: List<LevelMark>) {
        marks.forEach {
            setMark(cx, cy, it)
        }
    }

    override fun setMark(cx: Int, cy: Int, mark: LevelMark, dir: Int) {
        if (isValid(cx, cy) && !hasMark(cx, cy, mark)) {
            if (!marks.contains(mark)) {
                marks[mark] = mutableMapOf()
            }

            marks[mark]?.set(getCoordId(cx, cy), dir.clamp(-1, 1))
        }
    }

    // set level marks at start of level creation to react to certain tiles
    private fun createLevelMarks() {
        for (cy in 0 until levelHeight) {
            for (cx in 0 until levelWidth) {
                // no collision at current pos or north but has collision south.
                if (!hasCollision(cx, cy) && hasCollision(cx, cy + 1) && !hasCollision(cx, cy - 1)) {
                    // if collision to the east of current pos and no collision to the northeast
                    if (hasCollision(cx + 1, cy) && !hasCollision(cx + 1, cy - 1)) {
                        setMark(cx, cy, LevelMark.SMALL_STEP, 1);
                    }

                    // if collision to the west of current pos and no collision to the northwest
                    if (hasCollision(cx - 1, cy) && !hasCollision(cx - 1, cy - 1)) {
                        setMark(cx, cy, LevelMark.SMALL_STEP, -1);
                    }
                }

                if (!hasCollision(cx, cy) && hasCollision(cx, cy + 1)) {
                    if (hasCollision(cx + 1, cy) ||
                        (!hasCollision(cx + 1, cy + 1) && !hasCollision(cx + 1, cy + 2))
                    ) {
                        setMarks(cx, cy, listOf(LevelMark.PLATFORM_END, LevelMark.PLATFORM_END_RIGHT))
                    }
                    if (hasCollision(cx - 1, cy) ||
                        (!hasCollision(cx - 1, cy + 1) && !hasCollision(cx - 1, cy + 2))
                    ) {
                        setMarks(cx, cy, listOf(LevelMark.PLATFORM_END, LevelMark.PLATFORM_END_LEFT))
                    }
                }
            }
        }
    }

}

enum class LevelMark {
    PLATFORM_END,
    PLATFORM_END_RIGHT,
    PLATFORM_END_LEFT,
    SMALL_STEP
}

sealed class SleepState(val time: TimeSpan) {
//    object VeryLightSleep : SleepState(0.milliseconds)
//    object LightSleep : SleepState(60.seconds)
//    object MediumSleep : SleepState(120.seconds)
//    object DeeperSleep : SleepState(180.seconds)
//    object EvenDeeperSleep : SleepState(240.seconds)
//    object DeepestSleep : SleepState(300.seconds)

    // debug
    object VeryLightSleep : SleepState(0.milliseconds)
    object LightSleep : SleepState(10.seconds)
    object MediumSleep : SleepState(20.seconds)
    object DeeperSleep : SleepState(30.seconds)
    object EvenDeeperSleep : SleepState(40.seconds)
    object DeepestSleep : SleepState(0.seconds)
}