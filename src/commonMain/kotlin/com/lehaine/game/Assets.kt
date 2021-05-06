package com.lehaine.game

import com.lehaine.kiwi.korge.view.EnhancedSpriteAnimation
import com.lehaine.kiwi.korge.view.getEnhancedSpriteAnimation
import com.soywiz.klock.milliseconds
import com.soywiz.korau.sound.*
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

    lateinit var bossIdle: EnhancedSpriteAnimation
    lateinit var bossRun: EnhancedSpriteAnimation
    lateinit var bossAttack: EnhancedSpriteAnimation

    lateinit var heroIdle: EnhancedSpriteAnimation
    lateinit var heroRun: EnhancedSpriteAnimation
    lateinit var heroSleep: EnhancedSpriteAnimation
    lateinit var heroBroomAttack1: EnhancedSpriteAnimation
    lateinit var heroBroomAttack2: EnhancedSpriteAnimation
    lateinit var heroBroomAttack3: EnhancedSpriteAnimation
    lateinit var heroSlingShot: EnhancedSpriteAnimation
    lateinit var heroRoll: EnhancedSpriteAnimation
    lateinit var heroDie: EnhancedSpriteAnimation

    lateinit var longArmIdle: EnhancedSpriteAnimation
    lateinit var longArmSwing: EnhancedSpriteAnimation
    lateinit var longArmStunned: EnhancedSpriteAnimation
    lateinit var longArmWalk: EnhancedSpriteAnimation

    lateinit var sheepIdle: EnhancedSpriteAnimation
    lateinit var sheepAttack: EnhancedSpriteAnimation
    lateinit var sheepStunned: EnhancedSpriteAnimation
    lateinit var sheepWalk: EnhancedSpriteAnimation

    lateinit var dustBunnyIdle: EnhancedSpriteAnimation
    lateinit var dustBunnyJump: EnhancedSpriteAnimation
    lateinit var dustBunnyAttack: EnhancedSpriteAnimation

    lateinit var ghoulBob: EnhancedSpriteAnimation
    lateinit var ghoulAttack: EnhancedSpriteAnimation

    lateinit var stunIcon: EnhancedSpriteAnimation
    lateinit var sleepIcon: EnhancedSpriteAnimation

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
        lateinit var roll: Sound
        lateinit var longArmSwing: Sound

        lateinit var music: Sound
        lateinit var musicChannel: SoundChannel

        suspend fun init(views: Views) {
            this.views = views
            // define sounds here
            hit = loadSoundsByPrefix("hit", 3, volume = 0.6)
            swing = loadSoundsByPrefix("swing", 6, volume = 0.8)
            strongSwing = loadSoundsByPrefix("strongSwing", 4, volume = 0.8)
            bossYell = loadSound("bossYell0")
            footstep = loadSound("footstep0")
            land = loadSound("land0")
            shoot = loadSound("shoot0")
            teleport = loadSound("teleport0")
            ghoulAnticipation = loadSound("ghoulAnticipation0")
            ghoulSwing = loadSound("ghoulSwing0")
            roll = loadSound("roll0")
            longArmSwing = loadSound("longArmSwing0")

            // TODO reenable when KorGE 2.1 released
        //    music = resourcesVfs["sfx/music.mp3"].readMusic()
        }

        suspend fun playMusic() {
            // TODO reenable when KorGE 2.1 released
//            musicChannel = music.playForever()
//            musicChannel.volume = 0.1
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

        bossIdle = tiles.getEnhancedSpriteAnimation("bossIdle", 500.milliseconds)
        bossAttack = tiles.getEnhancedSpriteAnimation("bossAttack", 150.milliseconds)
        bossRun = tiles.getEnhancedSpriteAnimation("bossRun", 100.milliseconds)

        // define animations and other assets here
        heroIdle = tiles.getEnhancedSpriteAnimation("heroIdle", 500.milliseconds)
        heroRun = tiles.getEnhancedSpriteAnimation("heroRun", 100.milliseconds)
        heroSleep = tiles.getEnhancedSpriteAnimation("heroSleep", 500.milliseconds)
        heroBroomAttack1 = tiles.getEnhancedSpriteAnimation("heroBroomAttack1", 100.milliseconds)
        heroBroomAttack2 = tiles.getEnhancedSpriteAnimation("heroBroomAttack2", 100.milliseconds)
        heroBroomAttack3 = tiles.getEnhancedSpriteAnimation("heroBroomAttack3", 100.milliseconds)
        heroSlingShot = tiles.getEnhancedSpriteAnimation("heroSlingShot", 100.milliseconds)
        heroRoll = tiles.getEnhancedSpriteAnimation("heroRoll", 100.milliseconds)
        heroDie = tiles.getEnhancedSpriteAnimation("heroDie", 100.milliseconds)

        longArmIdle = tiles.getEnhancedSpriteAnimation("longArmIdle", 500.milliseconds)
        longArmSwing = tiles.getEnhancedSpriteAnimation("longArmSwing", 100.milliseconds)
        longArmStunned = tiles.getEnhancedSpriteAnimation("longArmStunned", 500.milliseconds)
        longArmWalk = tiles.getEnhancedSpriteAnimation("longArmWalk", 100.milliseconds)

        dustBunnyIdle = tiles.getEnhancedSpriteAnimation("dustBunnyIdle", 500.milliseconds)
        dustBunnyJump = tiles.getEnhancedSpriteAnimation("dustBunnyJump", 100.milliseconds)
        dustBunnyAttack = tiles.getEnhancedSpriteAnimation("dustBunnyAttack", 100.milliseconds)

        sheepIdle = tiles.getEnhancedSpriteAnimation("sheepIdle", 500.milliseconds)
        sheepAttack = tiles.getEnhancedSpriteAnimation("sheepAttack", 100.milliseconds)
        sheepStunned = tiles.getEnhancedSpriteAnimation("sheepStunned", 500.milliseconds)
        sheepWalk = tiles.getEnhancedSpriteAnimation("sheepWalk", 100.milliseconds)

        ghoulBob = tiles.getEnhancedSpriteAnimation("ghoulBob", 500.milliseconds)
        ghoulAttack = tiles.getEnhancedSpriteAnimation("ghoulAttack", 100.milliseconds)

        stunIcon = tiles.getEnhancedSpriteAnimation("stunIcon", 200.milliseconds)
        sleepIcon = tiles.getEnhancedSpriteAnimation("sleepIcon", 150.milliseconds)

        Sfx.init(views)
    }
}