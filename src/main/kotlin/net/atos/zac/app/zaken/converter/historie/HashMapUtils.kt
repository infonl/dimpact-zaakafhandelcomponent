package net.atos.zac.app.zaken.converter.historie

import net.atos.client.zgw.shared.util.JsonbUtil

fun HashMap<*, *>.getDiff(other: HashMap<*, *>): Map<Any, Pair<*, Any?>> =
    other.entries.mapNotNull {
        when (this.containsKey(it.key) && compare(it.value, this[it.key])) {
            true -> null
            else -> it.key to (this[it.key] to it.value)
        }
    }.toMap()

fun <T> HashMap<*, *>.getTypedValue(type: Class<T>): T? =
    JsonbUtil.JSONB.toJson(this)
        .let { JsonbUtil.JSONB.fromJson(it, type) }

fun compare(left: Any?, right: Any?): Boolean =
    when {
        left is HashMap<*, *> && right is HashMap<*, *> -> left.all { compare(it.value, right[it.key]) }
        left is ArrayList<*> && right is ArrayList<*> -> left.withIndex().all { compare(it.value, right[it.index]) }
        else -> left == right
    }

fun HashMap<*, *>.stringProperty(propName: String): String? =
    this[propName] as? String
