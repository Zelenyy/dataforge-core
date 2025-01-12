package hep.dataforge.context

import hep.dataforge.names.Name
import hep.dataforge.names.appendLeft
import hep.dataforge.names.toName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ContextTest {
    class DummyPlugin : AbstractPlugin() {
        override val tag get() = PluginTag("test")

        override fun provideTop(target: String): Map<Name, Any> {
            return when(target){
                "test" -> listOf("a", "b", "c.d").associate { it.toName() to it.toName() }
                else -> emptyMap()
            }
        }
    }

    @Test
    fun testPluginManager() {
        Global.plugins.load(DummyPlugin())
        val members = Global.content<Name>("test")
        assertEquals(3, members.count())
        members.forEach {
            assertTrue{it.key == it.value.appendLeft("test")}
        }
    }

}