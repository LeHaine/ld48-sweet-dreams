package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.component.ext.toGridPosition
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.stateMachine
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.anchor
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.ghoul(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: Ghoul.() -> Unit = {}
): Ghoul = Ghoul(
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
    healthComponent = HealthComponentDefault(75),
    dangerousComponent = DangerousComponentDefault(50)
).addTo(this).addToLevel().also(callback)

class Ghoul(
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
        private const val TELEPORT_CD = "teleportCD"
    }

    private sealed class GhoulState {
        object Idle : GhoulState()
        object Attack : GhoulState()
        object MovingToHero : GhoulState()
        object Teleport : GhoulState()

        object NoAffects : GhoulState()
        object Stunned : GhoulState()
    }


    private val affectIcon = container.run {
        enhancedSprite {
            smoothing = false
            y -= 15
            anchor(Anchor.MIDDLE_CENTER)
            visible = false
        }
    }
    private val moveSpeed = 0.01
    private val attackRange = 1.5

    private val attackingHero get() = distGridTo(level.hero) <= attackRange && !cd.has(ATTACK_CD)
    private val teleporting get() = distGridTo(level.hero) <= 10 && !cd.has(TELEPORT_CD)

    private val entityFSM = stateMachine<GhoulState>(GhoulState.NoAffects) {
        state(GhoulState.Stunned) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> GhoulState.Stunned
                    else -> GhoulState.NoAffects
                }
            }
            begin {
                affectIcon.playAnimationLooped(Assets.stunIcon)
                affectIcon.visible = true
                sprite.playAnimationLooped(Assets.ghoulBob)
            }
        }

        state(GhoulState.NoAffects) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> GhoulState.Stunned
                    else -> GhoulState.NoAffects
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

    private val controlFSM = stateMachine<GhoulState>(GhoulState.Idle) {
        state(GhoulState.Teleport) {
            transition {
                when {
                    attackingHero -> GhoulState.Attack
                    else -> GhoulState.Idle
                }
            }

            begin {
                toGridPosition(level.hero.cx - level.hero.dir, level.hero.cy)
                dir = dirTo(level.hero)
                cd(TELEPORT_CD, 150.milliseconds)
            }

        }
        state(GhoulState.MovingToHero) {
            transition {
                when {
                    teleporting -> GhoulState.Teleport
                    attackingHero -> GhoulState.Attack
                    else -> GhoulState.MovingToHero
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.ghoulBob)
            }
            update {
                moveTo(platformerDynamicComponent, spriteComponent, level.hero.cx, level.hero.cy, moveSpeed * tmod)
            }

        }
        state(GhoulState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> GhoulState.Attack
                    else -> GhoulState.Idle
                }
            }
            begin {
                dir = dirTo(level.hero)
                sprite.playOverlap(Assets.ghoulAttack, onAnimationFrameChange = {
                    if (it == 10) {
                        attemptToAttackHero()
                    }
                })
                cd(ANIM_PLAYING, Assets.ghoulAttack.duration)
                cd(ATTACK_CD, 3.seconds)
            }

        }
        state(GhoulState.Idle) {
            var playingAnim = false
            transition {
                when {
                    cd.has(IDLE) -> GhoulState.Idle
                    attackingHero -> GhoulState.Attack
                    else -> GhoulState.MovingToHero
                }
            }
            begin {
                playingAnim = false
                cd(IDLE, (500..1500).random().milliseconds)
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.ghoulBob)
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