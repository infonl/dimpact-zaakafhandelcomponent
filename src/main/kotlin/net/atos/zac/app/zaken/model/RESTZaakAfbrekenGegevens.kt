/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTZaakAfbrekenGegevens(
    var zaakbeeindigRedenId: Long
)
