/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.identity.model.toRestUsers
import nl.info.zac.app.zaak.model.RestZaakspecifiekGeautoriseerdeMedewerker
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import java.util.UUID

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZaakspecifiekeAutorisatieRestService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zaakService: ZaakService,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService
) {
    @GET
    @Path("zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten")
    fun listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
        @PathParam("uuid") zaakUUID: UUID,
        @QueryParam("groepId") groepId: String
    ): List<RestUser> {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).canWijzigen)
        return zaakspecifiekeAutorisatieService
            .listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(zaak, zaakType, groepId)
            .toRestUsers()
    }

    @POST
    @Path("zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers")
    fun addZaakspecifiekGeautoriseerdeMedewerker(
        @PathParam("uuid") zaakUUID: UUID,
        @Valid restZaakspecifiekGeautoriseerdeMedewerker: RestZaakspecifiekGeautoriseerdeMedewerker
    ) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).canWijzigen)
        zaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(
            zaak = zaak,
            zaakType = zaakType,
            groepId = restZaakspecifiekGeautoriseerdeMedewerker.groepId,
            medewerkerId = restZaakspecifiekGeautoriseerdeMedewerker.medewerkerId
        )
    }
}
