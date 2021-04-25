package com.lehaine.game.component

import com.lehaine.game.Fx
import com.lehaine.game.SleepState
import com.lehaine.game.World
import com.lehaine.game.entity.Hero
import com.lehaine.kiwi.component.LevelComponent
import com.lehaine.kiwi.korge.view.CameraContainer
import com.soywiz.klock.TimeSpan

/**
 * Add any extra references to this LevelComponent such as Hero reference for easier access in other entities.
 */
interface GenericGameLevelComponent<LevelMark> : LevelComponent<LevelMark> {
    val fx: Fx
    val hero: Hero
    val camera: CameraContainer
    val spawnPoints: List<World.EntitySpawners>
    val levelWidth: Int
    val levelHeight: Int
    var slingShotCDRemaining:TimeSpan
    var sleepState: SleepState
    var gameFinshed:Boolean
}