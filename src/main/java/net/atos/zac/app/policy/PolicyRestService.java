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

import net.atos.zac.app.policy.converter.RestRechtenConverter;
import net.atos.zac.app.policy.model.RestOverigeRechten;
import net.atos.zac.app.policy.model.RestWerklijstRechten;
import net.atos.zac.policy.PolicyService;

@Path("policy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PolicyRestService {

    @Inject
    private PolicyService policyService;

    @GET
    @Path("werklijstRechten")
    public RestWerklijstRechten readWerklijstRechten() {
        return RestRechtenConverter.convert(policyService.readWerklijstRechten());
    }

    @GET
    @Path("overigeRechten")
    public RestOverigeRechten readOverigeRechten() {
        return RestRechtenConverter.convert(policyService.readOverigeRechten());
    }
}
