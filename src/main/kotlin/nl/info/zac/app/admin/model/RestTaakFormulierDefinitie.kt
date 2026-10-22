/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

data class RestTaakFormulierDefinitie(
    val id: String,
    val veldDefinities: List<RestTaakFormulierVeldDefinitie>
)
