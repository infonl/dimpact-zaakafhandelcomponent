/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.mail;

import static net.atos.client.zgw.zrc.util.StatusTypeUtil.isHeropend;
import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter;
import net.atos.zac.app.mail.model.RESTMailGegevens;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.policy.PolicyService;
import nl.info.client.zgw.ztc.ZtcClientService;
import nl.info.client.zgw.ztc.model.generated.StatusType;
import nl.info.zac.mail.MailService;
import nl.info.zac.mail.model.BronnenKt;

@Singleton
@Path("mail")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailRestService {
    private MailService mailService;
    private ZaakVariabelenService zaakVariabelenService;
    private PolicyService policyService;
    private ZrcClientService zrcClientService;
    private ZtcClientService ztcClientService;
    private RESTMailGegevensConverter restMailGegevensConverter;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public MailRestService() {
    }

    @Inject
    public MailRestService(
            final MailService mailService,
            final ZaakVariabelenService zaakVariabelenService,
            final PolicyService policyService,
            final ZrcClientService zrcClientService,
            final ZtcClientService ztcClientService,
            final RESTMailGegevensConverter restMailGegevensConverter
    ) {
        this.mailService = mailService;
        this.zaakVariabelenService = zaakVariabelenService;
        this.policyService = policyService;
        this.zrcClientService = zrcClientService;
        this.ztcClientService = ztcClientService;
        this.restMailGegevensConverter = restMailGegevensConverter;
    }

    @POST
    @Path("send/{zaakUuid}")
    public void sendMail(
            @PathParam("zaakUuid") final UUID zaakUUID,
            final RESTMailGegevens restMailGegevens
    ) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        assertPolicy(policyService.readZaakRechten(zaak).versturenEmail());
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
                     policyService.readZaakRechten(zaak).versturenOntvangstbevestiging());
        mailService.sendMail(restMailGegevensConverter.convert(restMailGegevens), BronnenKt.getBronnenFromZaak(zaak));

        final StatusType statustype = zaak.getStatus() != null ?
                ztcClientService.readStatustype(zrcClientService.readStatus(zaak.getStatus()).getStatustype()) : null;
        if (!isHeropend(statustype)) {
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaakUuid, Boolean.TRUE);
        }
    }
}
