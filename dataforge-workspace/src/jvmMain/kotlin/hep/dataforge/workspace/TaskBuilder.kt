package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.data.*
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.reflect.KClass

@TaskBuildScope
class TaskBuilder(val name: String) {
    private var modelTransform: TaskModelBuilder.(Meta) -> Unit = { data("*") }
    var descriptor: NodeDescriptor? = null

    /**
     * TODO will look better as extension class
     */
    private class DataTransformation(
        val from: String = "",
        val to: String = "",
        val transform: (Context, TaskModel, DataNode<Any>) -> DataNode<Any>
    ) {
        operator fun invoke(workspace: Workspace, model: TaskModel, node: DataNode<Any>): DataNode<Any>? {
            val localData = if (from.isEmpty()) {
                node
            } else {
                node[from.toName()].node ?: return null
            }
            return transform(workspace.context, model, localData)
        }
    }

    private val dataTransforms: MutableList<DataTransformation> = ArrayList();

    fun model(modelTransform: TaskModelBuilder.(Meta) -> Unit) {
        this.modelTransform = modelTransform
    }

    fun <T : Any> transform(
        inputType: KClass<T>,
        from: String = "",
        to: String = "",
        block: TaskModel.(Context, DataNode<T>) -> DataNode<Any>
    ) {
        dataTransforms += DataTransformation(from, to) { context, model, data ->
            block(model, context, data.cast(inputType))
        }
    }

    inline fun <reified T : Any> transform(
        from: String = "",
        to: String = "",
        noinline block: TaskModel.(Context, DataNode<T>) -> DataNode<Any>
    ) {
        transform(T::class, from, to, block)
    }

    /**
     * Perform given action on data elements in `from` node in input and put the result to `to` node
     */
    inline fun <reified T : Any, reified R : Any> action(
        from: String = "",
        to: String = "",
        crossinline block: Context.() -> Action<T, R>
    ) {
        transform(from, to) { context, data: DataNode<T> ->
            block(context).invoke(data, meta)
        }
    }

    class TaskEnv(val name: Name, val meta: Meta, val context: Context)

    /**
     * A customized pipe action with ability to change meta and name
     */
    inline fun <reified T : Any, reified R : Any> customPipe(
        from: String = "",
        to: String = "",
        crossinline block: PipeBuilder<T, R>.(Context) -> Unit
    ) {
        action(from, to) {
            val context = this
            PipeAction(
                inputType = T::class,
                outputType = R::class
            ) { block(context) }
        }
    }

    /**
     * A simple pipe action without changing meta or name
     */
    inline fun <reified T : Any, reified R : Any> pipe(
        from: String = "",
        to: String = "",
        crossinline block: suspend TaskEnv.(T) -> R
    ) {
        action(from, to) {
            val context = this
            PipeAction(
                inputType = T::class,
                outputType = R::class
            ) {
                //TODO automatically append task meta
                result = { data ->
                    TaskEnv(name, meta, context).block(data)
                }
            }
        }
    }

    /**
     * Join elements in gathered data by multiple groups
     */
    inline fun <reified T : Any, reified R : Any> joinByGroup(
        from: String = "",
        to: String = "",
        crossinline block: JoinGroupBuilder<T, R>.(Context) -> Unit
    ) {
        action(from, to) {
            JoinAction(
                inputType = T::class,
                outputType = R::class
            ) { block(this@action) }
        }
    }

    /**
     * Join all elemlents in gathered data matching input type
     */
    inline fun <reified T : Any, reified R : Any> join(
        from: String = "",
        to: String = "",
        crossinline block: suspend TaskEnv.(Map<Name, T>) -> R
    ) {
        action(from, to) {
            val context = this
            JoinAction(
                inputType = T::class,
                outputType = R::class,
                action = {
                    result(
                        actionMeta[TaskModel.MODEL_TARGET_KEY]?.string ?: "@anonymous"
                    ) { data ->
                        TaskEnv(name, meta, context).block(data)
                    }
                }
            )
        }
    }

    /**
     * Split each element in gathered data into fixed number of fragments
     */
    inline fun <reified T : Any, reified R : Any> split(
        from: String = "",
        to: String = "",
        crossinline block: SplitBuilder<T, R>.(Context) -> Unit
    ) {
        action(from, to) {
            SplitAction(
                inputType = T::class,
                outputType = R::class
            ) { block(this@action) }
        }
    }

    /**
     * Use DSL to create a descriptor for this task
     */
    fun description(transform: NodeDescriptor.() -> Unit) {
        this.descriptor = NodeDescriptor.build(transform)
    }

    internal fun build(): GenericTask<Any> =
        GenericTask(
            name,
            Any::class,
            descriptor ?: NodeDescriptor.empty(),
            modelTransform
        ) {
            val workspace = this
            { data ->
                val model = this
                if (dataTransforms.isEmpty()) {
                    //return data node as is
                    logger.warn("No transformation present, returning input data")
                    data
                } else {
                    val builder = DataTreeBuilder(Any::class)
                    dataTransforms.forEach { transformation ->
                        val res = transformation(workspace, model, data)
                        if (res != null) {
                            if (transformation.to.isEmpty()) {
                                builder.update(res)
                            } else {
                                builder[transformation.to.toName()] = res
                            }
                        }
                    }
                    builder.build()
                }
            }
        }
}

fun Workspace.Companion.task(name: String, builder: TaskBuilder.() -> Unit): GenericTask<Any> {
    return TaskBuilder(name).apply(builder).build()
}

fun WorkspaceBuilder.task(name: String, builder: TaskBuilder.() -> Unit) {
    task(TaskBuilder(name).apply(builder).build())
}

//TODO add delegates to build gradle-like tasks