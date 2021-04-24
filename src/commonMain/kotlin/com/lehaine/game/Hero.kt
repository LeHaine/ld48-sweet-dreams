package com.lehaine.game

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
    }

    private sealed class HeroState {
        object Idle : HeroState()
        object Run : HeroState()
        object Jump : HeroState()
        object DoubleJump : HeroState()
    }

    private val moveSpeed = 0.05
    private val moveFriction = 0.8
    private val jumpHeight = -0.35

    private val runningLeft get() = input.keys.pressing(Key.A)
    private val runningRight get() = input.keys.pressing(Key.D)
    private val running get() = runningLeft || runningRight
    private val jumping get() = input.keys.justPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)

    private val fsm = stateMachine<HeroState>(HeroState.Idle) {
        state(HeroState.Jump) {
            update {


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

            update {
                run()
            }
        }
    }

    init {
        scaleX = 4.0
        scaleY = 4.0
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

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}