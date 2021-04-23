import com.lehaine.game.Assets
import com.lehaine.game.LevelScene
import com.lehaine.game.World
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.view.views
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.color.Colors
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.SizeInt

suspend fun main() = Korge(Korge.Config(module = GameModule))

object GameModule : Module() {
    override val mainScene = LevelScene::class

    override val windowSize: SizeInt = SizeInt(Size(480 * 3, 270 * 3))
    override val size: SizeInt = SizeInt(Size(480, 270))
    override val bgcolor = Colors["#2b2b2b"]

    override suspend fun AsyncInjector.configure() {
        Assets.init(views())
        mapSingleton { World().apply { loadAsync() } }
        mapInstance(0) // load first level
        mapPrototype { LevelScene(get(), get()) }
    }
}