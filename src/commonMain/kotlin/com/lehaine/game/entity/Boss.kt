package com.lehaine.game.entity

import com.lehaine.game.Assets
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.GRID_SIZE
import com.lehaine.game.Game
import com.lehaine.game.GameEntity
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
import com.soywiz.korma.geom.Anchor
import com.soywiz.korui.UiContainer

inline fun Container.boss(
    cx: Int, cy: Int,
    game: Game,
    callback: Boss.() -> Unit = {}
): Boss = Boss(
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
    healthComponent = HealthComponentDefault(750),
    dangerousComponent = DangerousComponentDefault(35)
).addTo(this).addToGame().also(callback)

class Boss(
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
        private const val DUST = "dust"
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
    private val attackRange = 5.0

    private val attackingHero get() = distGridTo(hero) <= (0.0..attackRange).random() && !cd.has(ATTACK_CD)

    private val entityFSM = stateMachine<BossState>(BossState.NoAffects) {
        state(BossState.NoAffects) {
            transition {
                BossState.NoAffects
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
                moveTo(platformerDynamicComponent, spriteComponent, hero.cx)
            }
            end { moveDir = 0 }

        }
        state(BossState.Attack) {
            transition {
                when {
                    cd.has(ANIM_PLAYING) -> BossState.Attack
                    else -> BossState.Idle
                }
            }
            begin {
                dir = dirTo(hero)
                sprite.playOverlap(Assets.bossAttack, onAnimationFrameChange = {
                    if (it in 6..11) {
                        if (it == 6) {
                            sfx.bossYell.playSfx()
                            camera.shake(300.milliseconds, 0.3)
                        }
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

    val healthBar: HealthBar = container.run {
        healthBar(health / 10.0, 4.0) {
            centerXOn(sprite)
            alignBottomToTopOf(sprite, 42)
            visible = false
        }
    }

    init {
        width = 32.0
        height = 32.0
        enableCollisionChecks = true
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (isDead) {
            destroy()
        }
        if (!cd.has(DUST)) {
            cd(DUST, 500.milliseconds)
            fx.bossDust(centerX, centerY)
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

    override fun damage(amount: Int, fromDir: Int) {
        blink()
        sfx.hit.playSfx()
        healthComponent.damage(amount, fromDir)
        healthBar.setHealthRatio(healthRatio)
        stretchX = 0.6
    }

    override fun destroy() {
        fx.bossDeath(centerX, centerY)
        if (health <= 0) {
            game.gameFinshed = true
        }
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