/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.admin.converter.RESTZaaktypeOverzichtConverter
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.zac.app.admin.model.RESTBuildInformation
import nl.info.zac.app.admin.model.RESTZaaktypeInrichtingscheck
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class HealthCheckRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configuratieService: ConfiguratieService,
    private val healthCheckService: HealthCheckService,
    private val policyService: PolicyService
) {
    @GET
    @Path("zaaktypes")
    fun listZaaktypeInrichtingschecks(): List<RESTZaaktypeInrichtingscheck> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return listZaaktypes().map {
            convertToREST(healthCheckService.controleerZaaktype(it.url))
        }
    }

    @GET
    @Path("bestaat-communicatiekanaal-eformulier")
    fun readBestaatCommunicatiekanaalEformulier() =
        healthCheckService.bestaatCommunicatiekanaalEformulier()

    @DELETE
    @Path("ztc-cache")
    fun clearZTCCaches(): ZonedDateTime {
        ztcClientService.clearZaaktypeCache()
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
    fun readZTCCacheTime() = ztcClientService.resetCacheTimeToNow()

    @GET
    @Path("build-informatie")
    fun readBuildInformatie() =
        healthCheckService.readBuildInformatie().let {
            RESTBuildInformation(it.commit, it.buildId, it.buildDatumTijd, it.versienummer)
        }

    private fun listZaaktypes() =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .filter { !it.concept }
            .filter { it.isNuGeldig() }

    private fun convertToREST(check: ZaaktypeInrichtingscheck): RESTZaaktypeInrichtingscheck =
        RESTZaaktypeInrichtingscheck(
            zaaktype = RESTZaaktypeOverzichtConverter.convert(check.zaaktype),
            besluittypeAanwezig = check.isBesluittypeAanwezig,
            resultaattypesMetVerplichtBesluit = check.resultaattypesMetVerplichtBesluit,
            resultaattypeAanwezig = check.isResultaattypeAanwezig,
            informatieobjecttypeEmailAanwezig = check.isInformatieobjecttypeEmailAanwezig,
            rolBehandelaarAanwezig = check.isRolBehandelaarAanwezig,
            rolInitiatorAanwezig = check.isRolInitiatorAanwezig,
            rolOverigeAanwezig = check.isRolOverigeAanwezig,
            statustypeAfgerondAanwezig = check.isStatustypeAfgerondAanwezig,
            statustypeAfgerondLaatsteVolgnummer = check.isStatustypeAfgerondLaatsteVolgnummer,
            statustypeHeropendAanwezig = check.isStatustypeHeropendAanwezig,
            statustypeAanvullendeInformatieVereist = check.isStatustypeAanvullendeInformatieVereist,
            statustypeInBehandelingAanwezig = check.isStatustypeInBehandelingAanwezig,
            statustypeIntakeAanwezig = check.isStatustypeIntakeAanwezig,
            zaakafhandelParametersValide = check.isZaakafhandelParametersValide,
            valide = check.isValide
        )
}
