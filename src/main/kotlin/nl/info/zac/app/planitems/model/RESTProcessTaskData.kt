/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RESTProcessTaskData(
    var planItemInstanceId: String,

    var data: Map<String, Any>
)
