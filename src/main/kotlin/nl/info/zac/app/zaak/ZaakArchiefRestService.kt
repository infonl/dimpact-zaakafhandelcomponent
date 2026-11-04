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
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.RestZaakArchiefnominatieGegevens
import nl.info.zac.app.zaak.model.toArchiefnominatieEnum
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import java.util.UUID

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZaakArchiefRestService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zaakService: ZaakService,
    private val zrcClientService: ZrcClientService,
    private val restZaakConverter: RestZaakConverter
) {
    @PUT
    @Path("/zaak/{uuid}/archiefnominatie")
    fun updateArchiefnominatie(
        @PathParam("uuid") zaakUUID: UUID,
        @Valid restZaakArchiefnominatieGegevens: RestZaakArchiefnominatieGegevens
    ): RestZaak {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(
            policyService.readZaakRechten(zaak = zaak, zaaktype = zaakType, loggedInUser = loggedInUser)
                .canWijzigenArchief
        )
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            Zaak().apply {
                archiefnominatie = restZaakArchiefnominatieGegevens.archiefnominatie.toArchiefnominatieEnum()
            },
            restZaakArchiefnominatieGegevens.reden
        )
        return restZaakConverter.toRestZaak(
            zaak = updatedZaak,
            zaakType = zaakType,
            zaakRechten = policyService.readZaakRechten(
                zaak = updatedZaak,
                zaaktype = zaakType,
                loggedInUser = loggedInUser
            ),
            loggedInUser = loggedInUser
        )
    }
}
