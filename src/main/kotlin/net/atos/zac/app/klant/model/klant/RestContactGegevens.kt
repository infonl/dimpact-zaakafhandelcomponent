/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.klant

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestContactGegevens(
    var emailadres: String? = null,
    var telefoonnummer: String? = null
)
