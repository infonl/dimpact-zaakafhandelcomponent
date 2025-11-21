/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.bag.api.AdresApi;
import net.atos.client.bag.api.NummeraanduidingApi;
import net.atos.client.bag.api.OpenbareRuimteApi;
import net.atos.client.bag.api.PandApi;
import net.atos.client.bag.api.WoonplaatsApi;
import net.atos.client.bag.model.BevraagAdressenParameters;
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

    private AdresApi adresApi;
    private WoonplaatsApi woonplaatsApi;
    private NummeraanduidingApi nummeraanduidingApi;
    private PandApi pandApi;
    private OpenbareRuimteApi openbareRuimteApi;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public BagClientService() {
    }

    @Inject
    public BagClientService(
            @RestClient AdresApi adresApi,
            @RestClient WoonplaatsApi woonplaatsApi,
            @RestClient NummeraanduidingApi nummeraanduidingApi,
            @RestClient PandApi pandApi,
            @RestClient OpenbareRuimteApi openbareRuimteApi
    ) {
        this.adresApi = adresApi;
        this.woonplaatsApi = woonplaatsApi;
        this.nummeraanduidingApi = nummeraanduidingApi;
        this.pandApi = pandApi;
        this.openbareRuimteApi = openbareRuimteApi;
    }

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
}
