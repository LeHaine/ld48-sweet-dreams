package com.lehaine.game

import GameModule
import com.soywiz.korev.Key
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korio.async.launchImmediately

class EndScene() : Scene() {

    override suspend fun Container.sceneInit() {
        solidRect(GameModule.size.width, GameModule.size.height, Colors.BLACK)
        val t1 = text("If you got this far then thank you for playing the game all the way through! You did it!") {
            font = Assets.pixelFont
            fontSize = 8.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            horizontalAlign = HorizontalAlign.CENTER
            centerOnStage()
        }
        val t2 = text("You can play again if you so choose by pressing 'Shift-R' on this screen!") {
            font = Assets.pixelFont
            fontSize = 8.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            horizontalAlign = HorizontalAlign.CENTER
            centerOnStage()
        }.alignTopToBottomOf(t1, 5)

       val t3 = text("I sincerely hope you enjoyed it. Even a little bit.") {
            font = Assets.pixelFont
            fontSize = 8.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            centerOnStage()
        }.alignTopToBottomOf(t2, 5)

        text("- LeHaine") {
            font = Assets.pixelFont
            fontSize = 8.0
            alignment = TextAlignment.CENTER
            verticalAlign = VerticalAlign.MIDDLE
            centerOnStage()
        }.alignTopToBottomOf(t3, 5)

       addUpdater {
           if(views.input.keys.pressing(Key.LEFT_SHIFT) && views.input.keys.justPressed(Key.R)) {
               launchImmediately { sceneContainer.changeTo<LevelScene>() }
           }
       }
    }
}