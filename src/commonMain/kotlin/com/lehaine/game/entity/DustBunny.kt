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
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.anchor
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.dustBunny(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: DustBunny.() -> Unit = {}
): DustBunny = DustBunny(
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
    dangerousComponent = DangerousComponentDefault(15)
).addTo(this).addToLevel().also(callback)

class DustBunny(
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
        private const val DUST = "dustCD"
    }

    private sealed class DustBunnyState {
        object Idle : DustBunnyState()
        object MovingToHero : DustBunnyState()
        object Attack : DustBunnyState()

        object NoAffects : DustBunnyState()
        object Stunned : DustBunnyState()
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
    private val attackRange = 1.5

    private val attackingHero get() = distGridTo(level.hero) <= (0.0..attackRange).random() && !cd.has(ATTACK_CD)

    private val entityFSM = stateMachine<DustBunnyState>(DustBunnyState.NoAffects) {
        state(DustBunnyState.Stunned) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> DustBunnyState.Stunned
                    else -> DustBunnyState.NoAffects
                }
            }
            begin {
                affectIcon.playAnimationLooped(Assets.stunIcon)
                affectIcon.visible = true
                sprite.playAnimationLooped(Assets.dustBunnyIdle)
            }
        }

        state(DustBunnyState.NoAffects) {
            transition {
                when {
                    hasAffect(Affect.STUN) -> DustBunnyState.Stunned
                    else -> DustBunnyState.NoAffects
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

    private val controlFSM = stateMachine<DustBunnyState>(DustBunnyState.Idle) {
        state(DustBunnyState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> DustBunnyState.Attack
                    else -> DustBunnyState.MovingToHero
                }
            }
            begin {
                cd(ANIM_PLAYING, Assets.dustBunnyAttack.duration)
                sprite.playOverlap(Assets.dustBunnyAttack, onAnimationFrameChange = {
                    if (it == 4) {
                        attemptToAttackHero()
                    }
                })
            }
        }
        state(DustBunnyState.MovingToHero) {
            transition {
                when {
                    attackingHero -> DustBunnyState.Attack
                    else -> DustBunnyState.MovingToHero
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.dustBunnyJump)
            }
            update {
                moveTo(platformerDynamicComponent, spriteComponent, level.hero.cx, level.hero.cy, moveSpeed * tmod)
            }

        }
        state(DustBunnyState.Idle) {
            var playingAnim = false
            transition {
                when {
                    cd.has(IDLE) -> DustBunnyState.Idle
                    attackingHero -> DustBunnyState.Attack
                    else -> DustBunnyState.MovingToHero
                }
            }
            begin {
                playingAnim = false
                cd(IDLE, (500..1500).random().milliseconds)
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.dustBunnyIdle)
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
            fx.dustExplosion(centerX, centerY)
            destroy()
        }

        if (!cd.has(DUST)) {
            cd(DUST, 250.milliseconds)
            fx.dust(centerX, centerY)
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