A base game template project for getting Korge games up and running quickly.

## How to use

Extend the `Entity` class to create new entities that use the grid positioning logic.

To create new components extend the `Component` interface or one of many other existing interfaces such as
* `DynamicComponent` - A simple physics movement with no checks to collision. Override the collision checks to add your own
* `PlatformerDynamicComponent` - A platformer dynamic component that handles collision and gravity for platformers
* `SpriteComponent` - This creates an `EnhancedSprite` view and adds a `dir` field.
* `LevelDynamicComponent` - Requires a `LevelComponent` reference to be used with collision checks or any other update checks.
* `ScaleAndStretchComponent` - Allows scaling and stretching/squashing of whatever.
And many others.
  
Use these components to create new entities by using composition vs inheritance.
Mix and match to prevent coupling of components and use the new entity as the behavior.

Examples:

**Base Entity class**:
```kotlin
class GameEntity() : Entity(),
    GridPositionComponent by gridPositionComponent,
    ScaleAndStretchComponent by scaleComponent {
        // add logic here
    }
```


**Sprite Entity class:**

```kotlin
class GameEntity(
    spriteComponent: SpriteComponent = SpriteComponentDefault(anchorX = 0.5, anchorY = 1.0),
    position: GridPositionComponent = GridPositionComponentDefault(
        anchorX = 0.5,
        anchorY = 1.0
    ),
    scaleComponent: ScaleAndStretchComponent = ScaleAndStretchComponentDefault()
) : SpriteEntity(spriteComponent, position, scaleComponent),
    SpriteComponent by spriteComponent,
    GridPositionComponent by position,
    ScaleAndStretchComponent by scaleComponent {
        // add logic here
    }
```


**Sprite Level Entity class:**

```Kotlin
class GameEntity(
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
        // add logic here
    }
```
