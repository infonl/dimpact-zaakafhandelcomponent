/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

/**
 * Interface for Zaakafhandelparameters data that is used-facing and can be modified.
 *
 * @param <T> Type of the associated set, part of {@link ZaakafhandelParameters}
 */
public interface ZaakafhandelparametersComponent<T extends ZaakafhandelparametersComponent<T>> {
    /**
     * Checks whether the given object is different from the original only using the fields that are changeable by user.
     *
     * @param original Original object (part of {@link ZaakafhandelParameters}) to compare to.
     * @return <code>true</code> if the object is different from the original, <code>false</code> otherwise.
     */
    boolean isChanged(T original);

    /**
     * Modifies the object with the given changes.
     * <p/>
     * Not all fields are changeable by the user, so this method should only be used to apply user-facing changes to the object.
     *
     * @param changes Object containing the changes to apply to the object.
     */
    void modify(T changes);

    /**
     * Clears the id field of the object.
     * <p/>
     * Frontend UI might have stale data with old id. This method is used to clean this id when adding back an object, so that
     * JPA layer can generate a new one
     *
     * @return The sanitized object.
     */
    T clearId();
}
