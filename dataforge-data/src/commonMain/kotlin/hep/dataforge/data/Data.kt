package hep.dataforge.data

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * A data element characterized by its meta
 */
interface Data<out T : Any> : Goal<T>, MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    val type: KClass<out T>
    /**
     * Meta for the data
     */
    val meta: Meta

    override fun toMeta(): Meta = meta

    companion object {
        const val TYPE = "data"

        operator fun <T : Any> invoke(
            type: KClass<out T>,
            meta: Meta = EmptyMeta,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            block: suspend CoroutineScope.() -> T
        ): Data<T> = DynamicData(type, meta, context, dependencies, block)

        operator inline fun <reified T : Any> invoke(
            meta: Meta = EmptyMeta,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            noinline block: suspend CoroutineScope.() -> T
        ): Data<T> = invoke(T::class, meta, context, dependencies, block)

        operator fun <T : Any> invoke(
            name: String,
            type: KClass<out T>,
            meta: Meta = EmptyMeta,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            block: suspend CoroutineScope.() -> T
        ): Data<T> = NamedData(name, invoke(type, meta, context, dependencies, block))

        operator inline fun <reified T : Any> invoke(
            name: String,
            meta: Meta = EmptyMeta,
            context: CoroutineContext = EmptyCoroutineContext,
            dependencies: Collection<Data<*>> = emptyList(),
            noinline block: suspend CoroutineScope.() -> T
        ): Data<T> =
            invoke(name, T::class, meta, context, dependencies, block)

        fun <T : Any> static(value: T, meta: Meta = EmptyMeta): Data<T> =
            StaticData(value, meta)
    }
}


fun <R : Any, T : R> Data<T>.cast(type: KClass<R>): Data<R> {
    return object : Data<R> by this {
        override val type: KClass<out R> = type
    }
}

/**
 * Upcast a [Data] to a supertype
 */
inline fun <reified R : Any, T : R> Data<T>.cast(): Data<R> = cast(R::class)


class DynamicData<T : Any>(
    override val type: KClass<out T>,
    override val meta: Meta = EmptyMeta,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend CoroutineScope.() -> T
) : Data<T>, DynamicGoal<T>(context, dependencies, block)

class StaticData<T : Any>(
    value: T,
    override val meta: Meta = EmptyMeta
) : Data<T>, StaticGoal<T>(value) {
    override val type: KClass<out T> get() = value::class
}

class NamedData<out T : Any>(val name: String, data: Data<T>) : Data<T> by data

fun <T : Any, R : Any> Data<T>.pipe(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    block: suspend CoroutineScope.(T) -> R
): Data<R> = DynamicData(outputType, meta, coroutineContext, listOf(this)) {
    block(await(this))
}


/**
 * Create a data pipe
 */
inline fun <T : Any, reified R : Any> Data<T>.pipe(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta = this.meta,
    noinline block: suspend CoroutineScope.(T) -> R
): Data<R> = DynamicData(R::class, meta, coroutineContext, listOf(this)) {
    block(await(this))
}

/**
 * Create a joined data.
 */
inline fun <T : Any, reified R : Any> Collection<Data<T>>.join(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    noinline block: suspend CoroutineScope.(Collection<T>) -> R
): Data<R> = DynamicData(
    R::class,
    meta,
    coroutineContext,
    this
) {
    block(map { this.run { it.await(this) } })
}

fun <K, T : Any, R : Any> Map<K, Data<T>>.join(
    outputType: KClass<out R>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    block: suspend CoroutineScope.(Map<K, T>) -> R
): DynamicData<R> = DynamicData(
    outputType,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await(this) })
}


/**
 * A joining of multiple data into a single one
 * @param K type of the map key
 * @param T type of the input goal
 * @param R type of the result goal
 */
inline fun <K, T : Any, reified R : Any> Map<K, Data<T>>.join(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    meta: Meta,
    noinline block: suspend CoroutineScope.(Map<K, T>) -> R
): DynamicData<R> = DynamicData(
    R::class,
    meta,
    coroutineContext,
    this.values
) {
    block(mapValues { it.value.await(this) })
}


