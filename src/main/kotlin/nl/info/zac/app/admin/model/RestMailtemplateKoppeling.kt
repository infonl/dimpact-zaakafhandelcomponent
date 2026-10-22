/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestMailtemplateKoppeling(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaaktypeConfiguration? = null,
    var mailtemplate: RestMailtemplate
)
