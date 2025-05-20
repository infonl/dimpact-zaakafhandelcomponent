/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

data class RestOverigeRechten(
    val startenZaak: Boolean,
    val beheren: Boolean,
    val zoeken: Boolean
)
