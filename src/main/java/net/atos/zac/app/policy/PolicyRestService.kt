/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.policy.converter.toRestOverigeRechten
import net.atos.zac.app.policy.converter.toRestWerklijstRechten
import net.atos.zac.app.policy.model.RestOverigeRechten
import net.atos.zac.app.policy.model.RestWerklijstRechten
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
}
