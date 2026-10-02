/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.ProcessingException
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObjectRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraagRequest
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagDocumentService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagDocumentService::class.java.name)

        private const val AANVRAAG_PDF_TITEL = "Aanvraag PDF"
        private const val AANVRAAG_PDF_BESCHRIJVING = "PDF document met de aanvraag gegevens van de zaak"
        private const val ZAAK_INFORMATIEOBJECT_REDEN =
            "Document toegevoegd tijdens het starten van de zaak vanuit een product aanvraag"
    }

    fun pairProductaanvraagWithZaak(productaanvraag: ModelObject, zaakUrl: URI) {
        ZaakobjectProductaanvraagRequest(zaakUrl, productaanvraag.url)
            .let(zrcClientService::createZaakobject)
    }

    fun pairAanvraagPDFWithZaak(productaanvraag: ProductaanvraagDimpact, zaakUrl: URI) {
        if (productaanvraag.pdf == null) {
            LOG.warning { "No PDF found in productaanvraag to link to the zaak" }
            return
        }
        ZaakInformatieObjectRequest().apply {
            informatieobject = productaanvraag.pdf
            zaak = zaakUrl
            titel = AANVRAAG_PDF_TITEL
            beschrijving = AANVRAAG_PDF_BESCHRIJVING
        }.run {
            zrcClientService.createZaakInformatieobject(this, ZAAK_INFORMATIEOBJECT_REDEN)
        }
    }

    fun pairBijlagenWithZaak(bijlageURIs: List<URI>, zaakUrl: URI) =
        bijlageURIs.map(drcClientService::readEnkelvoudigInformatieobject).forEach { bijlage ->
            ZaakInformatieObjectRequest().apply {
                informatieobject = bijlage.url
                zaak = zaakUrl
                titel = bijlage.titel
                beschrijving = bijlage.beschrijving
            }.run {
                zrcClientService.createZaakInformatieobject(this, ZAAK_INFORMATIEOBJECT_REDEN)
            }
        }

    fun pairBijlagenWithZaakIgnoringExceptions(bijlageURIs: List<URI>, zaakUrl: URI) =
        bijlageURIs.forEach { bijlageURI ->
            try {
                val bijlage = drcClientService.readEnkelvoudigInformatieobject(bijlageURI)
                ZaakInformatieObjectRequest().apply {
                    informatieobject = bijlage.url
                    zaak = zaakUrl
                    titel = bijlage.titel
                    beschrijving = bijlage.beschrijving
                }.run {
                    zrcClientService.createZaakInformatieobject(this, ZAAK_INFORMATIEOBJECT_REDEN)
                }
            } catch (zgwRuntimeException: ZgwRuntimeException) {
                logBijlagePairingFailure(bijlageURI, zaakUrl, zgwRuntimeException)
            } catch (zgwErrorException: ZgwErrorException) {
                logBijlagePairingFailure(bijlageURI, zaakUrl, zgwErrorException)
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                logBijlagePairingFailure(bijlageURI, zaakUrl, zgwValidationErrorException)
            } catch (processingException: ProcessingException) {
                logBijlagePairingFailure(bijlageURI, zaakUrl, processingException)
            }
        }

    private fun logBijlagePairingFailure(bijlageURI: URI, zaakUrl: URI, exception: RuntimeException) =
        LOG.log(Level.WARNING, "Failed to pair bijlage '$bijlageURI' with zaak url '$zaakUrl'", exception)
}
