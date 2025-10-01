/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mailtemplate;

import static nl.info.client.zgw.util.ZgwUriUtilsKt.extractUuid;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.admin.ZaaktypeCmmnConfigurationService;
import net.atos.zac.app.admin.converter.RESTMailtemplateConverter;
import net.atos.zac.app.admin.model.RESTMailtemplate;
import nl.info.client.zgw.zrc.ZrcClientService;
import nl.info.client.zgw.zrc.model.generated.Zaak;
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration;
import nl.info.zac.mailtemplates.MailTemplateService;
import nl.info.zac.mailtemplates.model.Mail;
import nl.info.zac.mailtemplates.model.MailTemplate;

@Singleton
@Path("mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateRESTService {

    @Inject
    private MailTemplateService mailTemplateService;

    @Inject
    private ZaaktypeCmmnConfigurationService zaaktypeCmmnConfigurationService;

    @Inject
    private ZrcClientService zrcClientService;

    @GET
    @Path("{mailtemplateEnum}/{zaakUUID}")
    public RESTMailtemplate findMailtemplate(
            @PathParam("mailtemplateEnum") final Mail mail,
            @PathParam("zaakUUID") final UUID zaakUUID
    ) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        final ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration = zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(
                extractUuid(
                        zaak.getZaaktype()));

        return zaaktypeCmmnConfiguration.getMailtemplateKoppelingen().stream()
                .filter(koppeling -> koppeling.getMailTemplate().mail.equals(mail))
                .map(koppeling -> RESTMailtemplateConverter.convert(koppeling.getMailTemplate()))
                .findFirst()
                .orElseGet(() -> {
                    MailTemplate mailTemplate = mailTemplateService.findDefaultMailtemplate(mail);
                    return mailTemplate != null ? RESTMailtemplateConverter.convert(mailTemplate) : null;
                });
    }
}
