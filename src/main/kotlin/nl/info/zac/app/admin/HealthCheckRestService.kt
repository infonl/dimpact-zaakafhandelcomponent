/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.app.admin.model.RestBuildInformation
import nl.info.zac.app.admin.model.RestInrichtingscheckParameters
import nl.info.zac.app.admin.model.RestInrichtingscheckSortField
import nl.info.zac.app.admin.model.RestInrichtingscheckStatus
import nl.info.zac.app.admin.model.RestReferentietabellenInrichtingscheck
import nl.info.zac.app.admin.model.RestZaaktypeInrichtingscheck
import nl.info.zac.app.admin.model.toRestInrichtingscheckZaaktype
import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.app.shared.takePage
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class HealthCheckRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService,
    private val healthCheckService: HealthCheckService,
    private val policyService: PolicyService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService
) {
    @GET
    @Path("zaaktypes")
    fun listZaaktypeInrichtingschecks(
        @Valid @BeanParam parameters: RestInrichtingscheckParameters
    ): RestResultaat<RestZaaktypeInrichtingscheck> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return listZaaktypesWithConfiguration()
            .map { (zaaktype, zaaktypeConfiguration) ->
                healthCheckService.controleerZaaktype(zaaktype, zaaktypeConfiguration)
            }
            .filter { it.matchesOmschrijving(parameters.omschrijving) && it.matchesStatus(parameters.status) }
            .sortedWith(comparatorOf(parameters.sortField, parameters.sortOrder))
            .let { checks ->
                RestResultaat(
                    results = checks.takePage(parameters).map(::convertToREST),
                    resultCount = checks.size.toLong()
                )
            }
    }

    @GET
    @Path("referentietabellen")
    fun readReferentietabellenInrichtingscheck(): RestReferentietabellenInrichtingscheck {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return RestReferentietabellenInrichtingscheck(
            isCommunicatiekanaalEformulierAanwezig = healthCheckService.bestaatCommunicatiekanaalEformulier()
        )
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

    private fun listZaaktypesWithConfiguration() =
        ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .filter { !it.concept }
            .filter { it.isNuGeldig() }
            .map { it to zaaktypeConfigurationService.findConfiguration(it.url.extractUuid()) }
            .filterNot { (_, zaaktypeConfiguration) -> zaaktypeConfiguration?.getProcessEngine() == ProcessEngine.BPMN }

    private fun ZaaktypeInrichtingscheck.matchesOmschrijving(omschrijving: String?) =
        omschrijving.isNullOrBlank() || zaaktype.omschrijving.contains(omschrijving.trim(), ignoreCase = true)

    private fun ZaaktypeInrichtingscheck.matchesStatus(status: RestInrichtingscheckStatus?) =
        when (status) {
            null -> true
            RestInrichtingscheckStatus.AANDACHT_NODIG -> !isValide || hasWaarschuwingen
            RestInrichtingscheckStatus.IN_ORDE -> isValide && !hasWaarschuwingen
        }

    private fun comparatorOf(
        restInrichtingscheckSortField: RestInrichtingscheckSortField,
        sorteerRichting: SorteerRichting
    ): Comparator<ZaaktypeInrichtingscheck> {
        val byOmschrijving = compareBy<ZaaktypeInrichtingscheck, String>(String.CASE_INSENSITIVE_ORDER) {
            it.zaaktype.omschrijving
        }
        val bySortField = when (restInrichtingscheckSortField) {
            RestInrichtingscheckSortField.OMSCHRIJVING -> byOmschrijving
            RestInrichtingscheckSortField.DOEL -> compareBy(nullsFirst(String.CASE_INSENSITIVE_ORDER)) {
                it.zaaktype.doel
            }
            RestInrichtingscheckSortField.BEGIN_GELDIGHEID -> compareBy { it.zaaktype.beginGeldigheid }
            RestInrichtingscheckSortField.VALIDE -> compareBy { it.isValide }
        }.thenComparing(byOmschrijving).thenComparing { check -> check.zaaktype.url }
        return if (sorteerRichting == SorteerRichting.DESC) bySortField.reversed() else bySortField
    }

    private fun convertToREST(check: ZaaktypeInrichtingscheck): RestZaaktypeInrichtingscheck =
        RestZaaktypeInrichtingscheck(
            zaaktype = check.zaaktype.toRestInrichtingscheckZaaktype(),
            isBesluittypeAanwezig = check.isBesluittypeAanwezig,
            resultaattypesMetVerplichtBesluit = check.resultaattypesMetVerplichtBesluit,
            resultaattypesMetNietOndersteundeBrondatum = check.resultaattypesMetNietOndersteundeBrondatum,
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
