package com.lehaine.game.component

import com.lehaine.kiwi.component.Component
import com.lehaine.kiwi.component.DynamicComponent
import com.lehaine.kiwi.component.SpriteComponent

interface TargetComponent : Component {

    fun moveTo(
        dynamicComponent: DynamicComponent,
        spriteComponent: SpriteComponent, tx: Int, ty: Int, speed: Double
    )
}

class TargetComponentDefault : TargetComponent {
    override fun moveTo(
        dynamicComponent: DynamicComponent,
        spriteComponent: SpriteComponent,
        tx: Int,
        ty: Int,
        speed: Double
    ) {
        val cx = dynamicComponent.cx
        val cy = dynamicComponent.cy

        if (cx != tx) {
            if (tx > cx) {
                spriteComponent.dir = 1
                dynamicComponent.velocityX += speed
            }
            if (tx < cx) {
                spriteComponent.dir = -1
                dynamicComponent.velocityX -= speed
            }
        }

        if (cy != ty) {
            if (ty > cy) {
                dynamicComponent.velocityY += speed
            }
            if (ty < cy) {
                dynamicComponent.velocityY -= speed
            }
        }
    }
}
