package com.lehaine.game

import com.soywiz.korau.sound.PlaybackTimes
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.playbackTimes
import com.soywiz.korau.sound.readSound
import com.soywiz.korge.view.Views
import com.soywiz.korim.atlas.Atlas
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.TtfFont
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs

object Assets {

    lateinit var tiles: Atlas

    lateinit var pixelFont: Font

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

        Sfx.init(views)
    }
}