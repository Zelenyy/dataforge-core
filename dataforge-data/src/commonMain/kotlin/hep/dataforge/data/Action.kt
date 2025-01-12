package hep.dataforge.data

import hep.dataforge.meta.Meta

/**
 * A simple data transformation on a data node
 */
interface Action<in T : Any, out R : Any> {
    /**
     * Transform the data in the node, producing a new node. By default it is assumed that all calculations are lazy
     * so not actual computation is started at this moment
     */
    operator fun invoke(node: DataNode<T>, meta: Meta): DataNode<R>

    /**
     * Terminal action is the one that could not be invoked lazily and requires some kind of blocking computation to invoke
     */
    val isTerminal: Boolean get() = false
}

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
infix fun <T : Any, I : Any, R : Any> Action<T, I>.then(action: Action<I, R>): Action<T, R> {
    // TODO introduce composite action and add optimize by adding action to the list
    return object : Action<T, R> {
        override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
            return action(this@then.invoke(node, meta), meta)
        }

        override val isTerminal: Boolean
            get() = this@then.isTerminal || action.isTerminal
    }
}

