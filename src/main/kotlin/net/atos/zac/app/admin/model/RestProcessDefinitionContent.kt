/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestProcessDefinitionContent(
    var filename: String,
    var content: String
)
