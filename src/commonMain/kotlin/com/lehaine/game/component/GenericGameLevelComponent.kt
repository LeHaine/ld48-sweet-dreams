package com.lehaine.game.component

import com.lehaine.kiwi.component.LevelComponent

/**
 * Add any extra references to this LevelComponent such as Hero reference for easier access in other entities.
 */
interface GenericGameLevelComponent<LevelMark> : LevelComponent<LevelMark> {
    val levelWidth: Int
    val levelHeight: Int
}