/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.extensions.ZaakTypeExtensionsKt;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.app.admin.converter.RESTZaaktypeOverzichtConverter;
import net.atos.zac.app.admin.model.RESTBuildInformatie;
import net.atos.zac.app.admin.model.RESTZaaktypeInrichtingscheck;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.healthcheck.HealthCheckService;
import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck;

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckRestService {

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private ConfiguratieService configuratieService;

    @Inject
    private HealthCheckService healthCheckService;

    @GET
    @Path("zaaktypes")
    public List<RESTZaaktypeInrichtingscheck> listZaaktypeInrichtingschecks() {
        return listZaaktypes().stream().map(zaaktype -> convertToREST(healthCheckService.controleerZaaktype(zaaktype.getUrl()))).toList();
    }

    @GET
    @Path("bestaat-communicatiekanaal-eformulier")
    public boolean readBestaatCommunicatiekanaalEformulier() {
        return healthCheckService.bestaatCommunicatiekanaalEformulier();
    }

    @DELETE
    @Path("ztc-cache")
    public ZonedDateTime clearZTCCaches() {
        ztcClientService.clearZaaktypeCache();
        ztcClientService.clearStatustypeCache();
        ztcClientService.clearResultaattypeCache();
        ztcClientService.clearInformatieobjecttypeCache();
        ztcClientService.clearZaaktypeInformatieobjecttypeCache();
        ztcClientService.clearBesluittypeCache();
        ztcClientService.clearRoltypeCache();
        ztcClientService.clearCacheTime();
        return ztcClientService.resetCacheTimeToNow();
    }

    @GET
    @Path("ztc-cache")
    public ZonedDateTime readZTCCacheTime() {
        return ztcClientService.resetCacheTimeToNow();
    }

    @GET
    @Path("build-informatie")
    public RESTBuildInformatie readBuildInformatie() {
        return new RESTBuildInformatie(healthCheckService.readBuildInformatie());
    }

    private List<ZaakType> listZaaktypes() {
        return ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI()).stream()
                .filter(zaaktype -> !zaaktype.getConcept())
                .filter(ZaakTypeExtensionsKt::isNuGeldig)
                .toList();
    }

    private RESTZaaktypeInrichtingscheck convertToREST(final ZaaktypeInrichtingscheck check) {
        final RESTZaaktypeInrichtingscheck restCheck = new RESTZaaktypeInrichtingscheck();
        restCheck.zaaktype = RESTZaaktypeOverzichtConverter.convert(check.getZaaktype());
        restCheck.besluittypeAanwezig = check.isBesluittypeAanwezig();
        restCheck.resultaattypesMetVerplichtBesluit = check.getResultaattypesMetVerplichtBesluit();
        restCheck.resultaattypeAanwezig = check.isResultaattypeAanwezig();
        restCheck.informatieobjecttypeEmailAanwezig = check.isInformatieobjecttypeEmailAanwezig();
        restCheck.rolBehandelaarAanwezig = check.isRolBehandelaarAanwezig();
        restCheck.rolInitiatorAanwezig = check.isRolInitiatorAanwezig();
        restCheck.rolOverigeAanwezig = check.isRolOverigeAanwezig();
        restCheck.statustypeAfgerondAanwezig = check.isStatustypeAfgerondAanwezig();
        restCheck.statustypeAfgerondLaatsteVolgnummer = check.isStatustypeAfgerondLaatsteVolgnummer();
        restCheck.statustypeHeropendAanwezig = check.isStatustypeHeropendAanwezig();
        restCheck.statustypeInBehandelingAanwezig = check.isStatustypeInBehandelingAanwezig();
        restCheck.statustypeIntakeAanwezig = check.isStatustypeIntakeAanwezig();
        restCheck.zaakafhandelParametersValide = check.isZaakafhandelParametersValide();
        restCheck.valide = check.isValide();
        return restCheck;
    }
}
