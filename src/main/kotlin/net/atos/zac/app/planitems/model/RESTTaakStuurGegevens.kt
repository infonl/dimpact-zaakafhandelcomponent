/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RESTTaakStuurGegevens(
    var sendMail: Boolean = false,

    var mail: String? = null
)
