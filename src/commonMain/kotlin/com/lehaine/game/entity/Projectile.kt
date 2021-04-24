package com.lehaine.game.entity

import com.lehaine.game.Assets
import com.lehaine.game.GRID_SIZE
import com.lehaine.game.GameEntity
import com.lehaine.game.LevelMark
import com.lehaine.game.component.DangerousComponent
import com.lehaine.game.component.DangerousComponentDefault
import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.game.component.HealthComponent
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.component.ext.toPixelPosition
import com.lehaine.kiwi.korge.getByPrefix
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.Colors
import com.soywiz.korui.UiContainer

inline fun GameEntity.projectile(
    px: Double,
    py: Double,
    fireDir: Int,
    level: GenericGameLevelComponent<LevelMark>,
    owner: GameEntity,
    callback: Projectile.() -> Unit = {}
): Projectile = Projectile(
    level = level,
    levelDynamicComponent = LevelDynamicComponentDefault(
        levelComponent = level,
        xr = 0.5,
        yr = 0.5,
        anchorX = 0.5,
        anchorY = 0.5,
        gridCellSize = GRID_SIZE,
        rightCollisionRatio = 1.0,
        leftCollisionRatio = 0.0
    ),
    spriteComponent = SpriteComponent(
        anchorX = 0.5,
        anchorY = 0.5,
    ),
    dangerousComponent = DangerousComponentDefault(25),
    fireDir = fireDir,
    owner = owner
).apply {
    toPixelPosition(px, py)
}.addToLevel()
    .addTo(level.camera.content)
    .also(callback)

class Projectile(
    level: GenericGameLevelComponent<LevelMark>,
    levelDynamicComponent: LevelDynamicComponent,
    spriteComponent: SpriteComponent,
    private val dangerousComponent: DangerousComponent,
    private val owner: GameEntity,
    fireDir: Int
) :
    GameEntity(level, spriteComponent, levelDynamicComponent),
    SpriteComponent by spriteComponent,
    LevelDynamicComponent by levelDynamicComponent,
    DangerousComponent by dangerousComponent {

    private val speed = 0.75

    init {
        width = 1.0
        height = 1.0
        enableCollisionChecks = true
        sprite.bitmap = Assets.tiles.getByPrefix("fxLineDir")
        dir = fireDir
        sprite.colorMul = Colors.YELLOW
        sprite.blendMode = BlendMode.ADD
        velocityX = speed * fireDir
        frictionX = 1.0
        frictionY = 1.0
    }

    override fun onCollisionEnter(entity: Entity) {
        super.onCollisionEnter(entity)
        if (entity != owner && entity is HealthComponent) {
            attack(entity, entity.dirTo(this))
            destroy()
        }
    }

    override fun onLevelCollision(xDir: Int, yDir: Int) {
        super.onLevelCollision(xDir, yDir)
        destroy()
    }

    override fun buildDebugInfo(container: UiContainer) {
        debugComponents(container)
    }
}
