package com.lehaine.game

sealed class GameInput {
    object Horizontal : GameInput()
    object Dodge : GameInput()
    object Jump : GameInput()
    object SlingShot : GameInput()
}