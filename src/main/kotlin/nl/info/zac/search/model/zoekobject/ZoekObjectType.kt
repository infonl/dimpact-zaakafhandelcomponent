/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

enum class ZoekObjectType(val zoekObjectClass: Class<out ZoekObject>) {
    // The order is important here.
    // For a full reindex we always want to reindex zaken first, before taken and documenten.
    ZAAK(ZaakZoekObject::class.java),
    TAAK(TaakZoekObject::class.java),
    DOCUMENT(DocumentZoekObject::class.java)
}
