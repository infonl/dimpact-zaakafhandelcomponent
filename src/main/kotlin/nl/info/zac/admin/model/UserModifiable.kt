/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

/**
 * Represents a ZaaktypeCmmnConfiguration data that can be modified by a user.
 * <p>
 * This interface provides a contract for applying changes and comparing states,
 * commonly used in DTOs or entities that are updated from user input.
 *
 * @param <T> The implementing type, used for method chaining and type safety.
 */
interface UserModifiable<T : UserModifiable<T>> {

    /**
     * Checks whether the passed original is the same as the current object and if it was changed by the user.
     *
     * The result is based on two checks:
     *  - one pivotal field that should be the same
     *  - one or more fields that should be different
     *
     * @param original Original object (part of [ZaaktypeCmmnConfiguration]) to compare to.
     * @return `true` if the object is different from the original, `false` otherwise.
     */
    fun isModifiedFrom(original: T): Boolean

    /**
     * Applies the modifiable fields from a given source object to this object.
     * This should only update fields that are intended to be changed by a user.
     *
     * @param changes An object containing the new values to apply.
     */
    fun applyChanges(changes: T)

    /**
     * Resets the persistent identity of the object.
     * Used to treat the object as a new entity, allowing the persistence layer (e.g., JPA)
     * to generate a new ID upon saving.
     *
     * @return This instance, for method chaining.
     */
    fun resetId(): T
}
