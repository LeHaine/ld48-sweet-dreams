package com.lehaine.game

import com.lehaine.kiwi.component.Entity
import com.lehaine.kiwi.korge.view.CameraContainer
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.SpriteAnimation

fun CameraContainer.follow(entity: Entity, setImmediately: Boolean = false) {
    follow(entity.container, setImmediately)
}

val SpriteAnimation.duration get() = (size * defaultTimePerFrame.milliseconds).milliseconds