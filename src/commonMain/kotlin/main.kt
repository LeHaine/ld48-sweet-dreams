import com.lehaine.game.*
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.view.views
import com.soywiz.korim.color.Colors
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.SizeInt

suspend fun main() = Korge(Korge.Config(module = GameModule))

object GameModule : Module() {
    override val mainScene = StartScene::class

    override val windowSize: SizeInt = SizeInt(Size(320 * 4, 180 * 4))
    override val size: SizeInt = SizeInt(Size(320, 180))
    override val bgcolor = Colors["#2b2b2b"]

    override suspend fun AsyncInjector.configure() {
        Assets.init(views())
        mapSingleton { World().apply { loadAsync() } }
        mapInstance(0) // load first level
        mapPrototype { StartScene() }
        mapPrototype { Game(get(), get()) }
        mapPrototype { EndScene() }
    }
}