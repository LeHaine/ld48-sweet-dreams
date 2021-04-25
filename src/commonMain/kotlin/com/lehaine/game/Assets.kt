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

    lateinit var bossIdle: SpriteAnimation
    lateinit var bossRun: SpriteAnimation
    lateinit var bossAttack: SpriteAnimation

    lateinit var heroIdle: SpriteAnimation
    lateinit var heroRun: SpriteAnimation
    lateinit var heroSleep: SpriteAnimation
    lateinit var heroBroomAttack1: SpriteAnimation
    lateinit var heroBroomAttack2: SpriteAnimation
    lateinit var heroBroomAttack3: SpriteAnimation
    lateinit var heroSlingShot: SpriteAnimation
    lateinit var heroRoll: SpriteAnimation
    lateinit var heroDie: SpriteAnimation

    lateinit var longArmIdle: SpriteAnimation
    lateinit var longArmSwing: SpriteAnimation
    lateinit var longArmStunned: SpriteAnimation
    lateinit var longArmWalk: SpriteAnimation

    lateinit var sheepIdle: SpriteAnimation
    lateinit var sheepAttack: SpriteAnimation
    lateinit var sheepStunned: SpriteAnimation
    lateinit var sheepWalk: SpriteAnimation

    lateinit var dustBunnyIdle: SpriteAnimation
    lateinit var dustBunnyJump: SpriteAnimation
    lateinit var dustBunnyAttack: SpriteAnimation

    lateinit var ghoulBob: SpriteAnimation
    lateinit var ghoulAttack: SpriteAnimation

    lateinit var stunIcon: SpriteAnimation
    lateinit var sleepIcon: SpriteAnimation

    object Sfx {
        lateinit var views: Views

        lateinit var hit: List<Sound>
        lateinit var swing: List<Sound>
        lateinit var strongSwing: List<Sound>
        lateinit var shoot: Sound
        lateinit var land: Sound
        lateinit var footstep: Sound
        lateinit var bossYell: Sound
        lateinit var ghoulAnticipation: Sound
        lateinit var ghoulSwing: Sound
        lateinit var teleport: Sound

        suspend fun init(views: Views) {
            this.views = views
            // define sounds here
            hit = loadSoundsByPrefix("hit", 3, volume = 0.6)
            swing = loadSoundsByPrefix("swing", 6, volume = 0.8)
            strongSwing = loadSoundsByPrefix("strongSwing", 4, volume = 0.8)
            bossYell = loadSound("bossYell0")
            footstep = loadSound("footstep0", volume = 0.4)
            land = loadSound("land0")
            shoot = loadSound("shoot0")
            teleport = loadSound("teleport0")
            ghoulAnticipation = loadSound("ghoulAnticipation0")
            ghoulSwing = loadSound("ghoulSwing0")
        }

        private suspend fun loadSoundsByPrefix(
            prefix: String,
            total: Int = 1,
            dir: String = "sfx/",
            fileType: String = ".wav",
            volume: Double = 1.0
        ): List<Sound> {
            val sounds = mutableListOf<Sound>()
            for (i in 0 until total) {
                sounds += resourcesVfs["$dir$prefix$i$fileType"].readSound().apply { this.volume = volume }
            }
            return sounds.toList()
        }

        private suspend fun loadSound(
            file: String,
            dir: String = "sfx/",
            fileType: String = ".wav",
            volume: Double = 1.0
        ) = resourcesVfs["$dir$file$fileType"].readSound().apply { this.volume = volume }

        fun play(sound: Sound, times: PlaybackTimes = 1.playbackTimes) {
            views.launch {
                sound.play(times)
            }
        }

        fun List<Sound>.playSfx(times: PlaybackTimes = 1.playbackTimes) {
            play(this.random(), times)
        }

        fun Sound.playSfx(times: PlaybackTimes = 1.playbackTimes) {
            play(this, times)
        }
    }

    suspend fun init(views: Views) {
        tiles = resourcesVfs["tiles.atlas.json"].readAtlas()
        pixelFont = TtfFont(resourcesVfs["m5x7.ttf"].readAll())

        bossIdle = tiles.getSpriteAnimation("bossIdle", 500.milliseconds)
        bossAttack = tiles.getSpriteAnimation("bossAttack", 150.milliseconds)
        bossRun = tiles.getSpriteAnimation("bossRun", 100.milliseconds)

        // define animations and other assets here
        heroIdle = tiles.getSpriteAnimation("heroIdle", 500.milliseconds)
        heroRun = tiles.getSpriteAnimation("heroRun", 100.milliseconds)
        heroSleep = tiles.getSpriteAnimation("heroSleep", 500.milliseconds)
        heroBroomAttack1 = tiles.getSpriteAnimation("heroBroomAttack1", 100.milliseconds)
        heroBroomAttack2 = tiles.getSpriteAnimation("heroBroomAttack2", 100.milliseconds)
        heroBroomAttack3 = tiles.getSpriteAnimation("heroBroomAttack3", 100.milliseconds)
        heroSlingShot = tiles.getSpriteAnimation("heroSlingShot", 100.milliseconds)
        heroRoll = tiles.getSpriteAnimation("heroRoll", 100.milliseconds)
        heroDie = tiles.getSpriteAnimation("heroDie", 100.milliseconds)

        longArmIdle = tiles.getSpriteAnimation("longArmIdle", 500.milliseconds)
        longArmSwing = tiles.getSpriteAnimation("longArmSwing", 100.milliseconds)
        longArmStunned = tiles.getSpriteAnimation("longArmStunned", 500.milliseconds)
        longArmWalk = tiles.getSpriteAnimation("longArmWalk", 100.milliseconds)

        dustBunnyIdle = tiles.getSpriteAnimation("dustBunnyIdle", 500.milliseconds)
        dustBunnyJump = tiles.getSpriteAnimation("dustBunnyJump", 100.milliseconds)
        dustBunnyAttack = tiles.getSpriteAnimation("dustBunnyAttack", 100.milliseconds)

        sheepIdle = tiles.getSpriteAnimation("sheepIdle", 500.milliseconds)
        sheepAttack = tiles.getSpriteAnimation("sheepAttack", 100.milliseconds)
        sheepStunned = tiles.getSpriteAnimation("sheepStunned", 500.milliseconds)
        sheepWalk = tiles.getSpriteAnimation("sheepWalk", 100.milliseconds)

        ghoulBob = tiles.getSpriteAnimation("ghoulBob", 500.milliseconds)
        ghoulAttack = tiles.getSpriteAnimation("ghoulAttack", 100.milliseconds)

        stunIcon = tiles.getSpriteAnimation("stunIcon", 200.milliseconds)
        sleepIcon = tiles.getSpriteAnimation("sleepIcon", 150.milliseconds)

        Sfx.init(views)
    }
}