/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestBpmnProcessDefinition(
    var id: String,
    var name: String,
    var version: Int,
    var key: String
)
