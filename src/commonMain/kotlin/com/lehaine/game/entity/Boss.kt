package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.component.*
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.stateMachine
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.anchor
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.boss(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: Boss.() -> Unit = {}
): Boss = Boss(
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
    healthComponent = HealthComponentDefault(25),
    dangerousComponent = DangerousComponentDefault(5)
).addTo(this).addToLevel().also(callback)

class Boss(
    level: GenericGameLevelComponent<LevelMark>,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
    private val targetComponent: TargetComponent,
    private val healthComponent: HealthComponent,
    private val dangerousComponent: DangerousComponent
) :
    GameEntity(level, spriteComponent, platformerDynamicComponent),
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

    private sealed class BossState {
        object Idle : BossState()
        object Attack : BossState()
        object MovingToHero : BossState()

        object NoAffects : BossState()
        object Stunned : BossState()
    }


    private val affectIcon = container.run {
        enhancedSprite {
            smoothing = false
            y -= 15
            anchor(Anchor.MIDDLE_CENTER)
            visible = false
        }
    }
    private val moveSpeed = 0.04

    private val attackingHero get() = distGridTo(level.hero) <= 3 && !cd.has(ATTACK_CD)

    private val entityFSM = stateMachine<BossState>(BossState.NoAffects) {
        state(BossState.Stunned) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> BossState.Stunned
                    else -> BossState.NoAffects
                }
            }
            begin {
                affectIcon.playAnimationLooped(Assets.stunIcon)
                affectIcon.visible = true
                sprite.playAnimationLooped(Assets.bossIdle)
            }
        }

        state(BossState.NoAffects) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> BossState.Stunned
                    else -> BossState.NoAffects
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

    private val controlFSM = stateMachine<BossState>(BossState.Idle) {
        state(BossState.MovingToHero) {
            transition {
                when {
                    attackingHero -> BossState.Attack
                    else -> BossState.MovingToHero
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.bossRun)
            }
            update {
                moveTo(platformerDynamicComponent, spriteComponent, level.hero.cx, level.hero.cy, moveSpeed * tmod)
            }

        }
        state(BossState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> BossState.Attack
                    else -> BossState.Idle
                }
            }
            begin {
                dir = dirTo(level.hero)
                sprite.playOverlap(Assets.bossAttack, onAnimationFrameChange = {
                    if (it in 3..8) {
                        attemptToAttackHero()
                    }
                })
                cd(ANIM_PLAYING, Assets.bossAttack.duration)
                cd(ATTACK_CD, 3.seconds)
            }

        }
        state(BossState.Idle) {
            var playingAnim = false
            transition {
                when {
                    cd.has(IDLE) -> BossState.Idle
                    attackingHero -> BossState.Attack
                    else -> BossState.MovingToHero
                }
            }
            begin {
                playingAnim = false
                cd(IDLE, (500..1500).random().milliseconds)
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.bossIdle)
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
        healthComponent.damage(amount, fromDir)
        stretchX = 0.6
    }

    private fun attemptToAttackHero() {
        if (distGridTo(level.hero) <= 3.5 && dir == dirTo(level.hero)) {
            attack(level.hero, -dirTo(level.hero))
        }
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}