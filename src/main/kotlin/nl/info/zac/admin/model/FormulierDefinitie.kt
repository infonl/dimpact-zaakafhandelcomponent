/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

enum class FormulierDefinitie(val veldDefinities: Set<FormulierVeldDefinitie>) {
    DEFAULT_TAAKFORMULIER(emptySet()),
    AANVULLENDE_INFORMATIE(emptySet()),
    ADVIES(setOf(FormulierVeldDefinitie.ADVIES)),
    EXTERN_ADVIES_VASTLEGGEN(emptySet()),
    EXTERN_ADVIES_MAIL(emptySet()),
    GOEDKEUREN(emptySet()),
    DOCUMENT_VERZENDEN_POST(emptySet())
}
