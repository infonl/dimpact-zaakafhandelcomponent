/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.policy;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.policy.converter.RESTRechtenConverter;
import net.atos.zac.app.policy.model.RestOverigeRechten;
import net.atos.zac.app.policy.model.RestWerklijstRechten;
import net.atos.zac.policy.PolicyService;

@Path("policy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PolicyRESTService {

    @Inject
    private PolicyService policyService;

    @Inject
    private RESTRechtenConverter rechtenConverter;

    @GET
    @Path("werklijstRechten")
    public RestWerklijstRechten readWerklijstRechten() {
        return rechtenConverter.convert(policyService.readWerklijstRechten());
    }

    @GET
    @Path("overigeRechten")
    public RestOverigeRechten readOverigeRechten() {
        return rechtenConverter.convert(policyService.readOverigeRechten());
    }
}
