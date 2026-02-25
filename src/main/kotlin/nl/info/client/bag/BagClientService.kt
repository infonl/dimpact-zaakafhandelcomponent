/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.bag.api.AdresApi
import nl.info.client.bag.api.NummeraanduidingApi
import nl.info.client.bag.api.OpenbareRuimteApi
import nl.info.client.bag.api.PandApi
import nl.info.client.bag.api.WoonplaatsApi
import nl.info.client.bag.model.BevraagAdressenParameters
import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.NummeraanduidingIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.PandIOHal
import nl.info.client.bag.model.generated.WoonplaatsIOHal
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@NoArgConstructor
@AllOpen
class BagClientService @Inject constructor(
    @RestClient private val adresApi: AdresApi,
    @RestClient private val woonplaatsApi: WoonplaatsApi,
    @RestClient private val nummeraanduidingApi: NummeraanduidingApi,
    @RestClient private val pandApi: PandApi,
    @RestClient private val openbareRuimteApi: OpenbareRuimteApi
) {
    companion object {
        const val DEFAULT_CRS = "epsg:28992"
        private const val ADRES_EXPAND = "panden, adresseerbaarObject, nummeraanduiding, openbareRuimte, woonplaats"
        private const val NUMMERAANDUIDING_EXPAND = "ligtAanOpenbareRuimte, ligtInWoonplaats"
        private const val OPENBARE_RUIMTE_EXPAND = "ligtInWoonplaats"
    }

    fun readAdres(nummeraanduidingIdentificatie: String): AdresIOHal =
        adresApi.bevraagAdressenMetNumId(nummeraanduidingIdentificatie, ADRES_EXPAND, null)

    fun readWoonplaats(woonplaatswIdentificatie: String): WoonplaatsIOHal =
        woonplaatsApi.woonplaatsIdentificatie(woonplaatswIdentificatie, null, null, null, null, null)

    fun readNummeraanduiding(nummeraanduidingIdentificatie: String): NummeraanduidingIOHal =
        nummeraanduidingApi.nummeraanduidingIdentificatie(
            nummeraanduidingIdentificatie, null, null, NUMMERAANDUIDING_EXPAND, null
        )

    fun readPand(pandIdentificatie: String): PandIOHal =
        pandApi.pandIdentificatie(pandIdentificatie, null, null, DEFAULT_CRS, null)

    fun readOpenbareRuimte(openbareRuimeIdentificatie: String): OpenbareRuimteIOHal =
        openbareRuimteApi.openbareruimteIdentificatie(
            openbareRuimeIdentificatie, null, null, OPENBARE_RUIMTE_EXPAND, null
        )

    fun listAdressen(parameters: BevraagAdressenParameters): List<AdresIOHal> {
        val embedded = adresApi.bevraagAdressen(parameters).getEmbedded()
        return embedded?.adressen ?: emptyList()
    }
}
