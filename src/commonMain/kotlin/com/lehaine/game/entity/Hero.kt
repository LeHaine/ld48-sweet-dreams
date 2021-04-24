package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.game.component.HealthComponent
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.stateMachine
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Key
import com.soywiz.korge.view.Container
import com.soywiz.korui.UiContainer
import kotlin.math.pow

fun Container.hero(
    data: World.EntityHero,
    level: GenericGameLevelComponent<LevelMark>,
    callback: Hero.() -> Unit = {}
): Hero = Hero(
    level = level,
    platformerDynamicComponent = PlatformerDynamicComponentDefault(
        levelComponent = level,
        cx = data.cx,
        cy = data.cy,
        xr = data.pivotX.toDouble(),
        yr = data.pivotY.toDouble(),
        anchorX = data.pivotX.toDouble(),
        anchorY = data.pivotY.toDouble(),
        gridCellSize = GRID_SIZE
    ),
    spriteComponent = SpriteComponent(
        anchorX = data.pivotX.toDouble(),
        anchorY = data.pivotY.toDouble()
    )
).addTo(this).addToLevel().also(callback)


class Hero(
    level: GenericGameLevelComponent<LevelMark>,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
) :
    GameEntity(level, spriteComponent, platformerDynamicComponent),
    SpriteComponent by spriteComponent,
    PlatformerDynamicComponent by platformerDynamicComponent {

    companion object {
        const val ON_GROUND_RECENTLY = "onGroundRecently"
        const val JUMP_EXTRA = "jumpExtra"
        const val ANIM_PLAYING = "animPlaying"
        const val ATTACK_CD = "attackCD"
    }

    private sealed class HeroState {
        object Idle : HeroState()
        object Run : HeroState()
        object Jump : HeroState()
        object DoubleJump : HeroState()
        object Fall : HeroState()
        object Sleep : HeroState()
        object BroomAttack1 : HeroState()
    }

    private val moveSpeed = 0.04
    private val moveFriction = 0.8
    private val jumpHeight = -0.5
    private val jumpExtraSpeed = 0.04

    private var canSwing = true
    private val swinging get() = input.mouseButtons == 1 && canSwing

    private val runningLeft get() = input.keys.pressing(Key.A)
    private val runningRight get() = input.keys.pressing(Key.D)
    private val running get() = runningLeft || runningRight

    private val jumping get() = input.keys.justPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)
    private val jumpingExtra get() = input.keys.justPressed(Key.SPACE) && cd.has(JUMP_EXTRA)

    private var broomCombo = 0

    private val fsm = stateMachine<HeroState>(HeroState.Sleep) {
        state(HeroState.BroomAttack1) {
            transition {
                when {
                    cd.has(ATTACK_CD) -> HeroState.BroomAttack1
                    else -> HeroState.Idle
                }

            }
            begin {
                canSwing = false
                sprite.playOverlap(Assets.heroBroomAttack1)
                cd(ATTACK_CD, 300.milliseconds)
                cd(ANIM_PLAYING, Assets.heroBroomAttack1.duration)
                level.entities.fastForEach {
                    if (it != this@Hero
                        && dirTo(it) == dir
                        && distGridTo(it) <= 2.5
                        && it is HealthComponent
                    ) {
                        it.damage(25, -dirTo(it))
                    }
                }

            }
        }
        state(HeroState.Sleep) {
            transition {
                when {
                    input.keys.justPressed(Key.SPACE) -> HeroState.Idle
                    else -> HeroState.Sleep
                }

            }
            begin {
                sprite.playAnimationLooped(Assets.heroSleep)
            }
        }
        state(HeroState.Fall) {
            transition {
                when {
                    onGround -> HeroState.Idle
                    else -> HeroState.Fall
                }
            }
            update {
                run()
            }
        }

        state(HeroState.Jump) {
            transition {
                when {
                    velocityY > 0 -> HeroState.Fall
                    else -> HeroState.Jump
                }
            }
            begin {
                if (jumping) {
                    jump()
                }
            }
            update {
                if (jumpingExtra) {
                    jumpExtra()
                }
                run()
            }

        }
        state(HeroState.Run) {
            transition {
                when {
                    jumping -> HeroState.Jump
                    swinging && broomCombo == 0 -> HeroState.BroomAttack1
                    running -> HeroState.Run
                    else -> HeroState.Idle
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.heroRun)
            }
            update { run() }
        }
        state(HeroState.Idle) {
            var playingAnim = false
            transition {
                when {
                    jumping -> HeroState.Jump
                    running -> HeroState.Run
                    swinging && broomCombo == 0 -> HeroState.BroomAttack1
                    else -> HeroState.Idle
                }
            }
            begin {
                playingAnim = false
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.heroIdle)
                    playingAnim = true
                }
                run()
            }
        }
    }

    init {
        enableCollisionChecks = true
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (onGround) {
            cd(ON_GROUND_RECENTLY, 150.milliseconds)
        }

        if (input.mouseButtons == 0) {
            canSwing = true
        }

        fsm.update(dt)
    }

    private fun run() {
        if (running) {
            val speed = if (runningRight) moveSpeed else -moveSpeed
            dir = if (runningRight) 1 else -1
            velocityX += speed * tmod
        } else {
            velocityX *= moveFriction.pow(tmod)
        }
    }

    private fun jump() {
        velocityY = jumpHeight
        stretchX = 0.7
        cd(JUMP_EXTRA, 100.milliseconds)
    }

    private fun jumpExtra() {
        velocityY -= jumpExtraSpeed * tmod
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}