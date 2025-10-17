/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.policy.model.RestNotitieRechten
import nl.info.zac.app.policy.model.RestOverigeRechten
import nl.info.zac.app.policy.model.RestWerklijstRechten
import nl.info.zac.app.policy.model.toRestNotitieRechten
import nl.info.zac.app.policy.model.toRestOverigeRechten
import nl.info.zac.app.policy.model.toRestWerklijstRechten
import nl.info.zac.policy.PolicyService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Path("policy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class PolicyRestService @Inject constructor(
    private val policyService: PolicyService
) {
    @GET
    @Path("werklijstRechten")
    fun readWerklijstRechten(): RestWerklijstRechten = policyService.readWerklijstRechten().toRestWerklijstRechten()

    @GET
    @Path("overigeRechten")
    fun readOverigeRechten(): RestOverigeRechten = policyService.readOverigeRechten().toRestOverigeRechten()

    @GET
    @Path("notitieRechten")
    fun readNotitieRechten(): RestNotitieRechten = policyService.readNotitieRechten().toRestNotitieRechten()
}
