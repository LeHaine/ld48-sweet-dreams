package com.lehaine.game.entity

import com.lehaine.game.*
import com.lehaine.game.Assets.Sfx.playSfx
import com.lehaine.game.component.*
import com.lehaine.game.view.HealthBar
import com.lehaine.game.view.healthBar
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.korge.getByPrefix
import com.lehaine.kiwi.korge.view.enhancedSprite
import com.lehaine.kiwi.random
import com.lehaine.kiwi.stateMachine
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korui.UiContainer

fun Container.hero(
    data: World.EntityHero,
    game: Game,
    callback: Hero.() -> Unit = {}
): Hero = Hero(
    game = game,
    platformerDynamicComponent = PlatformerDynamicComponentDefault(
        levelComponent = game.level,
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
).addTo(this).addToGame().also(callback)


class Hero(
    game: Game,
    platformerDynamicComponent: PlatformerDynamicComponent,
    spriteComponent: SpriteComponent,
    private val healthComponent: HealthComponent,
    private val dangerousComponent: DangerousComponent
) :
    GameEntity(game, spriteComponent, platformerDynamicComponent),
    SpriteComponent by spriteComponent,
    PlatformerDynamicComponent by platformerDynamicComponent,
    HealthComponent by healthComponent,
    DangerousComponent by dangerousComponent {

    companion object {
        private const val ON_GROUND_RECENTLY = "onGroundRecently"
        private const val ANIM_PLAYING = "animPlaying"
        private const val ATTACK_CD = "attackCD"
        private const val COMBO = "combo"
        private const val SLING_SHOT_CD = "slingShotCD"
        private const val DODGE = "dodge"
        private const val RUN_DUST = "runDust"
        private const val FOOTSTEP_SOUND = "footstepSound"
        private const val SLEEPY_Z = "sleepyZ"
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

    private val input = game.controller.createAccess("hero")

    private val speed = 0.08

    private var moveDir = 0.0

    private val jumpHeight = -0.65

    private var canSwing = true
    private val swinging get() = input.mouseDown() && canSwing

    private val runningRight get() = input.strength(GameInput.Horizontal) > 0
    private val running get() = input.down(GameInput.Horizontal)

    private val dodging
        get() = input.pressed(GameInput.Dodge) && !cd.has(
            DODGE
        )

    private val slingShot get() = input.pressed(GameInput.SlingShot) && !cd.has(SLING_SHOT_CD)

    private val jumping get() = input.pressed(GameInput.Jump) && cd.has(ON_GROUND_RECENTLY)

    private var broomCombo = 0

    private val fsm = stateMachine<HeroState>(HeroState.Sleep) {
        state(HeroState.Dead) {
            begin {
                healthBar.visible = false
                sprite.playAnimation(Assets.heroDie) {
                    sprite.playAnimationLooped(Assets.heroSleep)
                }
                var i = 0
                while (i < game.entities.size) {
                    val entity = game.entities[i]
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
                    camera.tween(
                        camera::cameraZoom[1.0, 3.0],
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
                sfx.roll.playSfx()
                sprite.playOverlap(Assets.heroRoll) {
                    animFinished = true
                }
                addAffect(Affect.INVULNERABLE, 1.seconds)
            }
            update {
                velocityX = 0.5 * dir
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
                game.slingShotCDRemaining = 2.seconds
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
                    projectile(x, y, dir, game, this@Hero)
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
                sfx.strongSwing.playSfx()
                velocityX = 0.1 * dir
                camera.shake(50.milliseconds, 0.5)
                damageEntities(true, 3.0)
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
                velocityX = 0.1 * dir
                sfx.swing.playSfx()
                damageEntities(true, 2.0)
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
                velocityX = 0.1 * dir
                sfx.swing.playSfx()
                damageEntities(false, 1.0)
            }
        }
        state(HeroState.Sleep) {
            transition {
                when {
                    running || jumping || input.mouseDown() -> HeroState.Idle
                    else -> HeroState.Sleep
                }
            }
            begin {
                sprite.playAnimationLooped(Assets.heroSleep)
            }

            update {
                if (!cd.has(SLEEPY_Z)) {
                    cd(SLEEPY_Z, 500.milliseconds)
                    fx.sleepyZs(centerX, centerY)
                }

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


    private var currentSleepState = game.sleepState

    val healthBar: HealthBar = container.run {
        healthBar(health / 10.0, 2.0, Colors["#066d00"]) {
            x = -10.0
            alignBottomToTopOf(sprite, 10)
            visible = false
        }
    }


    init {
        container.apply {
            enhancedSprite(Assets.tiles.getByPrefix("backShadow")) {
                alpha = 0.35
                smoothing = false
                x = 0.5
                y = 0.5
                anchor(0.5, 1.0)
                parent?.sendChildToBack(this)
            }
        }
        dir = -1
        enableCollisionChecks = true
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        moveDir = 0.0

        if (onGround) {
            cd(ON_GROUND_RECENTLY, 150.milliseconds)
        }

        if (!input.mouseDown()) {
            canSwing = true
        }

        if (!cd.has(COMBO)) {
            broomCombo = 0
        }

        if (game.sleepState != currentSleepState) {
            health += (maxHealth * 0.25).toInt()
            if (health > maxHealth) {
                health = maxHealth
            }
            healthBar.setHealthRatio(healthRatio)
            currentSleepState = game.sleepState
        }
        fsm.update(dt)
    }

    override fun fixedUpdate() {
        super.fixedUpdate()
        if (moveDir != 0.0) {
            velocityX += moveDir * speed
        } else {
            velocityX *= 0.3
        }
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
        game.entities.fastForEach {
            if (it != this@Hero
                && (distGridTo(it) <= 2.5 || (it is Boss && distGridTo(it) <= 4.5))
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
            dir = if (runningRight) 1 else -1
            moveDir = input.strength(GameInput.Horizontal)
        }
    }

    private fun jump() {
        velocityY = jumpHeight
        stretchX = 0.7
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}