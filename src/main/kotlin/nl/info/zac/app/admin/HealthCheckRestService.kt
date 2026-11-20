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
import nl.info.zac.app.admin.model.RestBuildInformation
import nl.info.zac.app.admin.model.RestInrichtingscheckParameters
import nl.info.zac.app.admin.model.RestReferentietabellenInrichtingscheck
import nl.info.zac.app.admin.model.RestZaaktypeInrichtingscheck
import nl.info.zac.app.admin.model.toRestZaaktypeInrichtingscheckResultaat
import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("health-check")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class HealthCheckRestService @Inject constructor(
    private val healthCheckService: HealthCheckService,
    private val policyService: PolicyService
) {
    @GET
    @Path("zaaktypes")
    fun listZaaktypeInrichtingschecks(
        @Valid @BeanParam parameters: RestInrichtingscheckParameters
    ): RestResultaat<RestZaaktypeInrichtingscheck> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return healthCheckService.controleerZaaktypes().toRestZaaktypeInrichtingscheckResultaat(parameters)
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
}
