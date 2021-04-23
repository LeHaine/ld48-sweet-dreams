package com.lehaine.game

import com.lehaine.kiwi.component.Entity
import com.lehaine.kiwi.korge.view.CameraContainer

fun CameraContainer.follow(entity: Entity, setImmediately: Boolean = false) {
    follow(entity.container, setImmediately)
}