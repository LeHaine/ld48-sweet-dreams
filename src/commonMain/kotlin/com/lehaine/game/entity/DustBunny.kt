package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.game.view.HealthBar
import com.lehaine.game.view.healthBar
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.random
import com.lehaine.kiwi.stateMachine
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.alignBottomToTopOf
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.centerXOn
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.dustBunny(
    cx: Int, cy: Int,
    game:Game,
    healthMultiplier: Double = 1.0,
    damageMultiplier: Double = 1.0,
    callback: DustBunny.() -> Unit = {}
): DustBunny = DustBunny(
    game = game,
    platformerDynamicComponent = PlatformerDynamicComponentDefault(
        levelComponent = game.level,
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
    healthComponent = HealthComponentDefault((25 * healthMultiplier).toInt()),
    dangerousComponent = DangerousComponentDefault((15 * damageMultiplier).toInt())
).addTo(this).addToGame().also(callback)

class DustBunny(
    game: Game,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
    private val targetComponent: TargetComponent,
    private val healthComponent: HealthComponent,
    private val dangerousComponent: DangerousComponent
) :
    GameEntity(game, spriteComponent, platformerDynamicComponent),
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

    private val attackingHero get() = distGridTo(hero) <= (0.0..attackRange).random() && !cd.has(ATTACK_CD)

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
                moveTo(platformerDynamicComponent, spriteComponent, hero.cx, cy, moveSpeed * it.seconds)
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
    val healthBar: HealthBar = container.run {
        healthBar(health / 10.0, 2.0) {
            centerXOn(sprite)
            alignBottomToTopOf(sprite, 10)
            visible = false
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

        if (!cd.has(DUST)) {
            cd(DUST, 250.milliseconds)
            fx.dust(centerX, centerY)
        }

        if (healthRatio != 1.0) {
            healthBar.visible = true
        }

        entityFSM.update(dt)
    }

    override fun damage(amount: Int, fromDir: Int) {
        sfx.hit.playSfx()
        velocityX = (0.15..0.25).random() * -fromDir
        velocityY = -(0.2..0.3).random()
        healthComponent.damage(amount, fromDir)
        healthBar.setHealthRatio(healthRatio)
        stretchX = 0.6
        blink()
    }


    override fun destroy() {
        fx.dustExplosion(centerX, centerY)
        super.destroy()
    }

    private fun attemptToAttackHero() {
        if (distGridTo(hero) <= attackRange && dir == dirTo(hero)) {
            attack(hero, -dirTo(hero))
        }
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }

}