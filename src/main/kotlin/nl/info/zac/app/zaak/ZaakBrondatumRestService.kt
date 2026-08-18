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
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.zac.app.zaak.model.RestZaakSetBrondatum
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
class ZaakBrondatumRestService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zaakService: ZaakService,
    private val zgwApiService: ZgwApiService
) {
    @PUT
    @Path("/zaak/{uuid}/brondatum")
    fun setBrondatum(@PathParam("uuid") zaakUUID: UUID, @Valid restZaakSetBrondatum: RestZaakSetBrondatum) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).brondatumZetten)
        zgwApiService.setBrondatum(zaak, restZaakSetBrondatum.brondatum)
    }
}
