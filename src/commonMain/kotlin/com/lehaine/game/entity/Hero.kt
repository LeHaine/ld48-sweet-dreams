package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.game.view.HealthBar
import com.lehaine.game.view.healthBar
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.random
import com.lehaine.kiwi.stateMachine
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.alignBottomToTopOf
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korui.UiContainer
import kotlin.math.pow

fun Container.hero(
    data: World.EntityHero,
    level: GenericGameLevelComponent<LevelMark>,
    callback: Hero.() -> Unit = {}
): Hero = Hero(
    level = level,
    platformerDynamicComponent = PlatformerDynamicComponentDefault(
        levelComponent = level,
        cx = data.cx,
        cy = data.cy,
        xr = data.pivotX.toDouble(),
        yr = data.pivotY.toDouble(),
        anchorX = data.pivotX.toDouble(),
        anchorY = data.pivotY.toDouble(),
        gridCellSize = GRID_SIZE
    ),
    spriteComponent = SpriteComponent(
        anchorX = data.pivotX.toDouble(),
        anchorY = data.pivotY.toDouble()
    ),
    healthComponent = HealthComponentDefault(200),
    dangerousComponent = DangerousComponentDefault(35)
).addTo(this).addToLevel().also(callback)


class Hero(
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
        private const val ON_GROUND_RECENTLY = "onGroundRecently"
        private const val JUMP_EXTRA = "jumpExtra"
        private const val ANIM_PLAYING = "animPlaying"
        private const val ATTACK_CD = "attackCD"
        private const val COMBO = "combo"
        private const val SLING_SHOT_CD = "slingShotCD"
        private const val DODGE = "dodge"
        private const val RUN_DUST = "runDust"
        private const val FOOTSTEP_SOUND = "footstepSound"
    }

    private sealed class HeroState {
        object Idle : HeroState()
        object Run : HeroState()
        object Jump : HeroState()
        object Dodge : HeroState()
        object Fall : HeroState()
        object Sleep : HeroState()
        object Dead : HeroState()
        object BroomAttack1 : HeroState()
        object BroomAttack2 : HeroState()
        object BroomAttack3 : HeroState()
        object SlingShot : HeroState()
    }

    private val moveSpeed = 0.04
    private val moveFriction = 0.8
    private val jumpHeight = -0.5
    private val jumpExtraSpeed = 0.04

    private var canSwing = true
    private val swinging get() = input.mouseButtons == 1 && canSwing

    private val runningLeft get() = input.keys.pressing(Key.A)
    private val runningRight get() = input.keys.pressing(Key.D)
    private val running get() = runningLeft || runningRight

    private val dodging
        get() = (input.keys.justPressed(Key.LEFT_SHIFT) || input.keys.justPressed(Key.RIGHT_SHIFT)) && !cd.has(
            DODGE
        )

    private val slingShot get() = input.keys.justPressed(Key.Q) && !cd.has(SLING_SHOT_CD)

    private val jumping get() = input.keys.justPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)
    private val jumpingExtra get() = input.keys.justPressed(Key.SPACE) && cd.has(JUMP_EXTRA)

    private var broomCombo = 0

    private val fsm = stateMachine<HeroState>(HeroState.Sleep) {
        state(HeroState.Dead) {
            begin {
                healthBar.visible = false
                sprite.playAnimation(Assets.heroDie) {
                    sprite.playAnimationLooped(Assets.heroSleep)
                }
                var i = 0
                while (i < level.entities.size) {
                    val entity = level.entities[i]
                    if (entity is MobComponent) {
                        i--
                        entity.destroy()
                    }
                    if (entity is EnemySpawner) {
                        entity.shouldSpawn = false
                        i--
                        entity.destroy()
                    }
                    i++
                }

                container.stage?.views?.launchImmediately {
                    level.camera.tween(
                        level.camera::cameraZoom[1.0, 3.0],
                        time = 500.milliseconds
                    )
                }
            }
        }
        state(HeroState.Dodge) {
            var animFinished = false
            transition {
                when {
                    animFinished -> HeroState.Idle
                    else -> HeroState.Dodge
                }
            }
            begin {
                sprite.playOverlap(Assets.heroRoll) {
                    animFinished = true
                }
                addAffect(Affect.INVULNERABLE, 1.seconds)
            }
            update {
                velocityX += moveSpeed * 1.25 * dir * tmod
                if (!cd.has(RUN_DUST)) {
                    cd(RUN_DUST, 100.milliseconds)
                    fx.runDust(centerX, bottom, -dir)
                }
            }
            end {
                cd(DODGE, 200.milliseconds)
                animFinished = false
            }
        }
        state(HeroState.SlingShot) {
            transition {
                when {
                    dodging -> HeroState.Dodge
                    cd.has(ATTACK_CD) -> HeroState.SlingShot
                    else -> HeroState.Idle
                }
            }
            begin {
                sfx.shoot.playSfx()
                cd(ATTACK_CD, 300.milliseconds)
                level.slingShotCDRemaining = 2.seconds
                cd(SLING_SHOT_CD, 2.seconds)
                cd(ANIM_PLAYING, Assets.heroSlingShot.duration)
                sprite.playOverlap(Assets.heroSlingShot)
                repeat(3) {
                    val x = when (it) {
                        0 -> centerX + (5 * dir)
                        else -> centerX
                    }
                    val y = when (it) {
                        0 -> centerY
                        1 -> centerY - 2
                        else -> centerY + 2
                    }
                    projectile(x, y, dir, level, this@Hero)
                }
            }
        }
        state(HeroState.BroomAttack3) {
            transition {
                when {
                    dodging -> HeroState.Dodge
                    cd.has(ATTACK_CD) -> HeroState.BroomAttack3
                    swinging -> HeroState.BroomAttack1
                    else -> HeroState.Idle
                }
            }
            begin {
                cd.remove(COMBO)
                canSwing = false
                sprite.playOverlap(Assets.heroBroomAttack3)
                cd(ATTACK_CD, 400.milliseconds)
                cd(ANIM_PLAYING, Assets.heroBroomAttack3.duration)
                broomCombo = 0
                sfx.swing.playSfx()
                damageEntities(true, 2.0)
            }
        }
        state(HeroState.BroomAttack2) {
            transition {
                when {
                    dodging -> HeroState.Dodge
                    cd.has(ATTACK_CD) -> HeroState.BroomAttack2
                    swinging && cd.has(COMBO) -> HeroState.BroomAttack3
                    else -> HeroState.Idle
                }
            }
            begin {
                canSwing = false
                sprite.playOverlap(Assets.heroBroomAttack2)
                cd(COMBO, 800.milliseconds)
                cd(ATTACK_CD, 400.milliseconds)
                cd(ANIM_PLAYING, Assets.heroBroomAttack2.duration)
                broomCombo++
                sfx.swing.playSfx()
                damageEntities(true, 1.25)
            }
        }
        state(HeroState.BroomAttack1) {
            transition {
                when {
                    dodging -> HeroState.Dodge
                    cd.has(ATTACK_CD) -> HeroState.BroomAttack1
                    swinging && cd.has(COMBO) -> HeroState.BroomAttack2
                    else -> HeroState.Idle
                }
            }
            begin {
                canSwing = false
                sprite.playOverlap(Assets.heroBroomAttack1)
                cd(ATTACK_CD, 400.milliseconds)
                cd(ANIM_PLAYING, Assets.heroBroomAttack1.duration)
                cd(COMBO, 800.milliseconds)
                broomCombo++
                sfx.swing.playSfx()
                damageEntities(false, 1.0)
            }
        }
        state(HeroState.Sleep) {
            transition {
                when {
                    running || jumping  || input.mouseButtons != 0 -> HeroState.Idle
                    else -> HeroState.Sleep
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.heroSleep)
            }
        }
        state(HeroState.Fall) {
            transition {
                when {
                    onGround -> {
                        camera.shake(25.milliseconds, 0.3)
                        sfx.land.playSfx()
                        HeroState.Idle
                    }
                    else -> HeroState.Fall
                }
            }
            update {
                run()
            }
        }

        state(HeroState.Jump) {
            transition {
                when {
                    velocityY > 0 -> HeroState.Fall
                    else -> HeroState.Jump
                }
            }
            begin {
                if (jumping) {
                    jump()
                }
            }
            update {
                if (jumpingExtra) {
                    jumpExtra()
                }
                run()
            }

        }
        state(HeroState.Run) {
            transition {
                when {
                    isDead -> HeroState.Dead
                    dodging -> HeroState.Dodge
                    jumping -> HeroState.Jump
                    slingShot -> HeroState.SlingShot
                    swinging && broomCombo == 0 -> HeroState.BroomAttack1
                    swinging && broomCombo == 1 -> HeroState.BroomAttack2
                    swinging && broomCombo == 2 -> HeroState.BroomAttack3
                    running -> HeroState.Run
                    else -> HeroState.Idle
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.heroRun)
            }
            update { run() }
        }
        state(HeroState.Idle) {
            var playingAnim = false
            transition {
                when {
                    isDead -> HeroState.Dead
                    dodging -> HeroState.Dodge
                    jumping -> HeroState.Jump
                    running -> HeroState.Run
                    slingShot -> HeroState.SlingShot
                    swinging && broomCombo == 0 -> HeroState.BroomAttack1
                    swinging && broomCombo == 1 -> HeroState.BroomAttack2
                    swinging && broomCombo == 2 -> HeroState.BroomAttack3
                    else -> HeroState.Idle
                }
            }
            begin {
                playingAnim = false
            }
            update {
                if (!playingAnim && !cd.has(ANIM_PLAYING)) {
                    sprite.playAnimationLooped(Assets.heroIdle)
                    playingAnim = true
                }
                run()
            }
        }
    }
    val healthBar: HealthBar = container.run {
        healthBar(health / 10.0, 2.0, Colors["#066d00"]) {
            x = -10.0
            alignBottomToTopOf(sprite, 10)
            visible = false
        }
    }

    init {
        dir = -1
        enableCollisionChecks = true
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (onGround) {
            cd(ON_GROUND_RECENTLY, 150.milliseconds)
        }

        if (input.mouseButtons == 0) {
            canSwing = true
        }

        if (!cd.has(COMBO)) {
            broomCombo = 0
        }
        fsm.update(dt)
    }

    override fun damage(amount: Int, fromDir: Int) {
        if (hasAffect(Affect.INVULNERABLE) || isDead) {
            return
        }
        addAffect(Affect.INVULNERABLE, 200.milliseconds)
        camera.shake(50.milliseconds, 0.5)
        velocityX = (0.3..0.5).random() * -fromDir
        velocityY = -(0.2..0.3).random()
        sfx.hit.playSfx()
        healthComponent.damage(amount, fromDir)
        healthBar.setHealthRatio(healthRatio)
        healthBar.visible = true
        blink()
    }

    private fun damageEntities(stunEnemy: Boolean, damageMultiplier: Double) {
        var hasHit = false
        level.entities.fastForEach {
            if (it != this@Hero
                && (distGridTo(it) <= 2 || (it is Boss && distGridTo(it) <= 4.5))
                && it is HealthComponent
            ) {
                if (!hasHit) {
                    val x = if (dir == 1) right else left
                    fx.broomDust(x, centerY, dir)
                    hasHit = true
                }
                if (dirTo(it) == dir || distGridTo(it) <= 1) {
                    attack(it, -dirTo(it), damageMultiplier)
                    if (stunEnemy) {
                        (it as GameEntity).addAffect(Affect.STUN, 1.seconds)
                    }
                }
            }
        }
    }

    private fun run() {
        if (running) {
            if (!cd.has(RUN_DUST) && onGround) {
                cd(RUN_DUST, 100.milliseconds)
                fx.runDust(centerX, bottom, -dir)
            }
            if (!cd.has(FOOTSTEP_SOUND) && onGround) {
                cd(FOOTSTEP_SOUND, 350.milliseconds)
                sfx.footstep.playSfx()
            }
            val speed = if (runningRight) moveSpeed else -moveSpeed
            dir = if (runningRight) 1 else -1
            velocityX += speed * tmod
        } else {
            velocityX *= moveFriction.pow(tmod)
        }
    }

    private fun jump() {
        velocityY = jumpHeight
        stretchX = 0.7
        cd(JUMP_EXTRA, 100.milliseconds)
    }

    private fun jumpExtra() {
        velocityY -= jumpExtraSpeed * tmod
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}