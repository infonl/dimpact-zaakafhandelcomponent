/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import nl.info.zac.admin.model.ZaaktypeMailtemplateParameters
import nl.info.zac.app.admin.model.RestMailtemplateKoppeling
import nl.info.zac.app.admin.model.toMailTemplate
import nl.info.zac.app.admin.model.toRestMailtemplate

fun ZaaktypeMailtemplateParameters.toRestMailtemplateKoppeling() = RestMailtemplateKoppeling(
    id = id,
    mailtemplate = checkNotNull(mailTemplate) { "Mailtemplate koppeling with id '$id' has no mailtemplate" }
        .toRestMailtemplate()
)

fun RestMailtemplateKoppeling.toZaaktypeMailtemplateParameters() = ZaaktypeMailtemplateParameters().apply {
    mailTemplate = this@toZaaktypeMailtemplateParameters.mailtemplate.toMailTemplate()
}
