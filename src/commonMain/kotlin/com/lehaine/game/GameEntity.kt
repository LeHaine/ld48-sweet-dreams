package com.lehaine.game

import com.lehaine.game.component.GenericGameLevelComponent
import com.lehaine.kiwi.component.*
import com.lehaine.kiwi.component.ext.dirTo
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.radians
import com.soywiz.korui.UiContainer
import kotlin.math.atan2

/**
 * An example [Entity] that extends the [SpriteLevelEntity] class.
 *
 * Create more entities that extend this class by adding different components.
 *
 * Example:
 * ```
 *  class Hero(
 *      level: GenericGameLevelComponent<LevelMark>,
 *      spriteComponent: SpriteComponent,
 *      platformerDynamic: PlatformerDynamicComponent
 *  ) : GameEntity(level, spriteComponent, platformerDynamic),
 *      PlatformerDynamicComponent by platformerDynamic,
 *      SpriteComponent by spriteComponent {}
 * ```
 *
 * When instantiated make sure to add it to a container and to the level! Entities
 * are automatically removed the level entities list when destroyed.
 *
 * ```
 * val hero = Hero(level, sprite, plaformer).addTo(parentContainer).addToLevel()
 * ```
 */
open class GameEntity(
    override val level: GenericGameLevelComponent<LevelMark>,
    spriteComponent: SpriteComponent = SpriteComponentDefault(anchorX = 0.5, anchorY = 1.0),
    position: LevelDynamicComponent = LevelDynamicComponentDefault(
        levelComponent = level,
        anchorX = 0.5,
        anchorY = 1.0
    ),
    scaleComponent: ScaleAndStretchComponent = ScaleAndStretchComponentDefault()
) : SpriteLevelEntity(level, spriteComponent, position, scaleComponent),
    ScaleAndStretchComponent by scaleComponent {

    val fx get() = level.fx
    val camera get() = level.camera
    val sfx get() = Assets.Sfx

    // TODO maybe add a component or something to handle creating inputs
    val input get() = container.stage!!.views.input

    val mouseX get() = level.camera.content.localMouseX(container.stage!!.views)
    val mouseY get() = level.camera.content.localMouseY(container.stage!!.views)

    val angleToMouse
        get() = atan2(
            mouseY - gridPositionComponent.centerY,
            mouseX - gridPositionComponent.centerX
        ).radians

    val dirToMouse
        get() = gridPositionComponent.dirTo(mouseX)

    private val affects = hashMapOf<Affect, TimeSpan>()

    protected var baseColor = Colors.WHITE
    protected var blinkColor = Colors.RED

    companion object {
        private const val BLINK = "blink"
    }

    override fun update(dt: TimeSpan) {
        super.update(dt)
        updateAffects(dt)

        if (cd.has(BLINK)) {
            spriteComponent.sprite.colorMul = blinkColor
        } else {
            spriteComponent.sprite.colorMul = baseColor
        }
    }

    protected fun debugComponents(container: UiContainer) {
        spriteComponent.buildDebugInfo(container)
        gridPositionComponent.buildDebugInfo(container)
        scaleComponent.buildDebugInfo(container)
    }

    fun blink(color: RGBA = Colors.RED) {
        blinkColor.withRGB(color.rgb)
        cd(BLINK, 200.milliseconds)
    }

    fun hasAffect(affect: Affect) = affects.containsKey(affect)

    fun addAffect(affect: Affect, duration: TimeSpan, addToCurrentDuration: Boolean = false) {
        if (affects.containsKey(affect)) {
            if (addToCurrentDuration) {
                affects[affect] = affects[affect]?.plus(duration) ?: duration
                return
            }
        }
        affects[affect] = duration
        onAffectStart(affect)
    }

    fun removeAffect(affect: Affect) {
        affects.remove(affect)
        onAffectEnd(affect)
    }

    open fun onAffectStart(affect: Affect) {}
    open fun onAffectUpdate(affect: Affect) {}
    open fun onAffectEnd(affect: Affect) {}

    private fun updateAffects(dt: TimeSpan) {
        affects.keys.forEach {
            var remainingTime = affects[it] ?: TimeSpan.ZERO
            remainingTime -= dt
            if (remainingTime <= TimeSpan.ZERO) {
                removeAffect(it)
            } else {
                affects[it] = remainingTime
                onAffectUpdate(it)
            }
        }
    }
}