/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.zac.app.admin.model.RestBuildInformation
import nl.info.zac.app.admin.model.RestZaaktypeInrichtingscheck
import nl.info.zac.app.admin.model.toRestZaaktypeOverzicht
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class HealthCheckRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService,
    private val healthCheckService: HealthCheckService,
    private val policyService: PolicyService
) {
    @GET
    @Path("zaaktypes")
    fun listZaaktypeInrichtingschecks(): List<RestZaaktypeInrichtingscheck> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return listZaaktypes().map {
            convertToREST(healthCheckService.controleerZaaktype(it.url))
        }
    }

    @GET
    @Path("bestaat-communicatiekanaal-eformulier")
    fun readBestaatCommunicatiekanaalEformulier(): Boolean {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return healthCheckService.bestaatCommunicatiekanaalEformulier()
    }

    @DELETE
    @Path("ztc-cache")
    fun clearZTCCaches(): ZonedDateTime {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        ztcClientService.clearZaaktypeCache()
        ztcClientService.clearStatustypeCache()
        ztcClientService.clearResultaattypeCache()
        ztcClientService.clearInformatieobjecttypeCache()
        ztcClientService.clearZaaktypeInformatieobjecttypeCache()
        ztcClientService.clearBesluittypeCache()
        ztcClientService.clearRoltypeCache()
        ztcClientService.clearEigenschapCache()
        ztcClientService.clearCacheTime()
        return ztcClientService.resetCacheTimeToNow()
    }

    @GET
    @Path("ztc-cache")
    fun readZTCCacheTime(): ZonedDateTime {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return ztcClientService.resetCacheTimeToNow()
    }

    /**
     * Returns the ZAC build information. This information may be read by all ZAC users.
     */
    @GET
    @Path("build-informatie")
    fun readBuildInformatie() =
        healthCheckService.readBuildInformatie().let {
            RestBuildInformation(
                commit = it.commit,
                buildId = it.buildId,
                buildDatumTijd = it.buildDateTime,
                versienummer = it.versionNumber
            )
        }

    private fun listZaaktypes() =
        ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .filter { !it.concept }
            .filter { it.isNuGeldig() }

    private fun convertToREST(check: ZaaktypeInrichtingscheck): RestZaaktypeInrichtingscheck =
        RestZaaktypeInrichtingscheck(
            zaaktype = check.zaaktype.toRestZaaktypeOverzicht(),
            isBesluittypeAanwezig = check.isBesluittypeAanwezig,
            resultaattypesMetVerplichtBesluit = check.resultaattypesMetVerplichtBesluit,
            isResultaattypeAanwezig = check.isResultaattypeAanwezig,
            isInformatieobjecttypeEmailAanwezig = check.isInformatieobjecttypeEmailAanwezig,
            aantalBehandelaarroltypen = check.aantalBehandelaarroltypen,
            aantalInitiatorroltypen = check.aantalInitiatorroltypen,
            isRolOverigeAanwezig = check.isRolOverigeAanwezig,
            isStatustypeAfgerondAanwezig = check.isStatustypeAfgerondAanwezig,
            isStatustypeAfgerondLaatsteVolgnummer = check.isStatustypeAfgerondLaatsteVolgnummer,
            isStatustypeHeropendAanwezig = check.isStatustypeHeropendAanwezig,
            isStatustypeAanvullendeInformatieVereist = check.isStatustypeAanvullendeInformatieVereist,
            isStatustypeInBehandelingAanwezig = check.isStatustypeInBehandelingAanwezig,
            isStatustypeIntakeAanwezig = check.isStatustypeIntakeAanwezig,
            isZaakafhandelParametersValide = check.isZaakafhandelParametersValide,
            isBrpInstellingenCorrect = check.isBrpInstellingenCorrect,
            isZaakspecifiekeAutorisatieEigenschapAanwezig = check.isZaakspecifiekeAutorisatieEigenschapAanwezig,
            isZaakspecifiekeAutorisatieRoltypeAanwezig = check.isZaakspecifiekeAutorisatieRoltypeAanwezig,
            hasWaarschuwingen = check.hasWaarschuwingen,
            isValide = check.isValide
        )
}
