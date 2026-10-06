/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import nl.info.zac.app.admin.model.toMailTemplate
import nl.info.zac.app.admin.model.toRestMailtemplate

fun ZaaktypeCmmnMailtemplateParameters.toRestMailtemplateKoppeling() = RESTMailtemplateKoppeling().apply {
    id = this@toRestMailtemplateKoppeling.id
    mailtemplate = this@toRestMailtemplateKoppeling.mailTemplate?.toRestMailtemplate()
}

fun RESTMailtemplateKoppeling.toZaaktypeCmmnMailtemplateParameters() = ZaaktypeCmmnMailtemplateParameters().apply {
    mailTemplate = this@toZaaktypeCmmnMailtemplateParameters.mailtemplate.toMailTemplate()
}
