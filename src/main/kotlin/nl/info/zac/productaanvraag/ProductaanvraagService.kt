/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_GROUP
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import net.atos.zac.productaanvraag.util.BetalingStatusEnumJsonAdapter
import net.atos.zac.productaanvraag.util.GeometryTypeEnumJsonAdapter
import net.atos.zac.productaanvraag.util.IndicatieMachtigingEnumJsonAdapter
import net.atos.zac.productaanvraag.util.RolOmschrijvingGeneriekEnumJsonAdapter
import net.atos.zac.util.JsonbUtil
import nl.info.client.klant.KlantClientService
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.app.zaak.exception.ExplanationRequiredException
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identity.IdentityService
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.Geometry
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.toGeoJSONGeometry
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.onFailure

const val TOELICHTING_MAX_LENGTH = 1000

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ProductaanvraagService @Inject constructor(
    private val objectsClientService: ObjectsClientService,
    private val zgwApiService: ZgwApiService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val identityService: IdentityService,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    private val inboxDocumentService: InboxDocumentService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val productaanvraagEmailService: ProductaanvraagEmailService,
    private val cmmnService: CMMNService,
    private val bpmnService: BpmnService,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService,
    private val configurationService: ConfigurationService,
    private val klantClientService: KlantClientService,
    private val productaanvraagBetrokkeneService: ProductaanvraagBetrokkeneService,
    private val productaanvraagDocumentService: ProductaanvraagDocumentService
) {

    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagService::class.java.name)

        private const val PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS = "aanvraaggegevens"
        private const val PRODUCTAANVRAAG_FORMULIER_VELD_BRON = "bron"
        private const val PRODUCTAANVRAAG_FORMULIER_VELD_TYPE = "type"
    }

    fun handleProductaanvraag(productaanvraagObjectUUID: UUID) {
        LOG.info { "Handling productaanvraag with object UUID: $productaanvraagObjectUUID" }
        productaanvraagObjectUUID
            .runCatching(objectsClientService::readObject)
            .onFailure { LOG.warning("Unable to read object with UUID: $productaanvraagObjectUUID") }
            .onSuccess { modelObject ->
                modelObject
                    .takeIf(::isProductaanvraagDimpact)
                    ?.runCatching {
                        LOG.info("Handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID")
                        handleProductaanvraagDimpact(this)
                    }?.onFailure {
                        LOG.log(
                            Level.WARNING,
                            "Failed to handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID",
                            it
                        )
                    }
            }
    }

    fun getAanvraaggegevens(productaanvraagObject: ModelObject): Map<String, Any> =
        (productaanvraagObject.record.data[PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS] as Map<*, *>)
            .values
            .filterIsInstance<Map<String, Any>>()
            .flatMap { it.entries }
            .associate { it.key to it.value }

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

    private fun assignZaakToGroup(zaak: Zaak, groupName: String) {
        LOG.info("Assigning zaak with UUID '${zaak.uuid}' to group: '$groupName'")
        zrcClientService.createRol(createRolGroep(groupName, zaak))
    }

    private fun assignZaakToEmployee(zaak: Zaak, employeeName: String) {
        LOG.info("Assigning zaak '${zaak.uuid}' to employee: '$employeeName'")
        zrcClientService.createRol(createRolMedewerker(employeeName, zaak))
    }

    private fun createRolGroep(groepID: String, zaak: Zaak): RolOrganisatorischeEenheid =
        identityService.readGroup(groepID).let {
            OrganisatorischeEenheidIdentificatie().apply {
                identificatie = it.name
                naam = it.description
            }
        }.let { organisatieEenheid ->
            RolOrganisatorischeEenheid(
                zaak.url,
                ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR),
                "Behandelend groep van de zaak",
                organisatieEenheid
            )
        }

    private fun createRolMedewerker(employeeName: String, zaak: Zaak): RolMedewerker =
        identityService.readUser(employeeName).let {
            MedewerkerIdentificatie().apply {
                identificatie = it.id
                voorletters = it.firstName
                achternaam = it.lastName
            }
        }.let { medewerker ->
            RolMedewerker(
                zaak.url,
                ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR),
                "Behandelaar van de zaak",
                medewerker
            )
        }

    private fun deleteInboxDocument(documentUUID: UUID) {
        val inboxDocument = inboxDocumentService.find(documentUUID) ?: run {
            LOG.warning { "Inbox document with id '$documentUUID' not found." }
            return
        }
        inboxDocument.id?.let(inboxDocumentService::delete)
    }

    /**
     * Handles a productaanvraag-Dimpact [ModelObject]
     * - If a CMMN mapping exists for the productaanvraagtype (via ZaaktypeCmmnConfiguration),
     *   a zaak is created and a CMMN case is started for that zaak.
     * - Else if a BPMN mapping exists for the productaanvraagtype, a zaak is created, and a BPMN
     *   process is started for that zaak using the configured process definition key.
     * - If both CMMN and BPMN mappings exist, CMMN takes precedence: a warning is logged, and
     *   the BPMN mapping is ignored.
     * - If no mapping exists, it will create an 'inbox productaanvraag'.
     *
     */
    private fun handleProductaanvraagDimpact(productaanvraagObject: ModelObject) {
        LOG.fine { "Start handling productaanvraag with object URL: ${productaanvraagObject.url}" }
        val productaanvraag = getProductaanvraag(productaanvraagObject)
        val zaaktypeCmmnConfiguration = zaaktypeCmmnConfigurationBeheerService
            .findActiveZaaktypeCmmnConfigurationsByProductaanvraagtype(productaanvraag.type)
        val zaaktypeBpmnProcessDefinition = zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
            productaanvraag.type
        )
        val hasCmmnDefinition = zaaktypeCmmnConfiguration.isNotEmpty()
        val hasBpmnDefinition = zaaktypeBpmnProcessDefinition != null
        if (hasCmmnDefinition && hasBpmnDefinition) {
            LOG.warning(
                "Both CMMN and BPMN zaaktype definitions found for productaanvraag-Dimpact type '${productaanvraag.type}'. " +
                    "CMMN takes precedence, so the BPMN definition is ignored."
            )
        }
        when {
            hasCmmnDefinition ->
                processProductaanvraagWithCmmnZaaktype(
                    zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration,
                    productaanvraagDimpact = productaanvraag,
                    productaanvraagObject = productaanvraagObject
                )

            hasBpmnDefinition ->
                processProductaanvraagWithBpmnZaaktype(
                    zaaktypeBpmnConfiguration = zaaktypeBpmnProcessDefinition,
                    productaanvraagDimpact = productaanvraag,
                    productaanvraagObject = productaanvraagObject
                )

            else -> {
                LOG.info(
                    "No CMMN nor BPMN zaaktype configured for productaanvraag-Dimpact type '${productaanvraag.type}'. " +
                        "No zaak was created. Registering productaanvraag as inbox productaanvraag."
                )
                registreerInbox(productaanvraag, productaanvraagObject)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processProductaanvraagWithCmmnZaaktype(
        zaaktypeCmmnConfiguration: List<ZaaktypeCmmnConfiguration>,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        if (zaaktypeCmmnConfiguration.size > 1) {
            LOG.warning(
                "Multiple zaaktypeCmmnConfiguration found for productaanvraag type '${productaanvraagDimpact.type}'. " +
                    "Using the first one with zaaktype UUID: '${zaaktypeCmmnConfiguration.first().zaaktypeUuid}' " +
                    "and zaaktype omschrijving: '${zaaktypeCmmnConfiguration.first().zaaktypeOmschrijving}'."
            )
        }

        val firstZaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration.first()
        try {
            LOG.fine {
                "Creating a zaak using a CMMN case with zaaktype UUID: '${firstZaaktypeCmmnConfiguration.zaaktypeUuid}'"
            }
            startZaakWithCmmnProcess(
                zaaktypeUuid = firstZaaktypeCmmnConfiguration.zaaktypeUuid,
                productaanvraagDimpact = productaanvraagDimpact,
                productaanvraagObject = productaanvraagObject
            )
        } catch (exception: ExplanationRequiredException) {
            logZaakCouldNotBeCreatedWarning("CMMN", productaanvraagDimpact, exception)
        } catch (exception: RuntimeException) {
            logZaakCouldNotBeCreatedWarning("CMMN", productaanvraagDimpact, exception)
        }
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

    private fun pairDocumentsWithZaak(
        productaanvraagDimpact: ProductaanvraagDimpact,
        zaak: Zaak
    ) {
        productaanvraagDimpact.runCatching {
            productaanvraagDocumentService.pairAanvraagPDFWithZaak(this, zaak.url)
        }.onFailure {
            LOG.log(
                Level.WARNING,
                "Failed to pair aanvraag PDF `${productaanvraagDimpact.pdf}` with zaak '${zaak.identificatie}'",
                it
            )
        }
        productaanvraagDimpact.bijlagen?.let {
            productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(bijlageURIs = it, zaakUrl = zaak.url)
        }
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
        productaanvraag.pdf?.let { pdfUri ->
            val pdfUUID = pdfUri.extractUuid()
            inboxProductaanvraag.aanvraagdocumentUUID = pdfUUID
            deleteInboxDocument(pdfUUID)
        }
        productaanvraag.bijlagen?.let { bijlagen ->
            inboxProductaanvraag.aantalBijlagen = bijlagen.size
            bijlagen.forEach { deleteInboxDocument(it.extractUuid()) }
        }

        inboxProductaanvraagService.create(inboxProductaanvraag)
    }

    private fun processProductaanvraagWithBpmnZaaktype(
        zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeBpmnConfiguration.zaaktypeUuid)
        val zaak = createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
        val baseBpmnVariablesMap = getAanvraaggegevens(productaanvraagObject)
        val zaakDataVariablesMap = zaaktypeBpmnConfiguration.groepID?.let { baseBpmnVariablesMap + mapOf(VAR_ZAAK_GROUP to it) }
            ?: baseBpmnVariablesMap
        bpmnService.startProcess(
            zaak = zaak,
            zaaktype = zaaktype,
            processDefinitionKey = zaaktypeBpmnConfiguration.bpmnProcessDefinitionKey,
            zaakData = zaakDataVariablesMap
        )
        // First, pair the productaanvraag and assign the zaak to the group and/or user,
        // so that should things fail afterward, at least the productaanvraag has been paired and the zaak has been assigned.
        productaanvraagDocumentService.pairProductaanvraagWithZaak(
            productaanvraag = productaanvraagObject,
            zaakUrl = zaak.url
        )
        zaaktypeBpmnConfiguration.groepID?.let {
            assignZaakToGroup(zaak = zaak, groupName = it)
        }
        // note: BPMN zaaktypes do not yet support a default employee to be assigned to the zaak, as is the case for CMMN
        pairDocumentsWithZaak(productaanvraagDimpact = productaanvraagDimpact, zaak = zaak)
        // note: BPMN zaaktypes do not yet support adding an initiator nor other betrokkenen to the zaak, as is the case for CMMN
        // note: for BPMN zaken no automatic email notification is sent here, because this is the responsibility of the BPMN process
        klantClientService.findProductaanvraagSpecificContactDetails(
            productaanvraagDimpact.bron.kenmerk
        )?.let {
            klantClientService.linkProductaanvraagSpecificContactDetailsToZaak(it, zaak.uuid)
        }
    }

    private fun startZaakWithCmmnProcess(
        zaaktypeUuid: UUID,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUuid)
        val zaak = createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
        val zaaktypeCmmnConfiguration = zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        // First, start the CMMN process for the zaak and only then perform other actions related to the zaak,
        // so that should things fail, at least the CMMN process has been started.
        // Note that the error handling here still has room for improvement.
        cmmnService.startCase(
            zaak = zaak,
            zaaktype = zaaktype,
            zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration,
            zaakData = getAanvraaggegevens(productaanvraagObject)
        )
        // First, pair the productaanvraag and assign the zaak to the group and/or user,
        // so that should things fail afterward, at least the productaanvraag has been paired and the zaak has been assigned.
        productaanvraagDocumentService.pairProductaanvraagWithZaak(
            productaanvraag = productaanvraagObject,
            zaakUrl = zaak.url
        )
        zaaktypeCmmnConfiguration.groepID?.run {
            assignZaakToGroup(
                zaak = zaak,
                groupName = this,
            )
        } ?: LOG.warning(
            "No group ID found in zaaktypeCmmnConfiguration for zaak ${zaak.identificatie} with UUID '${zaak.uuid}'. " +
                "No group role was assigned for this zaak created for ${generateProductaanvraagDescription(productaanvraagDimpact)}."
        )
        zaaktypeCmmnConfiguration.defaultBehandelaarId?.run {
            assignZaakToEmployee(
                zaak = zaak,
                employeeName = this,
            )
        }
        pairDocumentsWithZaak(productaanvraagDimpact = productaanvraagDimpact, zaak = zaak)
        productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
            productaanvraag = productaanvraagDimpact,
            zaak = zaak,
            brpEnabled = isBrpEnabled(zaaktypeCmmnConfiguration),
            kvkEnabled = isKvkEnabled(zaaktypeCmmnConfiguration)
        ).run {
            val productaanvraagSpecificContactDetails = klantClientService.findProductaanvraagSpecificContactDetails(
                productaanvraagDimpact.bron.kenmerk
            )
            productaanvraagSpecificContactDetails?.let {
                klantClientService.linkProductaanvraagSpecificContactDetailsToZaak(it, zaak.uuid)
            }
            productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                zaak = zaak,
                betrokkene = this,
                productaanvraagSpecificEmailAddress = productaanvraagSpecificContactDetails?.contactDetails?.emailAddress,
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
        }
    }

    private fun createZaak(
        zaakType: ZaakType,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ): Zaak {
        return Zaak().apply {
            this.zaaktype = zaakType.url
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
        }.let(zgwApiService::createZaak)
    }

    private fun generateZaakExplanationFromProductaanvraag(productaanvraag: ProductaanvraagDimpact): String =
        (
            "Aangemaakt vanuit ${productaanvraag.bron.naam} met kenmerk '${productaanvraag.bron.kenmerk}'." +
                (productaanvraag.zaakgegevens?.toelichting?.let { " $it" } ?: "")
            )
            // truncate to the maximum length allowed by the ZGW APIs
            .take(TOELICHTING_MAX_LENGTH)

    private fun logZaakCouldNotBeCreatedWarning(
        processType: String,
        productaanvraagDimpact: ProductaanvraagDimpact,
        exception: RuntimeException
    ) {
        LOG.log(
            Level.WARNING,
            "Failed to create a zaak of process type: '$processType' for productaanvraag with PDF '${productaanvraagDimpact.pdf}'",
            exception
        )
    }

    private fun isBrpEnabled(zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration) =
        zaaktypeCmmnConfiguration.zaaktypeBetrokkeneParameters?.brpKoppelen ?: false

    private fun isKvkEnabled(zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration) =
        zaaktypeCmmnConfiguration.zaaktypeBetrokkeneParameters?.kvkKoppelen ?: false

    private fun generateProductaanvraagDescription(productaanvraag: ProductaanvraagDimpact) =
        "Productaanvraag '${productaanvraag.bron.naam}' with characteristics '${productaanvraag.bron.kenmerk}' and " +
            "type '${productaanvraag.type}'"
}
