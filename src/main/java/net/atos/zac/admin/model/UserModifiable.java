/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

/**
 * Represents a Zaakafhandelparameters data that can be modified by a user.
 * <p>
 * This interface provides a contract for applying changes and comparing states,
 * commonly used in DTOs or entities that are updated from user input.
 *
 * @param <T> The implementing type, used for method chaining and type safety.
 */
public interface UserModifiable<T extends UserModifiable<T>> {
    /**
     * Checks whether the passed original is the same as the current object and if it was changed by the user.
     * <p>
     * The result is based on two checks done on:
     * <ul>
     * <li>one pivotal field that should be the same</li>
     * <li>one or more fields that should be different</li>
     * </ul>
     *
     * @param original Original object (part of {@link ZaakafhandelParameters}) to compare to.
     * @return <code>true</code> if the object is different from the original, <code>false</code> otherwise.
     */
    boolean isModifiedFrom(T original);

    /**
     * Applies the modifiable fields from a given source object to this object.
     * <p>
     * This method should only update fields that are intended to be changed by a user.
     *
     * @param changes An object containing the new values to apply.
     */
    void applyChanges(T changes);

    /**
     * Resets the persistent identity of the object.
     * <p>
     * Used to treat the object as a new entity, allowing the persistence layer (e.g., JPA)
     * to generate a new ID upon saving.
     *
     * @return This instance, for method chaining.
     */
    T resetId();
}
