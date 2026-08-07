/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

/**
 * ObjectAdres
 */
abstract class ObjectBagObject(
    /**
     * De unieke identificatie van het OBJECT
     * - maxLength: 100
     * - required
     */
    open var identificatie: String? = null
)
