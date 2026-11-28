/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.mockk.mockk
import nl.info.client.zgw.ztc.log.ZtcReadOperation
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.log.LogCapture
import java.net.URI
import java.util.UUID

class ZtcClientServiceLoggingTest : BehaviorSpec({
    val zaaktypeURI = URI("https://example.com/zaaktypen/fakeZaaktype")
    val reads: List<Triple<ZtcReadOperation, String, ZtcClientService.() -> Any?>> = listOf(
        Triple(ZtcReadOperation.READ_ZAAKTYPE, "readZaaktype", { readZaaktype(UUID.randomUUID()) }),
        Triple(
            ZtcReadOperation.LIST_ZAAKTYPEN,
            "listZaaktypen",
            { listZaaktypen(URI("https://example.com/catalogi/fakeCatalogus")) }
        ),
        Triple(ZtcReadOperation.READ_STATUSTYPE, "readStatustype", { readStatustype(UUID.randomUUID()) }),
        Triple(ZtcReadOperation.READ_STATUSTYPEN, "readStatustypen", { readStatustypen(zaaktypeURI) }),
        Triple(
            ZtcReadOperation.READ_ZAAKTYPE_INFORMATIEOBJECTTYPEN,
            "readZaaktypeInformatieobjecttypen",
            { readZaaktypeInformatieobjecttypen(zaaktypeURI) }
        ),
        Triple(
            ZtcReadOperation.READ_INFORMATIEOBJECTTYPEN,
            "readInformatieobjecttypen",
            { readInformatieobjecttypen(zaaktypeURI) }
        ),
        Triple(
            ZtcReadOperation.READ_INFORMATIEOBJECTTYPE,
            "readInformatieobjecttype",
            { readInformatieobjecttype(UUID.randomUUID()) }
        ),
        Triple(ZtcReadOperation.READ_BESLUITTYPE, "readBesluittype", { readBesluittype(UUID.randomUUID()) }),
        Triple(ZtcReadOperation.READ_BESLUITTYPEN, "readBesluittypen", { readBesluittypen(zaaktypeURI) }),
        Triple(ZtcReadOperation.READ_RESULTAATTYPE, "readResultaattype", { readResultaattype(UUID.randomUUID()) }),
        Triple(ZtcReadOperation.READ_RESULTAATTYPEN, "readResultaattypen", { readResultaattypen(zaaktypeURI) }),
        Triple(
            ZtcReadOperation.FIND_ROLTYPEN,
            "findRoltypen by omschrijving",
            { findRoltypen(zaaktypeURI, "fakeOmschrijving") }
        ),
        Triple(
            ZtcReadOperation.FIND_ROLTYPEN,
            "findRoltypen by omschrijving generiek",
            { findRoltypen(zaaktypeURI, OmschrijvingGeneriekEnum.BEHANDELAAR) }
        ),
        Triple(ZtcReadOperation.LIST_ROLTYPEN, "listRoltypen", { listRoltypen() }),
        Triple(ZtcReadOperation.READ_ROLTYPE, "readRoltype by UUID", { readRoltype(UUID.randomUUID()) }),
        Triple(
            ZtcReadOperation.READ_ROLTYPE,
            "readRoltype by omschrijving generiek",
            { readRoltype(zaaktypeURI, OmschrijvingGeneriekEnum.BEHANDELAAR) }
        ),
        Triple(ZtcReadOperation.READ_EIGENSCHAPPEN, "readEigenschappen", { readEigenschappen(zaaktypeURI) })
    )

    reads.forEach { (operation, functionName, read) ->
        given("an empty cache, FINE logging and a ZTC that cannot be reached, for $functionName") {
            val ztcClientService = ZtcClientService(
                ztcClient = mockk<ZtcClient>(),
                cacheMaxSize = 100L,
                cacheExpirationSeconds = 86_400L
            )

            `when`("the ZTC is read") {
                val records = LogCapture(ZtcClientService::class).use { logCapture ->
                    shouldThrowAny { ztcClientService.read() }
                    logCapture.records
                }

                then("the read is logged with the $operation operation before the ZTC is called") {
                    records.filter { it.mdc["event"] == "ZtcRead" }.map { it.mdc["operation"] } shouldContain
                        operation.name
                }
            }
        }
    }
})
