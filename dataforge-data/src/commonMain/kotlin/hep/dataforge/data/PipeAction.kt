package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlin.reflect.KClass

class ActionEnv(val name: Name, val meta: Meta)


/**
 * Action environment
 */
class PipeBuilder<T, R>(var name: Name, var meta: MetaBuilder) {
    lateinit var result: suspend ActionEnv.(T) -> R

    /**
     * Calculate the result of goal
     */
    fun result(f: suspend ActionEnv.(T) -> R) {
        result = f;
    }
}


class PipeAction<T : Any, R : Any>(
    val inputType: KClass<T>,
    val outputType: KClass<R>,
    private val block: PipeBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        node.checkType(inputType)

        return DataNode.build(outputType) {
            node.dataSequence().forEach { (name, data) ->
                //merging data meta with action meta (data meta is primary)
                val oldMeta = meta.builder().apply { update(data.meta) }
                // creating environment from old meta and name
                val env = ActionEnv(name, oldMeta)
                //applying transformation from builder
                val builder = PipeBuilder<T, R>(name, oldMeta).apply(block)
                //getting new name
                val newName = builder.name
                //getting new meta
                val newMeta = builder.meta.seal()
                val newData = data.pipe(outputType, meta = newMeta) { builder.result(env, it) }
                //setting the data node
                this[newName] = newData
            }
        }
    }
}

inline fun <reified T : Any, reified R : Any> DataNode<T>.pipe(
    meta: Meta,
    noinline action: PipeBuilder<T, R>.() -> Unit
): DataNode<R> = PipeAction(T::class, R::class, action).invoke(this, meta)



