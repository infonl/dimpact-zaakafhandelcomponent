package net.atos.zac.app.zaken.converter.historie

import net.atos.client.zgw.shared.util.JsonbUtil

fun Map<*, *>.diff(other: Map<*, *>): Map<Any?, Pair<Any?, Any?>> = other
    .filterNot { (key, value) -> this.containsKey(key) && compare(value, this[key]) }
    .mapValues { (key, value) -> this[key] to value }

private fun compare(left: Any?, right: Any?): Boolean =
    when {
        left is Map<*, *> && right is Map<*, *> -> left.all { right.containsKey(it.key) && compare(it.value, right[it.key]) }
        left is List<*> && right is List<*> -> left.withIndex().all {
            right.size > it.index && compare(it.value, right[it.index])
        }
        else -> left == right
    }

fun <T> Map<*, *>.getTypedValue(type: Class<T>): T? =
    JsonbUtil.JSONB.toJson(this)
        .let { JsonbUtil.JSONB.fromJson(it, type) }

fun Map<*, *>.stringProperty(propName: String): String? =
    this[propName] as? String
