package com.lehaine.game.entity

import com.lehaine.game.Assets
import com.lehaine.game.GRID_SIZE
import com.lehaine.game.GameEntity
import com.lehaine.game.LevelMark
import com.lehaine.game.component.*
import com.lehaine.kiwi.component.*
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Container
import com.soywiz.korui.UiContainer

fun Container.longArm(
    cx: Int, cy: Int,
    level: GenericGameLevelComponent<LevelMark>,
    callback: LongArm.() -> Unit = {}
): LongArm = LongArm(
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
    healthComponent = HealthComponentDefault(50),
    dangerousComponent = DangerousComponentDefault(10)
).addTo(this).addToLevel().also(callback)

class LongArm(
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

    init {
        enableCollisionChecks = true
        sprite.playAnimationLooped(Assets.longArmIdle)
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        if (isDead) {
            destroy()
        }
    }

    override fun damage(amount: Int, fromDir: Int) {
        healthComponent.damage(amount, fromDir)
        stretchX = 0.6
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }

}