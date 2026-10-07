/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

/**
 * Zaaktype configuration data that a user can modify.
 */
interface UserModifiableZaaktypeConfigurationData<T : UserModifiableZaaktypeConfigurationData<T>> {
    /**
     * Copies the fields that a user can modify from [changes] to this object.
     */
    fun applyChanges(changes: T)

    /**
     * Clears the id, so that JPA persists this object as a new row.
     */
    fun resetId(): T
}

/**
 * Makes this set match [desired], where [key] identifies an element: removes the elements whose key is not desired,
 * applies the changes of the desired element to each element with the same key, and adds the desired elements whose
 * key is new. Existing elements keep their id, so that JPA updates their rows instead of deleting and inserting them.
 */
fun <T : UserModifiableZaaktypeConfigurationData<T>, K> MutableSet<T>.mergeWith(desired: Collection<T>, key: (T) -> K) {
    val desiredByKey = desired.associateBy(key)
    require(desiredByKey.size == desired.size) { "Desired elements have duplicate keys" }
    removeIf { key(it) !in desiredByKey }
    forEach { desiredByKey.getValue(key(it)).let(it::applyChanges) }
    val existingKeys = map(key).toSet()
    desiredByKey.filterKeys { it !in existingKeys }.values.forEach { add(it.resetId()) }
}
