package hep.dataforge.workspace

import hep.dataforge.context.Named
import hep.dataforge.data.DataNode
import hep.dataforge.descriptors.Described
import hep.dataforge.meta.Meta
import hep.dataforge.provider.Type
import hep.dataforge.workspace.Task.Companion.TYPE
import kotlin.reflect.KClass

@Type(TYPE)
interface Task<out R : Any> : Named, Described {
    /**
     * Terminal task is the one that could not build model lazily
     */
    val isTerminal: Boolean get() = false

    /**
     * The explicit type of the node returned by the task
     */
    val type: KClass<out R>

    /**
     * Build a model for this task
     *
     * @param workspace
     * @param taskConfig
     * @return
     */
    fun build(workspace: Workspace, taskConfig: Meta): TaskModel

    /**
     * Check if the model is valid and is acceptable by the task. Throw exception if not.
     *
     * @param model
     */
    fun validate(model: TaskModel) {
        if(this.name != model.name) error("The task $name could not be run with model from task ${model.name}")
    }

    /**
     * Run given task model. Type check expected to be performed before actual
     * calculation.
     *
     * @param workspace - a workspace to run task model in
     * @param model - a model to be executed
     * @return
     */
    fun run(workspace: Workspace, model: TaskModel): DataNode<R>

    companion object {
        const val TYPE = "task"
    }
}