package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.random
import com.lehaine.kiwi.stateMachine
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.anchor
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.longArm(
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
    targetComponent = TargetComponentDefault(),
    healthComponent = HealthComponentDefault(100),
    dangerousComponent = DangerousComponentDefault(35)
).addTo(this).addToLevel().also(callback)

class LongArm(
    level: GenericGameLevelComponent<LevelMark>,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
    private val targetComponent: TargetComponent,
    private val healthComponent: HealthComponent,
    private val dangerousComponent: DangerousComponent
) :
    GameEntity(level, spriteComponent, platformerDynamicComponent),
    MobComponent,
    SpriteComponent by spriteComponent,
    PlatformerDynamicComponent by platformerDynamicComponent,
    TargetComponent by targetComponent,
    HealthComponent by healthComponent,
    DangerousComponent by dangerousComponent {

    companion object {
        private const val ANIM_PLAYING = "animPlaying"
        private const val ATTACK_CD = "attackCD"
        private const val IDLE = "idle"
    }

    private sealed class LongArmState {
        object Idle : LongArmState()
        object Attack : LongArmState()
        object MovingToHero : LongArmState()

        object NoAffects : LongArmState()
        object Stunned : LongArmState()
    }


    private val affectIcon = container.run {
        enhancedSprite {
            smoothing = false
            y -= 15
            anchor(Anchor.MIDDLE_CENTER)
            visible = false
        }
    }
    private val moveSpeed = 0.015
    private val attackRange = 2.5

    private val attackingHero get() = distGridTo(level.hero) <= (0.0..attackRange).random() && !cd.has(ATTACK_CD)

    private val entityFSM = stateMachine<LongArmState>(LongArmState.NoAffects) {
        state(LongArmState.Stunned) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> LongArmState.Stunned
                    else -> LongArmState.NoAffects
                }
            }
            begin {
                affectIcon.playAnimationLooped(Assets.stunIcon)
                affectIcon.visible = true
                sprite.playAnimationLooped(Assets.longArmStunned)
            }
        }

        state(LongArmState.NoAffects) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> LongArmState.Stunned
                    else -> LongArmState.NoAffects
                }
            }
            begin {
                affectIcon.stopAnimation()
                affectIcon.visible = false
            }

            update {
                controlFSM.update(it)
            }
        }

    }

    private val controlFSM = stateMachine<LongArmState>(LongArmState.Idle) {
        state(LongArmState.MovingToHero) {
            transition {
                when {
                    attackingHero -> LongArmState.Attack
                    else -> LongArmState.MovingToHero
                }

            }
            begin {
                sprite.playAnimationLooped(Assets.longArmWalk)
            }
            update {
                moveTo(platformerDynamicComponent, spriteComponent, level.hero.cx, level.hero.cy, moveSpeed * tmod)
            }

        }
        state(LongArmState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> LongArmState.Attack
                    else -> LongArmState.Idle
                }
            }
            begin {
                dir = dirTo(level.hero)
                sprite.playOverlap(Assets.longArmSwing, onAnimationFrameChange = {
                    if (it == 7) {
                        attemptToAttackHero()
                    }
                })
                cd(ANIM_PLAYING, Assets.longArmSwing.duration)
                cd(ATTACK_CD, 3.seconds)
            }

        }
        state(LongArmState.Idle) {
            var playingAnim = false
            transition {
                when {
                    cd.has(IDLE) -> LongArmState.Idle
                    attackingHero -> LongArmState.Attack
                    else -> LongArmState.MovingToHero
                }
            }
            begin {
                playingAnim = false
                cd(IDLE, (500..1500).random().milliseconds)
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.longArmIdle)
                    playingAnim = true
                }
            }
        }
    }

    init {
        enableCollisionChecks = true
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (isDead) {
            destroy()
        }

        entityFSM.update(dt)
    }

    override fun damage(amount: Int, fromDir: Int) {
        sfx.hit.playSfx()
        healthComponent.damage(amount, fromDir)
        stretchX = 0.6
        blink()
    }

    private fun attemptToAttackHero() {
        if (distGridTo(level.hero) <= attackRange && dir == dirTo(level.hero)) {
            attack(level.hero, -dirTo(level.hero))
        }
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }

}