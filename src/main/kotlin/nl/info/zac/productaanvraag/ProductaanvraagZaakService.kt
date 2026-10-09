/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.ProcessingException
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.model.KlantcontactContactDetails
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.Zaak.TOELICHTING_MAX_LENGTH
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.Geometry
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.toGeoJSONGeometry
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import java.util.logging.Level
import java.util.logging.Logger

/**
 * The steps to create a zaak from a productaanvraag that do not depend on the process engine of its zaaktype.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class ProductaanvraagZaakService @Inject constructor(
    private val zgwApiService: ZgwApiService,
    private val zaakService: ZaakService,
    private val identityService: IdentityService,
    private val configurationService: ConfigurationService,
    private val klantClientService: KlantClientService,
    private val productaanvraagBetrokkeneService: ProductaanvraagBetrokkeneService,
    private val productaanvraagDocumentService: ProductaanvraagDocumentService,
    private val productaanvraagClaimRepository: ProductaanvraagClaimRepository
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagZaakService::class.java.name)
    }

    fun createZaak(
        zaaktype: ZaakType,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ): Zaak = Zaak().apply {
        this.zaaktype = zaaktype.url
        startdatum = productaanvraagObject.record.startAt
        bronorganisatie = configurationService.readBronOrganisatie()
        communicatiekanaalNaam = ConfigurationService.COMMUNICATIEKANAAL_EFORMULIER
        verantwoordelijkeOrganisatie = configurationService.readBronOrganisatie()
        productaanvraagDimpact.zaakgegevens?.let { zaakgegevens ->
            // note that ZAC currently only supports 'POINT' zaakgeometries
            zaakgegevens.geometry?.takeIf { it.type == Geometry.Type.POINT }?.let {
                zaakgeometrie = it.toGeoJSONGeometry()
            }
            zaakgegevens.omschrijving?.let { omschrijving = it }
        }
        toelichting = generateZaakExplanationFromProductaanvraag(productaanvraagDimpact)
    }.let(zgwApiService::createZaak).also {
        // Mark the productaanvraag as done as soon as its zaak exists. A productaanvraag that is not marked as done
        // is handled again after its claim times out, and that would create a second zaak for it.
        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
    }

    /**
     * A default behandelaar that is no longer a member of the default group is a stale configuration. It must not
     * stop the intake, so the zaak is then assigned to the group only.
     */
    fun findValidDefaultBehandelaarId(zaaktypeConfiguration: ZaaktypeConfiguration, zaak: Zaak): String? {
        val groupId = zaaktypeConfiguration.groepID
        val defaultBehandelaarId = zaaktypeConfiguration.defaultBehandelaarId
        if (defaultBehandelaarId == null || groupId == null || identityService.isUserInGroup(defaultBehandelaarId, groupId)) {
            return defaultBehandelaarId
        }
        LOG.warning {
            "Default behandelaar '$defaultBehandelaarId' is not a member of default group '$groupId'. " +
                "Therefore zaak with UUID '${zaak.uuid}' is assigned to the group only."
        }
        return null
    }

    /**
     * Sets up the zaak with the data of the productaanvraag: pairs the productaanvraag and its documents with the
     * zaak, assigns the zaak to the default group and the given behandelaar, adds the initiator and the other
     * betrokkenen, and links the productaanvraag-specific contact details.
     */
    fun setUpZaakFromProductaanvraag(
        zaak: Zaak,
        zaaktypeConfiguration: ZaaktypeConfiguration,
        behandelaarId: String?,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ): ProductaanvraagInitiator {
        // First, pair the productaanvraag and assign the zaak to the group and/or user,
        // so that should things fail afterward, at least the productaanvraag has been paired and the zaak has been assigned.
        productaanvraagDocumentService.pairProductaanvraagWithZaak(
            productaanvraag = productaanvraagObject,
            zaakUrl = zaak.url
        )
        assignZaak(
            zaak = zaak,
            groupId = zaaktypeConfiguration.groepID,
            behandelaarId = behandelaarId,
            productaanvraagDimpact = productaanvraagDimpact
        )
        pairDocumentsWithZaak(productaanvraagDimpact = productaanvraagDimpact, zaak = zaak)
        val initiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
            productaanvraag = productaanvraagDimpact,
            zaak = zaak,
            brpEnabled = zaaktypeConfiguration.zaaktypeBetrokkeneParameters?.isBrpKoppelenEnabled ?: false,
            kvkEnabled = zaaktypeConfiguration.zaaktypeBetrokkeneParameters?.isKvkKoppelenEnabled ?: false
        )
        val klantcontactContactDetails = klantClientService.findKlantcontactContactDetailsForFormulier(
            productaanvraagDimpact.bron.kenmerk
        )?.also {
            klantClientService.linkKlantcontactToZaak(klantcontactUuid = it.klantcontactUuid, zaakUuid = zaak.uuid)
        }
        return ProductaanvraagInitiator(
            betrokkene = initiator,
            klantcontactContactDetails = klantcontactContactDetails
        )
    }

    private fun assignZaak(
        zaak: Zaak,
        groupId: String?,
        behandelaarId: String?,
        productaanvraagDimpact: ProductaanvraagDimpact
    ) {
        if (groupId == null) {
            LOG.warning {
                "No group ID found in the zaaktype configuration for zaak ${zaak.identificatie} with UUID '${zaak.uuid}'. " +
                    "No group role was assigned for this zaak created for " +
                    "${generateProductaanvraagDescription(productaanvraagDimpact)}."
            }
        }
        if (groupId == null && behandelaarId == null) return
        LOG.info { "Assigning zaak with UUID '${zaak.uuid}' to group: '$groupId' and behandelaar: '$behandelaarId'" }
        zaakService.assignZaak(
            zaak = zaak,
            groupId = groupId,
            userName = behandelaarId,
            reason = null
        )
    }

    private fun pairDocumentsWithZaak(productaanvraagDimpact: ProductaanvraagDimpact, zaak: Zaak) {
        try {
            productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url)
        } catch (zgwRuntimeException: ZgwRuntimeException) {
            logAanvraagPdfPairingFailure(productaanvraagDimpact, zaak, zgwRuntimeException)
        } catch (zgwErrorException: ZgwErrorException) {
            logAanvraagPdfPairingFailure(productaanvraagDimpact, zaak, zgwErrorException)
        } catch (zgwValidationErrorException: ZgwValidationErrorException) {
            logAanvraagPdfPairingFailure(productaanvraagDimpact, zaak, zgwValidationErrorException)
        } catch (processingException: ProcessingException) {
            logAanvraagPdfPairingFailure(productaanvraagDimpact, zaak, processingException)
        }
        productaanvraagDimpact.bijlagen?.let {
            productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(bijlageURIs = it, zaakUrl = zaak.url)
        }
    }

    private fun logAanvraagPdfPairingFailure(
        productaanvraagDimpact: ProductaanvraagDimpact,
        zaak: Zaak,
        exception: RuntimeException
    ) = LOG.log(
        Level.WARNING,
        "Failed to pair aanvraag PDF `${productaanvraagDimpact.pdf}` with zaak '${zaak.identificatie}'",
        exception
    )

    private fun generateZaakExplanationFromProductaanvraag(productaanvraag: ProductaanvraagDimpact): String =
        (
            "Aangemaakt vanuit ${productaanvraag.bron.naam} met kenmerk '${productaanvraag.bron.kenmerk}'." +
                productaanvraag.zaakgegevens?.toelichting?.let { " $it" }.orEmpty()
            )
            .take(TOELICHTING_MAX_LENGTH)

    private fun generateProductaanvraagDescription(productaanvraag: ProductaanvraagDimpact) =
        "Productaanvraag '${productaanvraag.bron.naam}' with characteristics '${productaanvraag.bron.kenmerk}' and " +
            "type '${productaanvraag.type}'"
}

data class ProductaanvraagInitiator(
    val betrokkene: Betrokkene?,
    val klantcontactContactDetails: KlantcontactContactDetails?
)
