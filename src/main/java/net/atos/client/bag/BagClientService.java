/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag;

import static net.atos.client.bag.util.BagClientHeadersFactory.API_KEY;
import static net.atos.client.bag.util.BagClientHeadersFactory.X_API_KEY;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.bag.api.AdresApi;
import net.atos.client.bag.api.NummeraanduidingApi;
import net.atos.client.bag.api.OpenbareRuimteApi;
import net.atos.client.bag.api.PandApi;
import net.atos.client.bag.api.WoonplaatsApi;
import net.atos.client.bag.model.BevraagAdressenParameters;
import net.atos.client.util.JAXRSClientFactory;
import net.atos.zac.util.MediaTypes;
import nl.info.client.bag.model.generated.AdresIOHal;
import nl.info.client.bag.model.generated.AdresIOHalCollectionEmbedded;
import nl.info.client.bag.model.generated.NummeraanduidingIOHal;
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal;
import nl.info.client.bag.model.generated.PandIOHal;
import nl.info.client.bag.model.generated.WoonplaatsIOHal;

@ApplicationScoped
public class BagClientService {

    public static final String DEFAULT_CRS = "epsg:28992";

    private static final String ADRES_EXPAND = "panden, adresseerbaarObject, nummeraanduiding, openbareRuimte, woonplaats";

    private static final String NUMMERAANDUIDING_EXPAND = "ligtAanOpenbareRuimte, ligtInWoonplaats";

    private static final String OPENBARE_RUIMTE_EXPAND = "ligtInWoonplaats";

    @Inject
    @RestClient
    private AdresApi adresApi;

    @Inject
    @RestClient
    private WoonplaatsApi woonplaatsApi;

    @Inject
    @RestClient
    private NummeraanduidingApi nummeraanduidingApi;

    @Inject
    @RestClient
    private PandApi pandApi;

    @Inject
    @RestClient
    private OpenbareRuimteApi openbareRuimteApi;

    public AdresIOHal readAdres(final String nummeraanduidingIdentificatie) {
        return adresApi.bevraagAdressenMetNumId(nummeraanduidingIdentificatie, ADRES_EXPAND,
                null);
    }

    public WoonplaatsIOHal readWoonplaats(final String woonplaatswIdentificatie) {
        return woonplaatsApi.woonplaatsIdentificatie(woonplaatswIdentificatie, null, null, null, null, null);
    }

    public NummeraanduidingIOHal readNummeraanduiding(final String nummeraanduidingIdentificatie) {
        return nummeraanduidingApi.nummeraanduidingIdentificatie(nummeraanduidingIdentificatie, null, null, NUMMERAANDUIDING_EXPAND, null);
    }

    public PandIOHal readPand(final String pandIdentificatie) {
        return pandApi.pandIdentificatie(pandIdentificatie, null, null, DEFAULT_CRS, null);
    }

    public OpenbareRuimteIOHal readOpenbareRuimte(final String openbareRuimeIdentificatie) {
        return openbareRuimteApi.openbareruimteIdentificatie(openbareRuimeIdentificatie, null, null, OPENBARE_RUIMTE_EXPAND, null);
    }

    public List<AdresIOHal> listAdressen(final BevraagAdressenParameters parameters) {
        final AdresIOHalCollectionEmbedded embedded = adresApi.bevraagAdressen(parameters).getEmbedded();
        if (embedded != null && embedded.getAdressen() != null) {
            return embedded.getAdressen();
        } else {
            return Collections.emptyList();
        }
    }

    public AdresIOHal readAdres(final URI adresURI) {
        return createInvocationBuilder(adresURI).get(AdresIOHal.class);
    }

    private Invocation.Builder createInvocationBuilder(final URI uri) {
        return JAXRSClientFactory.getOrCreateClient().target(uri)
                .request(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
                .header(X_API_KEY, API_KEY);
    }
}
