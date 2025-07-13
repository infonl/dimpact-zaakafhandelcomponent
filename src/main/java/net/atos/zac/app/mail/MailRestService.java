/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail;

import static nl.info.zac.policy.PolicyServiceKt.assertPolicy;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.mail.converter.RESTMailGegevensConverter;
import net.atos.zac.app.mail.model.RESTMailGegevens;
import net.atos.zac.flowable.ZaakVariabelenService;
import nl.info.client.zgw.zrc.ZrcClientService;
import nl.info.client.zgw.zrc.model.generated.Zaak;
import nl.info.zac.mail.MailService;
import nl.info.zac.mail.model.BronnenKt;
import nl.info.zac.policy.PolicyService;
import nl.info.zac.zaak.ZaakService;

@Singleton
@Path("mail")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailRestService {
    private ZaakService zaakService;
    private MailService mailService;
    private ZaakVariabelenService zaakVariabelenService;
    private PolicyService policyService;
    private ZrcClientService zrcClientService;
    private RESTMailGegevensConverter restMailGegevensConverter;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public MailRestService() {
    }

    @Inject
    public MailRestService(
            final ZaakService zaakService,
            final MailService mailService,
            final ZaakVariabelenService zaakVariabelenService,
            final PolicyService policyService,
            final ZrcClientService zrcClientService,
            final RESTMailGegevensConverter restMailGegevensConverter
    ) {
        this.zaakService = zaakService;
        this.mailService = mailService;
        this.zaakVariabelenService = zaakVariabelenService;
        this.policyService = policyService;
        this.zrcClientService = zrcClientService;
        this.restMailGegevensConverter = restMailGegevensConverter;
    }

    @POST
    @Path("send/{zaakUuid}")
    public void sendMail(
            @PathParam("zaakUuid") final UUID zaakUUID,
            final RESTMailGegevens restMailGegevens
    ) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        assertPolicy(policyService.readZaakRechten(zaak).getVersturenEmail());
        mailService.sendMail(restMailGegevensConverter.convert(restMailGegevens), BronnenKt.getBronnenFromZaak(zaak));
    }

    @POST
    @Path("acknowledge/{zaakUuid}")
    public void sendAcknowledgmentReceiptMail(
            @PathParam("zaakUuid") final UUID zaakUuid,
            final RESTMailGegevens restMailGegevens
    ) {
        final Zaak zaak = zrcClientService.readZaak(zaakUuid);
        assertPolicy(!zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.getUuid()).orElse(false) &&
                     policyService.readZaakRechten(zaak).getVersturenOntvangstbevestiging());
        mailService.sendMail(restMailGegevensConverter.convert(restMailGegevens), BronnenKt.getBronnenFromZaak(zaak));
        zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak);
    }
}
