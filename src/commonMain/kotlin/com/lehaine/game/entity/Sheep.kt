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
import com.soywiz.klock.seconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.alignBottomToTopOf
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.centerXOn
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.sheep(
    cx: Int, cy: Int,
    game: Game,
    healthMultiplier: Double = 1.0,
    damageMultiplier: Double = 1.0,
    callback: Sheep.() -> Unit = {}
): Sheep = Sheep(
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
    healthComponent = HealthComponentDefault((30 * healthMultiplier).toInt()),
    dangerousComponent = DangerousComponentDefault((5 * damageMultiplier).toInt())
).addTo(this).addToGame().also(callback)

class Sheep(
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
    private val attackRange = 1.5

    private val attackingHero get() = distGridTo(hero) <= (0.0..attackRange).random() && !cd.has(ATTACK_CD)

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
                moveTo(platformerDynamicComponent, spriteComponent, hero.cx, cy, moveSpeed * it.seconds)
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
                dir = dirTo(hero)
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

        if (healthRatio != 1.0) {
            healthBar.visible = true
        }

        entityFSM.update(dt)
    }

    override fun destroy() {
        fx.poofDust(centerX, centerY, Colors["#c3c0b0"])
        super.destroy()
    }

    override fun damage(amount: Int, fromDir: Int) {
        sfx.hit.playSfx()
        healthComponent.damage(amount, fromDir)
        healthBar.setHealthRatio(healthRatio)
        stretchX = 0.6
        blink()
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