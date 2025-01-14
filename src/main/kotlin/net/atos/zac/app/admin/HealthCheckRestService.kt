/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.admin.converter.RESTZaaktypeOverzichtConverter
import net.atos.zac.app.admin.model.RESTBuildInformatie
import net.atos.zac.app.admin.model.RESTZaaktypeInrichtingscheck
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck
import java.time.ZonedDateTime

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class HealthCheckRestService {
    @Inject
    private val ztcClientService: ZtcClientService? = null

    @Inject
    private val configuratieService: ConfiguratieService? = null

    @Inject
    private val healthCheckService: HealthCheckService? = null

    @GET
    @Path("zaaktypes")
    fun listZaaktypeInrichtingschecks(): MutableList<RESTZaaktypeInrichtingscheck?> {
        return listZaaktypes().stream().map<RESTZaaktypeInrichtingscheck?> { zaaktype: ZaakType? ->
            convertToREST(
                healthCheckService!!.controleerZaaktype(zaaktype!!.getUrl())
            )
        }.toList()
    }

    @GET
    @Path("bestaat-communicatiekanaal-eformulier")
    fun readBestaatCommunicatiekanaalEformulier(): Boolean {
        return healthCheckService!!.bestaatCommunicatiekanaalEformulier()
    }

    @DELETE
    @Path("ztc-cache")
    fun clearZTCCaches(): ZonedDateTime {
        ztcClientService!!.clearZaaktypeCache()
        ztcClientService.clearStatustypeCache()
        ztcClientService.clearResultaattypeCache()
        ztcClientService.clearInformatieobjecttypeCache()
        ztcClientService.clearZaaktypeInformatieobjecttypeCache()
        ztcClientService.clearBesluittypeCache()
        ztcClientService.clearRoltypeCache()
        ztcClientService.clearCacheTime()
        return ztcClientService.resetCacheTimeToNow()
    }

    @GET
    @Path("ztc-cache")
    fun readZTCCacheTime(): ZonedDateTime {
        return ztcClientService!!.resetCacheTimeToNow()
    }

    @GET
    @Path("build-informatie")
    fun readBuildInformatie(): RESTBuildInformatie {
        return RESTBuildInformatie(healthCheckService!!.readBuildInformatie())
    }

    private fun listZaaktypes(): MutableList<ZaakType?> {
        return ztcClientService!!.listZaaktypen(configuratieService!!.readDefaultCatalogusURI()).stream()
            .filter { zaaktype: ZaakType? -> !zaaktype!!.getConcept() }
            .filter { isNuGeldig() }
            .toList()
    }

    private fun convertToREST(check: ZaaktypeInrichtingscheck): RESTZaaktypeInrichtingscheck {
        val restCheck = RESTZaaktypeInrichtingscheck()
        restCheck.zaaktype = RESTZaaktypeOverzichtConverter.convert(check.zaaktype)
        restCheck.besluittypeAanwezig = check.isBesluittypeAanwezig
        restCheck.resultaattypesMetVerplichtBesluit = check.resultaattypesMetVerplichtBesluit
        restCheck.resultaattypeAanwezig = check.isResultaattypeAanwezig
        restCheck.informatieobjecttypeEmailAanwezig = check.isInformatieobjecttypeEmailAanwezig
        restCheck.rolBehandelaarAanwezig = check.isRolBehandelaarAanwezig
        restCheck.rolInitiatorAanwezig = check.isRolInitiatorAanwezig
        restCheck.rolOverigeAanwezig = check.isRolOverigeAanwezig
        restCheck.statustypeAfgerondAanwezig = check.isStatustypeAfgerondAanwezig
        restCheck.statustypeAfgerondLaatsteVolgnummer = check.isStatustypeAfgerondLaatsteVolgnummer
        restCheck.statustypeHeropendAanwezig = check.isStatustypeHeropendAanwezig
        restCheck.statustypeAanvullendeInformatieVereist = check.isStatustypeAanvullendeInformatieVereist
        restCheck.statustypeInBehandelingAanwezig = check.isStatustypeInBehandelingAanwezig
        restCheck.statustypeIntakeAanwezig = check.isStatustypeIntakeAanwezig
        restCheck.zaakafhandelParametersValide = check.isZaakafhandelParametersValide
        restCheck.valide = check.isValide
        return restCheck
    }
}
