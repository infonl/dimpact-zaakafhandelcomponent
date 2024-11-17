/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken;

import static net.atos.zac.policy.PolicyService.assertPolicy;
import static net.atos.zac.zoeken.model.zoekobject.ZoekObjectType.TAAK;
import static net.atos.zac.zoeken.model.zoekobject.ZoekObjectType.ZAAK;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.zoeken.converter.RESTZoekParametersConverter;
import net.atos.zac.app.zoeken.converter.RESTZoekResultaatConverter;
import net.atos.zac.app.zoeken.model.AbstractRESTZoekObject;
import net.atos.zac.app.zoeken.model.RESTZoekParameters;
import net.atos.zac.app.zoeken.model.RESTZoekResultaat;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.zoeken.ZoekenService;
import net.atos.zac.zoeken.model.ZoekParameters;
import net.atos.zac.zoeken.model.ZoekResultaat;
import net.atos.zac.zoeken.model.zoekobject.ZoekObject;

@Path("zoeken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ZoekenRESTService {

    @Inject
    private ZoekenService zoekenService;

    @Inject
    private RESTZoekParametersConverter zoekZaakParametersConverter;

    @Inject
    private RESTZoekResultaatConverter ZoekResultaatConverter;

    @Inject
    private PolicyService policyService;

    @PUT
    @Path("list")
    public RESTZoekResultaat<? extends AbstractRESTZoekObject> list(final RESTZoekParameters restZoekParameters) {
        if (restZoekParameters.type == ZAAK || restZoekParameters.type == TAAK) {
            assertPolicy(policyService.readWerklijstRechten().zakenTaken());
        } else {
            assertPolicy(policyService.readOverigeRechten().zoeken());
        }
        final ZoekParameters zoekParameters = zoekZaakParametersConverter.convert(restZoekParameters);
        final ZoekResultaat<? extends ZoekObject> zoekResultaat = zoekenService.zoek(zoekParameters);
        return ZoekResultaatConverter.convert(zoekResultaat, restZoekParameters);
    }
}
