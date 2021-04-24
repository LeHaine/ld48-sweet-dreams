package com.lehaine.game

import com.soywiz.klock.milliseconds
import com.soywiz.korau.sound.PlaybackTimes
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.playbackTimes
import com.soywiz.korau.sound.readSound
import com.soywiz.korge.view.SpriteAnimation
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.getSpriteAnimation
import com.soywiz.korim.atlas.Atlas
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.TtfFont
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs

object Assets {

    lateinit var tiles: Atlas

    lateinit var pixelFont: Font

    lateinit var heroIdle: SpriteAnimation
    lateinit var heroRun: SpriteAnimation
    lateinit var heroSleep: SpriteAnimation
    lateinit var heroBroomAttack1: SpriteAnimation

    lateinit var longArmIdle: SpriteAnimation
    lateinit var longArmSwing: SpriteAnimation

    object Sfx {
        lateinit var views: Views

        suspend fun init(views: Views) {
            this.views = views
            // define sounds here
        }

        private suspend fun loadSoundsByPrefix(
            prefix: String,
            total: Int = 1,
            dir: String = "sfx/",
            fileType: String = ".wav"
        ): List<Sound> {
            val sounds = mutableListOf<Sound>()
            for (i in 0 until total) {
                sounds += resourcesVfs["$dir$prefix$i$fileType"].readSound()
            }
            return sounds.toList()
        }

        private suspend fun loadSound(file: String) = resourcesVfs[file].readSound()

        fun play(sound: Sound, times: PlaybackTimes = 1.playbackTimes) {
            views.launch {
                sound.play(times)
            }
        }
    }

    suspend fun init(views: Views) {
        tiles = resourcesVfs["tiles.atlas.json"].readAtlas()
        pixelFont = TtfFont(resourcesVfs["m5x7.ttf"].readAll())

        // define animations and other assets here
        heroIdle = tiles.getSpriteAnimation("heroIdle", 500.milliseconds)
        heroRun = tiles.getSpriteAnimation("heroRun", 100.milliseconds)
        heroSleep = tiles.getSpriteAnimation("heroSleep", 500.milliseconds)
        heroBroomAttack1 = tiles.getSpriteAnimation("heroBroomAttack1", 100.milliseconds)

        longArmIdle = tiles.getSpriteAnimation("longArmIdle", 500.milliseconds)
        longArmSwing = tiles.getSpriteAnimation("longArmSwing", 100.milliseconds)

        Sfx.init(views)
    }
}