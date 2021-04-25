package com.lehaine.game

import GameModule
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korio.async.launchImmediately

class StartScene() : Scene() {

    override suspend fun Container.sceneInit() {
        solidRect(GameModule.size.width, GameModule.size.height, Colors.BLACK)
        val t1 = text("A game made in 48 hours for Ludum Dare 48 by LeHaine") {
            font = Assets.pixelFont
            fontSize = 12.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            horizontalAlign = HorizontalAlign.CENTER
            centerOnStage()
        }
        text("Press any key or click to continue") {
            font = Assets.pixelFont
            fontSize = 12.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            centerOnStage()
        }.alignTopToBottomOf(t1, 10)

        mouse {
            onClick {
                launchImmediately { sceneContainer.changeTo<LevelScene>() }
            }

        }

        keys {
            down {
                launchImmediately { sceneContainer.changeTo<LevelScene>() }
            }
        }
    }
}