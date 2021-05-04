package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.game.view.HealthBar
import com.lehaine.game.view.healthBar
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.component.ext.toGridPosition
import com.lehaine.kiwi.korge.view.enhancedSprite
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

inline fun Container.ghoul(
    cx: Int, cy: Int,
    game: Game,
    healthMultiplier: Double = 1.0,
    damageMultiplier: Double = 1.0,
    callback: Ghoul.() -> Unit = {}
): Ghoul = Ghoul(
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
    healthComponent = HealthComponentDefault((75 * healthMultiplier).toInt()),
    dangerousComponent = DangerousComponentDefault((50 * damageMultiplier).toInt())
).addTo(this).addToGame().also(callback)

class Ghoul(
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

    private val attackingHero get() = distGridTo(hero) <= attackRange && !cd.has(ATTACK_CD)
    private val teleporting get() = distGridTo(hero) <= 10 && !cd.has(TELEPORT_CD)

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
                toGridPosition(hero.cx - hero.dir, hero.cy)
                dir = dirTo(hero)
                cd(TELEPORT_CD, 150.milliseconds)
                sfx.teleport.playSfx()
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
                moveTo(platformerDynamicComponent, spriteComponent, hero.cx)
            }
            end { moveDir = 0 }
        }
        state(GhoulState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> GhoulState.Attack
                    else -> GhoulState.Idle
                }
            }
            begin {
                dir = dirTo(hero)
                sfx.ghoulAnticipation.playSfx()
                sprite.playOverlap(Assets.ghoulAttack, onAnimationFrameChange = {
                    if (it == 10) {
                        sfx.ghoulSwing.playSfx()
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

    override fun fixedUpdate() {
        super.fixedUpdate()
        if (moveDir != 0) {
            velocityX += moveDir * moveSpeed
        } else {
            velocityX *= 0.6
        }
    }

    override fun destroy() {
        fx.poofDust(centerX, centerY, Colors["#1884a1"])
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