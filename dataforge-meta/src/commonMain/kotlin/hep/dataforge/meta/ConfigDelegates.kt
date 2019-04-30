package hep.dataforge.meta

import hep.dataforge.values.Value
import kotlin.jvm.JvmName


//Configurable delegates

/**
 * A property delegate that uses custom key
 */
fun <T> Configurable.value(default: T, key: String? = null) =
    MutableValueDelegate(config, key, Value.of(default))

fun <T> Configurable.value(default: T? = null, key: String? = null, transform: (Value?) -> T) =
    MutableValueDelegate(config, key, Value.of(default)).transform(reader = transform)

fun Configurable.stringList(key: String? = null) =
    value { it?.list?.map { value -> value.string } ?: emptyList() }

fun Configurable.numberList(key: String? = null) =
    value { it?.list?.map { value -> value.number } ?: emptyList() }

fun Configurable.string(default: String? = null, key: String? = null) =
    MutableStringDelegate(config, key, default)

fun Configurable.boolean(default: Boolean? = null, key: String? = null) =
    MutableBooleanDelegate(config, key, default)

fun Configurable.number(default: Number? = null, key: String? = null) =
    MutableNumberDelegate(config, key, default)

/* Number delegates*/

fun Configurable.int(default: Int? = null, key: String? = null) =
    number(default, key).int

fun Configurable.double(default: Double? = null, key: String? = null) =
    number(default, key).double

fun Configurable.long(default: Long? = null, key: String? = null) =
    number(default, key).long

fun Configurable.short(default: Short? = null, key: String? = null) =
    number(default, key).short

fun Configurable.float(default: Float? = null, key: String? = null) =
    number(default, key).float


@JvmName("safeString")
fun Configurable.string(default: String, key: String? = null) =
    MutableSafeStringDelegate(config, key) { default }

@JvmName("safeBoolean")
fun Configurable.boolean(default: Boolean, key: String? = null) =
    MutableSafeBooleanDelegate(config, key) { default }

@JvmName("safeNumber")
fun Configurable.number(default: Number, key: String? = null) =
    MutableSafeNumberDelegate(config, key) { default }

@JvmName("safeString")
fun Configurable.string(key: String? = null, default: () -> String) =
    MutableSafeStringDelegate(config, key, default)

@JvmName("safeBoolean")
fun Configurable.boolean(key: String? = null, default: () -> Boolean) =
    MutableSafeBooleanDelegate(config, key, default)

@JvmName("safeNumber")
fun Configurable.number(key: String? = null, default: () -> Number) =
    MutableSafeNumberDelegate(config, key, default)


/* Safe number delegates*/

@JvmName("safeInt")
fun Configurable.int(default: Int, key: String? = null) =
    number(default, key).int

@JvmName("safeDouble")
fun Configurable.double(default: Double, key: String? = null) =
    number(default, key).double

@JvmName("safeLong")
fun Configurable.long(default: Long, key: String? = null) =
    number(default, key).long

@JvmName("safeShort")
fun Configurable.short(default: Short, key: String? = null) =
    number(default, key).short

@JvmName("safeFloat")
fun Configurable.float(default: Float, key: String? = null) =
    number(default, key).float

/**
 * Enum delegate
 */
inline fun <reified E : Enum<E>> Configurable.enum(default: E, key: String? = null) =
    MutableSafeEnumvDelegate(config, key, default) { enumValueOf(it) }

/* Node delegates */

fun Configurable.node(key: String? = null) = MutableNodeDelegate(config, key)

fun <T : Specific> Configurable.spec(spec: Specification<T>, key: String? = null) =
    MutableMorphDelegate(config, key) { spec.wrap(it) }

fun <T : Specific> Configurable.spec(builder: (Config) -> T, key: String? = null) =
    MutableMorphDelegate(config, key) { specification(builder).wrap(it) }
