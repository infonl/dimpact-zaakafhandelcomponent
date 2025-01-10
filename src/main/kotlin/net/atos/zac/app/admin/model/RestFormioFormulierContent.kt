/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
class RestFormioFormulierContent(
    var filename: String,
    var content: String
)
