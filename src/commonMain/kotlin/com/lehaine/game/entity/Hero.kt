package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.stateMachine
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
    }

    private sealed class HeroState {
        object Idle : HeroState()
        object Run : HeroState()
        object Jump : HeroState()
        object DoubleJump : HeroState()
        object Fall : HeroState()
        object Sleep : HeroState()
    }

    private val moveSpeed = 0.03
    private val moveFriction = 0.8
    private val jumpHeight = -0.5
    private val jumpExtraSpeed = 0.04

    private val runningLeft get() = input.keys.pressing(Key.A)
    private val runningRight get() = input.keys.pressing(Key.D)
    private val running get() = runningLeft || runningRight
    private val jumping get() = input.keys.justPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)
    private val jumpingExtra get() = input.keys.justPressed(Key.SPACE) && cd.has(JUMP_EXTRA)

    private val fsm = stateMachine<HeroState>(HeroState.Sleep) {
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
            transition {
                when {
                    jumping -> HeroState.Jump
                    running -> HeroState.Run
                    else -> HeroState.Idle
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.heroIdle)
            }
            update {
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