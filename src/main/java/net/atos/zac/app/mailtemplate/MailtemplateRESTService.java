/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mailtemplate;

import static net.atos.zac.util.UriUtilsKt.extractUuid;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.admin.ZaakafhandelParameterService;
import net.atos.zac.admin.model.ZaakafhandelParameters;
import net.atos.zac.app.admin.converter.RESTMailtemplateConverter;
import net.atos.zac.app.admin.model.RESTMailtemplate;
import net.atos.zac.mailtemplates.MailTemplateService;
import net.atos.zac.mailtemplates.model.Mail;

@Singleton
@Path("mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateRESTService {

    @Inject
    private MailTemplateService mailTemplateService;

    @Inject
    private ZaakafhandelParameterService zaakafhandelParameterService;

    @Inject
    private ZrcClientService zrcClientService;

    @GET
    @Path("{mailtemplateEnum}/{zaakUUID}")
    public RESTMailtemplate findMailtemplate(
            @PathParam("mailtemplateEnum") final Mail mail,
            @PathParam("zaakUUID") final UUID zaakUUID
    ) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(extractUuid(
                zaak.getZaaktype()));

        return zaakafhandelParameters.getMailtemplateKoppelingen().stream()
                .filter(koppeling -> koppeling.getMailTemplate().getMail().equals(mail))
                .map(koppeling -> RESTMailtemplateConverter.convert(koppeling.getMailTemplate()))
                .findFirst()
                .orElseGet(() -> mailTemplateService.findDefaultMailtemplate(mail)
                        .map(RESTMailtemplateConverter::convert)
                        .orElse(null)
                );
    }
}
