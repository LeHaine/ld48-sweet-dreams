package com.lehaine.game.entity

import com.lehaine.game.Assets
import com.lehaine.game.GRID_SIZE
import com.lehaine.game.GameEntity
import com.lehaine.game.LevelMark
import com.lehaine.game.component.*
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.stateMachine
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import com.soywiz.korui.UiContainer

fun Container.longArm(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: LongArm.() -> Unit = {}
): LongArm = LongArm(
    level = level,
    platformerDynamicComponent = PlatformerDynamicComponentDefault(
        levelComponent = level,
        cx = cx,
        cy = cy,
        xr = 0.5,
        yr = 1.0,
        anchorX = 0.5,
        anchorY = 1.0,
        gridCellSize = GRID_SIZE
    ),
    spriteComponent = SpriteComponent(
        anchorX = 0.5,
        anchorY = 1.0,
    ),
    healthComponent = HealthComponentDefault(50),
    dangerousComponent = DangerousComponentDefault(10)
).addTo(this).addToLevel().also(callback)

class LongArm(
    level: GenericGameLevelComponent<LevelMark>,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
    private val healthComponent: HealthComponent,
    private val dangerousComponent: DangerousComponent
) :
    GameEntity(level, spriteComponent, platformerDynamicComponent),
    SpriteComponent by spriteComponent,
    PlatformerDynamicComponent by platformerDynamicComponent,
    HealthComponent by healthComponent,
    DangerousComponent by dangerousComponent {

    companion object {
        private const val ANIM_PLAYING = "animPlaying"
        private const val ATTACK_CD = "attackCD"
    }

    init {
        enableCollisionChecks = true
        sprite.playAnimationLooped(Assets.longArmIdle)
    }

    private sealed class LongArmState {
        object Idle : LongArmState()
        object Attack : LongArmState()
    }

    private val attackingHero get() = distGridTo(level.hero) <= 3 && !cd.has(ATTACK_CD)

    private val fsm = stateMachine<LongArmState>(LongArmState.Idle) {
        state(LongArmState.Attack) {
            var animPlaying = true
            transition {
                when {
                    animPlaying -> LongArmState.Attack
                    else -> LongArmState.Idle
                }
            }
            begin {
                animPlaying = true
                dir = dirTo(level.hero)
                sprite.playOverlap(Assets.longArmSwing, onAnimationFrameChange = {
                    if (it == 4) {
                        attemptToAttackHero()
                    }
                }) {
                    animPlaying = false
                }
                cd(ATTACK_CD, 3.seconds)
            }
        }
        state(LongArmState.Idle) {
            var playingAnim = false
            transition {
                when {
                    attackingHero -> LongArmState.Attack
                    else -> LongArmState.Idle
                }
            }
            begin {
                playingAnim = false
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.longArmIdle)
                    playingAnim = true
                }
            }
        }
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (isDead) {
            destroy()
        }

        fsm.update(dt)
    }

    override fun damage(amount: Int, fromDir: Int) {
        healthComponent.damage(amount, fromDir)
        stretchX = 0.6
    }

    private fun attemptToAttackHero() {
        if (distGridTo(level.hero) <= 2.5) {
            attack(level.hero, -dirTo(level.hero))
        }
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }

}