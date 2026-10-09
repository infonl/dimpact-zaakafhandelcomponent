/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import net.atos.zac.util.JsonbUtil
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.or.shared.exception.ORErrorException
import nl.info.client.or.shared.exception.ORRuntimeException
import nl.info.client.or.shared.exception.ORValidationErrorException
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.authentication.LoggedInUserProvider.Companion.PRODUCTAANVRAAG_GEBRUIKER
import nl.info.zac.authentication.runAsLoggedInUser
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.productaanvraag.model.InboxProductaanvraag
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.BetalingStatusEnumJsonAdapter
import nl.info.zac.productaanvraag.util.GeometryTypeEnumJsonAdapter
import nl.info.zac.productaanvraag.util.IndicatieMachtigingEnumJsonAdapter
import nl.info.zac.productaanvraag.util.RolOmschrijvingGeneriekEnumJsonAdapter
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

private const val PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS = "aanvraaggegevens"
private const val PRODUCTAANVRAAG_FORMULIER_VELD_BRON = "bron"
private const val PRODUCTAANVRAAG_FORMULIER_VELD_TYPE = "type"

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class ProductaanvraagService @Inject constructor(
    private val objectsClientService: ObjectsClientService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val inboxDocumentService: InboxDocumentService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val productaanvraagCmmnService: ProductaanvraagCmmnService,
    private val productaanvraagBpmnService: ProductaanvraagBpmnService,
    private val productaanvraagClaimRepository: ProductaanvraagClaimRepository
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagService::class.java.name)
    }

    fun handleProductaanvraag(productaanvraagObjectUUID: UUID) {
        LOG.info { "Handling productaanvraag with object UUID: $productaanvraagObjectUUID" }
        if (!productaanvraagClaimRepository.claim(productaanvraagObjectUUID)) {
            LOG.info {
                "Productaanvraag with object UUID: '$productaanvraagObjectUUID' is already being handled or has already " +
                    "been handled. Skipping it."
            }
            return
        }
        runAsLoggedInUser(PRODUCTAANVRAAG_GEBRUIKER) {
            readProductaanvraagObject(productaanvraagObjectUUID)
                ?.takeIf(::isProductaanvraagDimpact)
                ?.let { handleProductaanvraagDimpactWithoutFailing(productaanvraagObjectUUID, it) }
        }
    }

    fun getAanvraaggegevens(productaanvraagObject: ModelObject) = productaanvraagObject.extractAanvraaggegevens()

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    fun getProductaanvraag(productaanvraagObject: ModelObject): ProductaanvraagDimpact =
        JsonbBuilder.create(
            JsonbConfig()
                // Register our enum JSON adapters because by default enums are deserialized using the enum's name
                // instead of the value, and this fails because in the generated model classes the enum names are
                // capitalized and the values are not
                .withAdapters(
                    IndicatieMachtigingEnumJsonAdapter(),
                    RolOmschrijvingGeneriekEnumJsonAdapter(),
                    BetalingStatusEnumJsonAdapter(),
                    GeometryTypeEnumJsonAdapter()
                )
        ).fromJson(
            JsonbUtil.JSONB.toJson(productaanvraagObject.record.data),
            ProductaanvraagDimpact::class.java
        )

    private fun readProductaanvraagObject(productaanvraagObjectUUID: UUID): ModelObject? =
        try {
            objectsClientService.readObject(productaanvraagObjectUUID)
        } catch (orErrorException: ORErrorException) {
            logUnreadableProductaanvraagObject(productaanvraagObjectUUID, orErrorException)
        } catch (orValidationErrorException: ORValidationErrorException) {
            logUnreadableProductaanvraagObject(productaanvraagObjectUUID, orValidationErrorException)
        } catch (orRuntimeException: ORRuntimeException) {
            logUnreadableProductaanvraagObject(productaanvraagObjectUUID, orRuntimeException)
        } catch (webApplicationException: WebApplicationException) {
            logUnreadableProductaanvraagObject(productaanvraagObjectUUID, webApplicationException)
        } catch (processingException: ProcessingException) {
            logUnreadableProductaanvraagObject(productaanvraagObjectUUID, processingException)
        }

    private fun logUnreadableProductaanvraagObject(productaanvraagObjectUUID: UUID, exception: RuntimeException): Nothing? {
        LOG.log(Level.WARNING, "Unable to read object with UUID: $productaanvraagObjectUUID", exception)
        return null
    }

    /**
     * Checks if the required attributes defined by the 'Productaanvraag Dimpact' JSON schema are present.
     * This is a bit of a poor man's solution because we are currently 'misusing' the very generic Objects API
     * to store specific productaanvraag JSON data.
     */
    private fun isProductaanvraagDimpact(productaanvraagObject: ModelObject) =
        productaanvraagObject.record.data.let {
            it.containsKey(PRODUCTAANVRAAG_FORMULIER_VELD_BRON) &&
                it.containsKey(PRODUCTAANVRAAG_FORMULIER_VELD_TYPE) &&
                it.containsKey(PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS)
        }

    private fun handleProductaanvraagDimpactWithoutFailing(productaanvraagObjectUUID: UUID, productaanvraagObject: ModelObject) {
        LOG.info("Handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID")
        try {
            handleProductaanvraagDimpact(productaanvraagObject)
        } catch (@Suppress("TooGenericExceptionCaught") runtimeException: RuntimeException) {
            LOG.log(
                Level.WARNING,
                "Failed to handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID",
                runtimeException
            )
        }
    }

    /**
     * Handles a productaanvraag-Dimpact [ModelObject]
     * - If the current configuration of a zaaktype has the productaanvraagtype and is bound to a process engine,
     *   a zaak of that zaaktype is created, and a case or process is started for it in that engine.
     * - If more than one bound configuration has the productaanvraagtype, the most recently created one is used
     *   and a warning is logged.
     * - If no bound configuration has the productaanvraagtype, it creates an 'inbox productaanvraag'.
     */
    private fun handleProductaanvraagDimpact(productaanvraagObject: ModelObject) {
        LOG.fine { "Start handling productaanvraag with object URL: ${productaanvraagObject.url}" }
        val productaanvraag = getProductaanvraag(productaanvraagObject)
        val zaaktypeConfiguration = findBoundZaaktypeConfiguration(productaanvraag.type) ?: run {
            LOG.info(
                "No zaaktype configured for productaanvraag-Dimpact type '${productaanvraag.type}'. " +
                    "No zaak was created. Registering productaanvraag as inbox productaanvraag."
            )
            registreerInbox(productaanvraag, productaanvraagObject)
            productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
            return
        }
        LOG.fine {
            "Creating a zaak using process engine ${zaaktypeConfiguration.getProcessEngine()} with zaaktype UUID: " +
                "'${zaaktypeConfiguration.zaaktypeUuid}'"
        }
        when (zaaktypeConfiguration.getProcessEngine()) {
            ProcessEngine.BPMN -> productaanvraagBpmnService.createAndStartZaak(
                zaaktypeConfiguration = zaaktypeConfiguration,
                productaanvraagDimpact = productaanvraag,
                productaanvraagObject = productaanvraagObject
            )
            else -> productaanvraagCmmnService.createAndStartZaak(
                zaaktypeConfiguration = zaaktypeConfiguration,
                productaanvraagDimpact = productaanvraag,
                productaanvraagObject = productaanvraagObject
            )
        }
    }

    private fun findBoundZaaktypeConfiguration(productaanvraagtype: String): ZaaktypeConfiguration? {
        val (boundConfigurations, unboundConfigurations) = zaaktypeConfigurationService
            .listCurrentConfigurationsByProductaanvraagtype(productaanvraagtype)
            .partition { it.processBinding != null }
        unboundConfigurations.forEach {
            LOG.warning(
                "Zaaktype configuration with zaaktype UUID '${it.zaaktypeUuid}' has productaanvraag type " +
                    "'$productaanvraagtype' but is not bound to a process engine, so it is ignored."
            )
        }
        if (boundConfigurations.size > 1) {
            LOG.warning(
                "Multiple zaaktype configurations found for productaanvraag type '$productaanvraagtype'. " +
                    "Using the most recently created one with zaaktype UUID: " +
                    "'${boundConfigurations.first().zaaktypeUuid}' and zaaktype omschrijving: " +
                    "'${boundConfigurations.first().zaaktypeOmschrijving}'."
            )
        }
        return boundConfigurations.firstOrNull()
    }

    private fun registreerInbox(productaanvraag: ProductaanvraagDimpact, productaanvraagObject: ModelObject) {
        val inboxProductaanvraag = InboxProductaanvraag().apply {
            productaanvraagObjectUUID = productaanvraagObject.uuid
            type = productaanvraag.type
            ontvangstdatum = productaanvraagObject.record.registrationAt
        }
        productaanvraag.betrokkenen?.let { betrokkenen ->
            // note: we also need to support KVK vestigingsnummer type here
            // as well as other types of betrokkenen

            // we are only interested in the first betrokkene with the role 'INITIATOR'
            betrokkenen.first { it.rolOmschrijvingGeneriek == Betrokkene.RolOmschrijvingGeneriek.INITIATOR }
                .let { inboxProductaanvraag.initiatorID = it.inpBsn }
        }
        productaanvraag.pdf?.let { inboxProductaanvraag.aanvraagdocumentUUID = it.extractUuid() }
        productaanvraag.bijlagen?.let { inboxProductaanvraag.aantalBijlagen = it.size }

        inboxProductaanvraagService.create(inboxProductaanvraag)

        productaanvraag.pdf?.let { deleteInboxDocument(it.extractUuid()) }
        productaanvraag.bijlagen?.forEach { deleteInboxDocument(it.extractUuid()) }
    }

    private fun deleteInboxDocument(documentUUID: UUID) {
        val inboxDocument = inboxDocumentService.find(documentUUID) ?: run {
            LOG.warning { "Inbox document with id '$documentUUID' not found." }
            return
        }
        inboxDocument.id?.run(inboxDocumentService::deleteIfExists)
    }
}

fun ModelObject.extractAanvraaggegevens(): Map<String, Any> =
    (record.data[PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS] as Map<*, *>)
        .values
        .filterIsInstance<Map<String, Any>>()
        .flatMap { it.entries }
        .associate { it.key to it.value }
