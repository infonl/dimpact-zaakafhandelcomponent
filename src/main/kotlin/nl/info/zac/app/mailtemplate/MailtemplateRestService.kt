/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.mailtemplate

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.admin.model.RestMailtemplate
import nl.info.zac.app.admin.model.toRestMailtemplate
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class MailtemplateRestService @Inject constructor(
    private val mailTemplateService: MailTemplateService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val zrcClientService: ZrcClientService
) {
    @GET
    @Path("{mailtemplateEnum}/{zaakUUID}")
    fun findMailtemplate(
        @PathParam("mailtemplateEnum") mail: Mail,
        @PathParam("zaakUUID") zaakUUID: UUID
    ): RestMailtemplate? {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return zaaktypeConfigurationService.findConfiguration(zaak.zaaktype.extractUuid())
            ?.getMailtemplateKoppelingen()
            .orEmpty()
            .mapNotNull { it.mailTemplate }
            .firstOrNull { it.mail == mail }
            ?.toRestMailtemplate()
            ?: mailTemplateService.findDefaultMailtemplate(mail)?.toRestMailtemplate()
    }
}
