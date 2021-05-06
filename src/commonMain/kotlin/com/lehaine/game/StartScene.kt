package com.lehaine.game

import GameModule
import com.lehaine.kiwi.korge.getByPrefix
import com.soywiz.klock.seconds
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
        solidRect(GameModule.size.width, GameModule.size.height, Colors["#222034"])
        val logo = image(Assets.tiles.getByPrefix("logo")) {
            smoothing = false
            centerOnStage()

        }
        val t1 = text("A game made in 48 hours for Ludum Dare 48 by LeHaine") {
            font = Assets.pixelFont
            fontSize = 12.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            horizontalAlign = HorizontalAlign.CENTER
            centerOnStage()
            alignTopToBottomOf(logo, 5)
        }
        text("Press any key or click to continue") {
            font = Assets.pixelFont
            fontSize = 12.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            centerOnStage()
        }.alignTopToBottomOf(t1, 5)

        var launched = false
        mouse {
            onClick {
                if (!launched) {
                    launched = true
                    Assets.Sfx.playMusic()
                    launchImmediately { sceneContainer.changeTo<Game>() }
                }
            }
        }

        keys {
            down {
                if (!launched) {
                    launched = true
                    Assets.Sfx.playMusic()
                    launchImmediately { sceneContainer.changeTo<Game>() }
                }
            }
        }
    }
}