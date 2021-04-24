package com.lehaine.game.entity

import com.lehaine.game.*
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

inline fun Container.sheep(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: Sheep.() -> Unit = {}
): Sheep = Sheep(
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
    healthComponent = HealthComponentDefault(30),
    dangerousComponent = DangerousComponentDefault(5)
).addTo(this).addToLevel().also(callback)

class Sheep(
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

    private sealed class SheepState {
        object Idle : SheepState()
        object Attack : SheepState()
        object MovingToHero : SheepState()

        object NoAffects : SheepState()
        object Stunned : SheepState()
    }


    private val affectIcon = container.run {
        enhancedSprite {
            smoothing = false
            y -= 15
            anchor(Anchor.MIDDLE_CENTER)
            visible = false
        }
    }
    private val moveSpeed = 0.02

    private val attackingHero get() = distGridTo(level.hero) <= 3 && !cd.has(ATTACK_CD)

    private val entityFSM = stateMachine<SheepState>(SheepState.NoAffects) {
        state(SheepState.Stunned) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> SheepState.Stunned
                    else -> SheepState.NoAffects
                }
            }
            begin {
                affectIcon.playAnimationLooped(Assets.stunIcon)
                affectIcon.visible = true
                sprite.playAnimationLooped(Assets.sheepStunned)
            }
        }

        state(SheepState.NoAffects) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> SheepState.Stunned
                    else -> SheepState.NoAffects
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

    private val controlFSM = stateMachine<SheepState>(SheepState.Idle) {
        state(SheepState.MovingToHero) {
            transition {
                when {
                    attackingHero -> SheepState.Attack
                    else -> SheepState.MovingToHero
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.sheepWalk)
            }
            update {
                moveTo(platformerDynamicComponent, spriteComponent, level.hero.cx, level.hero.cy, moveSpeed * tmod)
            }

        }
        state(SheepState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> SheepState.Attack
                    else -> SheepState.Idle
                }
            }
            begin {
                dir = dirTo(level.hero)
                sprite.playOverlap(Assets.sheepAttack, onAnimationFrameChange = {
                    if (it == 8) {
                        attemptToAttackHero()
                    }
                })
                cd(ANIM_PLAYING, Assets.sheepAttack.duration)
                cd(ATTACK_CD, 3.seconds)
            }

        }
        state(SheepState.Idle) {
            var playingAnim = false
            transition {
                when {
                    cd.has(IDLE) -> SheepState.Idle
                    attackingHero -> SheepState.Attack
                    else -> SheepState.MovingToHero
                }
            }
            begin {
                playingAnim = false
                cd(IDLE, (500..1500).random().milliseconds)
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.sheepIdle)
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
        blink()
    }

    private fun attemptToAttackHero() {
        if (distGridTo(level.hero) <= (1.5..2.5).random() && dir == dirTo(level.hero)) {
            attack(level.hero, -dirTo(level.hero))
        }
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }

}