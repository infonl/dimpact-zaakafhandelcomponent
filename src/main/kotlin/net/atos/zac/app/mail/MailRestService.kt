/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter
import net.atos.zac.app.mail.model.RESTMailGegevens
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import java.util.UUID

@Singleton
@Path("mail")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class MailRestService @Inject constructor(
    private val zaakService: ZaakService,
    private val mailService: MailService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val policyService: PolicyService,
    private val zrcClientService: ZrcClientService,
    private val restMailGegevensConverter: RESTMailGegevensConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @POST
    @Path("send/{zaakUuid}")
    fun sendMail(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @Valid restMailGegevens: RESTMailGegevens
    ) {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, loggedInUser).versturenEmail)
        mailService.sendMail(restMailGegevensConverter.convert(restMailGegevens), zaak.getBronnenFromZaak())
    }

    @POST
    @Path("acknowledge/{zaakUuid}")
    fun sendAcknowledgmentReceiptMail(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @Valid restMailGegevens: RESTMailGegevens
    ) {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(zaakUuid)
        val ontvangstbevestigingVerstuurd = zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid) == true
        assertPolicy(
            !ontvangstbevestigingVerstuurd &&
                policyService.readZaakRechten(zaak, loggedInUser).versturenOntvangstbevestiging
        )
        // an ontvangstbevestiging is always Openbaar, regardless of what the caller supplied
        restMailGegevens.vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.OPENBAAR
        mailService.sendMail(restMailGegevensConverter.convert(restMailGegevens), zaak.getBronnenFromZaak())
        zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)
    }
}
