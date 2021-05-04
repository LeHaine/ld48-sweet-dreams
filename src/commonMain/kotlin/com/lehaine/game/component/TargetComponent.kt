package com.lehaine.game.component

import com.lehaine.kiwi.component.Component
import com.lehaine.kiwi.component.DynamicComponent
import com.lehaine.kiwi.component.SpriteComponent

interface TargetComponent : Component {

    var moveDir: Int

    fun moveTo(
        dynamicComponent: DynamicComponent,
        spriteComponent: SpriteComponent, tx: Int//, ty: Int, speed: Double
    )
}

class TargetComponentDefault : TargetComponent {
    override var moveDir: Int = 0

    override fun moveTo(
        dynamicComponent: DynamicComponent,
        spriteComponent: SpriteComponent,
        tx: Int,
        //      ty: Int,
        //   speed: Double
    ) {
        moveDir = 0
        val cx = dynamicComponent.cx
        //       val cy = dynamicComponent.cy

        if (cx != tx) {
            if (tx > cx) {
                spriteComponent.dir = 1
                moveDir = 1
            }
            if (tx < cx) {
                spriteComponent.dir = -1
                moveDir = -1
            }
        }

//        if (cy != ty) {
//            if (ty > cy) {
//                dynamicComponent.velocityY += speed
//            }
//            if (ty < cy) {
//                dynamicComponent.velocityY -= speed
//            }
//        }
    }
}
