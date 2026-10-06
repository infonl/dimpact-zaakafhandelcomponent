/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestTaakStuurGegevens(
    var shouldSendMail: Boolean = false,

    var mail: String? = null
)
