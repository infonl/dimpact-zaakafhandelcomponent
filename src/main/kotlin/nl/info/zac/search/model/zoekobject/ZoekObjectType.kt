/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

enum class ZoekObjectType(val zoekObjectClass: Class<out ZoekObject>) {
    TAAK(TaakZoekObject::class.java),
    ZAAK(ZaakZoekObject::class.java),
    DOCUMENT(DocumentZoekObject::class.java)
}
