/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
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
import nl.info.zac.zaak.readZaakAutorisatieGegevens
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
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
    private val zrcClientService: ZrcClientService
) {
    @GET
    @Path("zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten")
    fun listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
        @PathParam("uuid") zaakUUID: UUID,
        @QueryParam("groepId") @NotBlank groepId: String
    ): List<RestUser> {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakAutorisatieGegevens = readZaakAutorisatieGegevensAndAssertWijzigen(zaak, zaakType)
        return zaakspecifiekeAutorisatieService
            .listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(zaakAutorisatieGegevens, zaakType, groepId)
            .toRestUsers()
    }

    @POST
    @Path("zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers")
    fun addZaakspecifiekGeautoriseerdeMedewerker(
        @PathParam("uuid") zaakUUID: UUID,
        @Valid restZaakspecifiekGeautoriseerdeMedewerker: RestZaakspecifiekGeautoriseerdeMedewerker
    ) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakAutorisatieGegevens = readZaakAutorisatieGegevensAndAssertWijzigen(zaak, zaakType)
        zaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(
            zaak = zaak,
            zaakType = zaakType,
            zaakAutorisatieGegevens = zaakAutorisatieGegevens,
            groepId = restZaakspecifiekGeautoriseerdeMedewerker.groepId,
            medewerkerId = restZaakspecifiekGeautoriseerdeMedewerker.medewerkerId
        )
    }

    private fun readZaakAutorisatieGegevensAndAssertWijzigen(zaak: Zaak, zaakType: ZaakType) =
        zaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(zrcClientService, zaak).also {
            assertPolicy(
                policyService.readZaakRechten(
                    zaak = zaak,
                    zaaktype = zaakType,
                    loggedInUser = loggedInUserInstance.get(),
                    zaakAutorisatieGegevens = it
                ).canWijzigen
            )
        }
}
