package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.builder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.collections.set
import kotlin.reflect.KClass


class FragmentRule<T : Any, R : Any>(val name: Name, var meta: MetaBuilder) {
    lateinit var result: suspend (T) -> R

    fun result(f: suspend (T) -> R) {
        result = f;
    }
}


class SplitBuilder<T : Any, R : Any>(val name: Name, val meta: Meta) {
    internal val fragments: MutableMap<Name, FragmentRule<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    fun fragment(name: String, rule: FragmentRule<T, R>.() -> Unit) {
        fragments[name.toName()] = rule
    }
}

class SplitAction<T : Any, R : Any>(
    val inputType: KClass<T>,
    val outputType: KClass<R>,
    private val action: SplitBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        node.checkType(inputType)

        return DataNode.build(outputType) {
            node.dataSequence().forEach { (name, data) ->

                val laminate = Laminate(data.meta, meta)

                val split = SplitBuilder<T, R>(name, data.meta).apply(action)


                // apply individual fragment rules to result
                split.fragments.forEach { (fragmentName, rule) ->
                    val env = FragmentRule<T, R>(fragmentName, laminate.builder())

                    rule(env)

                    val res = data.pipe(outputType, meta = env.meta) { env.result(it) }
                    set(env.name, res)
                }
            }
        }
    }
}