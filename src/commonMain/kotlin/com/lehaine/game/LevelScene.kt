package com.lehaine.game

import GameModule
import com.lehaine.game.entity.Hero
import com.lehaine.game.entity.enemySpawner
import com.lehaine.game.entity.hero
import com.lehaine.kiwi.korge.cd
import com.lehaine.kiwi.korge.getByPrefix
import com.lehaine.kiwi.korge.view.cameraContainer
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.korge.view.ldtk.ldtkMapView
import com.lehaine.kiwi.korge.view.ldtk.toLDtkLevel
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.kmem.umod
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.Rectangle
import kotlin.math.floor


class LevelScene(private val world: World, private val levelIdx: Int = 0) : Scene() {

    override suspend fun Container.sceneInit() {
        val worldLevel = world.allLevels[levelIdx]
        val ldtkLevel = worldLevel.toLDtkLevel()
        val gameLevel = GameLevel(worldLevel)

        lateinit var fx: Fx
        lateinit var hero: Hero

        val cam = cameraContainer(
            GameModule.size.width.toDouble(), GameModule.size.height.toDouble(),
            deadZone = 10,
            viewBounds = Rectangle(0, 0, worldLevel.pxWidth, worldLevel.pxHeight),
            clampToViewBounds = true,
            clip = true,
            simpleCull = false
        ) {
            ldtkMapView(ldtkLevel)
            worldLevel.layerEntities.allSpawners.forEach {
                gameLevel.spawnPoints += it
            }

            container EntityContainer@{
                name = "EntityContainer"

                // instantiate and add entities to game level list here
                worldLevel.layerEntities.allText.fastForEach {
                    container {
                        text(it.text ?: "").apply {
                            font = Assets.pixelFont
                            fontSize = 6.0
                            alignment = TextAlignment.CENTER
                        }
                        x = it.pixelX.toDouble()
                        y = it.pixelY.toDouble()
                    }
                }

                hero = hero(worldLevel.layerEntities.allHero[0], gameLevel).also { gameLevel._hero = it }
            }

            val particleContainer = fastSpriteContainer(useRotation = true, smoothing = false)
            fx = Fx(gameLevel, particleContainer).also { gameLevel._fx = it }

        }.apply {
            cameraZoom = 1.3
            // follow newly created entity or do something with camera
            follow(hero)
        }.also {
            gameLevel._camera = it
        }

        val warningText = text("Face your nightmare! \nThey are coming! MOVE!") {
            font = Assets.pixelFont
            fontSize = 16.0
            alignment = TextAlignment.CENTER
            centerOnStage()
            alignTopToTopOf(this@sceneInit, 15)
            cd("WARNING", 10.seconds) {
                visible = false
            }
        }

        // overlay
        val overlay =
            solidRect(
                GameModule.size.width.toDouble(),
                GameModule.size.height.toDouble(),
                Colors["#221e3d1b"]
            )
        val timerText = text("0:00") {
            font = Assets.pixelFont
            fontSize = 12.0
            x = 3.0
            y = 3.0
        }
        lateinit var slingshotCDCover: SolidRect
        lateinit var slingshotCDText: Text
        container {
            val slingshotIcon = enhancedSprite(Assets.tiles.getByPrefix("slingshotIcon"), smoothing = false) {
                alpha(0.65)
            }
            slingshotCDCover =
                solidRect(slingshotIcon.width, slingshotIcon.height, Colors["#151213d0"]) {
                    alignTopToTopOf(slingshotIcon)
                    alignLeftToLeftOf(slingshotIcon)
                    visible = false
                }
            slingshotCDText = text("0") {
                font = Assets.pixelFont
                fontSize = 12.0
                alignment = TextAlignment.CENTER
                verticalAlign = VerticalAlign.MIDDLE
                horizontalAlign = HorizontalAlign.CENTER
                y -= 5
                x -= 2
                setTextBounds(Rectangle(0.0, 0.0, this@container.width, this@container.height))
                visible = false
            }
        }.alignBottomToBottomOf(this, 3.0)
            .alignLeftToLeftOf(this, 3.0)

        text("F") {
            font = Assets.pixelFont
            fontSize = 8.0
            x = 7.0
        }.alignBottomToTopOf(slingshotCDCover, 1)

        val sleepState = text("Very Light Sleep") {
            font = Assets.pixelFont
            fontSize = 8.0
            alignment = TextAlignment.CENTER
            y = 4.0
        }.alignRightToRightOf(this, 3.0)

        enhancedSprite(smoothing = false) {
            playAnimationLooped(Assets.sleepIcon)
            alignRightToLeftOf(sleepState, 3.0)
        }

        enemySpawner(gameLevel)
        var timer = TimeSpan.ZERO

        var showDeathScreen = false
        var transitionToEndScene = false
        val transitionOut = solidRect(GameModule.size.width, GameModule.size.height, Colors["#00000000"])

        var newOverlayAlpha = 0.1
        var originalOverlayAlpha = 0.1

        addUpdater { dt ->
            val tmod = if (dt == 0.milliseconds) 0.0 else (dt / 16.666666.milliseconds)
            if (gameLevel.gameFinshed && !transitionToEndScene && !cd.has("GAME_DONE_CD")) {
                cd("GAME_DONE_CD", 3.seconds) {
                    transitionToEndScene = true
                }
            }
            if (cd.has("GAME_DONE_CD")) {
                transitionOut.color = Colors["#00000000"].withAd(1 - cd.ratio("GAME_DONE_CD"))
            }
            if (transitionToEndScene) {
                launchImmediately { sceneContainer.changeTo<EndScene>() }
                return@addUpdater
            }
            if (originalOverlayAlpha != newOverlayAlpha && !cd.has("OVERLAY_TRANSITION")) {
                cd("OVERLAY_TRANSITION", 5.seconds) {
                    originalOverlayAlpha = newOverlayAlpha
                }
            }
            if (cd.has("OVERLAY_TRANSITION")) {
                val diff = newOverlayAlpha - originalOverlayAlpha
                overlay.color = RGBA(overlay.color).withAd(cd.ratio("OVERLAY_TRANSITION") * diff + originalOverlayAlpha)
            }
            if (!hero.isDead) {
                timer += dt
                val minutes = floor((timer.seconds / 60) umod 60.0).toInt()
                val seconds = floor(timer.seconds umod 60.0).toInt()
                timerText.text =
                    "${minutes}:${seconds.toString().padStart(2, '0')}"

                sleepState.text = when {
                    timer >= SleepState.DeepestSleep.time -> {
                        if (gameLevel.sleepState != SleepState.DeepestSleep) {
                            warningText.apply {
                                text = "Entering REM - the deepest sleep stage.\nFace your nightmare!"
                                visible = true
                                cd("WARNING", 5.seconds) {
                                    visible = false

                                }
                            }
                        }
                        gameLevel.sleepState = SleepState.DeepestSleep
                        newOverlayAlpha = 0.44
                        "Deepest SLEEP!"
                    }
                    timer >= SleepState.EvenDeeperSleep.time -> {
                        if (gameLevel.sleepState != SleepState.LightSleep) {
                            warningText.apply {
                                text = "Entering an even deeper sleep state"
                                visible = true
                                cd("WARNING", 5.seconds) {
                                    visible = false
                                }
                            }
                        }
                        gameLevel.sleepState = SleepState.EvenDeeperSleep
                        newOverlayAlpha = 0.35
                        "Even Deeper Sleep"
                    }
                    timer >= SleepState.DeeperSleep.time -> {
                        if (gameLevel.sleepState != SleepState.LightSleep) {
                            warningText.apply {
                                text = "Entering a deeper sleep state"
                                visible = true
                                cd("WARNING", 5.seconds) {
                                    visible = false

                                }
                            }
                        }
                        gameLevel.sleepState = SleepState.DeeperSleep
                        newOverlayAlpha = 0.3
                        "Deeper Sleep"
                    }
                    timer >= SleepState.MediumSleep.time -> {
                        if (gameLevel.sleepState != SleepState.LightSleep) {
                            warningText.apply {
                                text = "Entering a medium sleep state"
                                visible = true
                                cd("WARNING", 5.seconds) {
                                    visible = false

                                }
                            }
                        }
                        gameLevel.sleepState = SleepState.MediumSleep
                        newOverlayAlpha = 0.2
                        "Medium Sleep"
                    }
                    timer >= SleepState.LightSleep.time -> {
                        if (gameLevel.sleepState != SleepState.LightSleep) {
                            warningText.apply {
                                text = "Entering a light sleep state"
                                visible = true
                                cd("WARNING", 5.seconds) {
                                    visible = false

                                }
                            }
                        }
                        gameLevel.sleepState = SleepState.LightSleep
                        newOverlayAlpha = 0.15
                        "Light Sleep"
                    }
                    else -> {
                        gameLevel.sleepState = SleepState.VeryLightSleep
                        newOverlayAlpha = 0.1
                        "Very Light Sleep"
                    }
                } + "\n(${((timer.seconds / SleepState.DeepestSleep.time.seconds) * 100).toIntFloor()}%)"
                if (gameLevel.slingShotCDRemaining > 0.milliseconds) {
                    gameLevel.slingShotCDRemaining -= dt
                    slingshotCDText.text = gameLevel.slingShotCDRemaining.seconds.toIntCeil().toString()
                    slingshotCDCover.visible = true
                    slingshotCDText.visible = true
                } else {
                    slingshotCDCover.visible = false
                    slingshotCDText.visible = false
                }
            }

            fx.update(dt)
            gameLevel.entities.fastForEach {
                it.tmod = tmod
                it.update(dt)
            }
            gameLevel.entities.fastForEach {
                it.postUpdate(dt)
            }

            if (hero.isDead && !showDeathScreen) {
                showDeathScreen = true
                overlay.color = Colors["#000000cc"]
                container {
                    centerOnStage()
                    val t1 = text("Your nightmare consumed you!") {
                        font = Assets.pixelFont
                        fontSize = 16.0
                        alignment = TextAlignment.CENTER

                    }
                    text("Press 'ALT+R' to restart!") {
                        font = Assets.pixelFont
                        fontSize = 16.0
                        alignment = TextAlignment.CENTER

                    }.alignTopToBottomOf(t1, 10)
                        .centerOn(this)
                }
            }

            if (views.input.keys.pressing(Key.LEFT_ALT) && views.input.keys.justPressed(Key.R)) {
                launchImmediately { sceneContainer.changeTo<LevelScene>() }
            }
        }

        keys {
//            down(Key.ESCAPE) {
//                stage?.views?.debugViews = false
//                stage?.gameWindow?.run {
//                    debug = false
//                    close()
//                }
//            }

            down(Key.M) {
                Assets.Sfx.musicChannel.togglePaused()
            }
//            down(Key.PAGE_UP) {
//                cam.cameraZoom += 0.1
//            }
//            down(Key.PAGE_DOWN) {
//                cam.cameraZoom -= 0.1
//            }
        }
    }
}