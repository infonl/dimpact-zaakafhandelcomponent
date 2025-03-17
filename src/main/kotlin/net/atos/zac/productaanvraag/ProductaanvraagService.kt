/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.objects.model.generated.ModelObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.client.zgw.zrc.model.Vestiging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.Zaak.TOELICHTING_MAX_LENGTH
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.flowable.bpmn.BpmnService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.identity.IdentityService
import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import net.atos.zac.productaanvraag.model.generated.Betrokkene
import net.atos.zac.productaanvraag.model.generated.Geometry
import net.atos.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import net.atos.zac.productaanvraag.util.BetalingStatusEnumJsonAdapter
import net.atos.zac.productaanvraag.util.GeometryTypeEnumJsonAdapter
import net.atos.zac.productaanvraag.util.IndicatieMachtigingEnumJsonAdapter
import net.atos.zac.productaanvraag.util.RolOmschrijvingGeneriekEnumJsonAdapter
import net.atos.zac.productaanvraag.util.convertToZgwPoint
import net.atos.zac.util.JsonbUtil
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ProductaanvraagService @Inject constructor(
    private val objectsClientService: ObjectsClientService,
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val identityService: IdentityService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val inboxDocumentenService: InboxDocumentenService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val cmmnService: CMMNService,
    private val bpmnService: BpmnService,
    private val configuratieService: ConfiguratieService
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagService::class.java.name)

        private const val AANVRAAG_PDF_TITEL = "Aanvraag PDF"
        private const val AANVRAAG_PDF_BESCHRIJVING = "PDF document met de aanvraag gegevens van de zaak"
        private const val PRODUCTAANVRAAG_FORMULIER_VELD_AANVRAAGGEGEVENS = "aanvraaggegevens"
        private const val PRODUCTAANVRAAG_FORMULIER_VELD_BRON = "bron"
        private const val PRODUCTAANVRAAG_FORMULIER_VELD_TYPE = "type"
        private const val ROLTYPE_OMSCHRIJVING_INITIATOR = "Initiator"
        private const val ROLTYPE_OMSCHRIJVING_BEHANDELAAR = "Behandelaar"
        private const val ROL_TOELICHTING = "Overgenomen vanuit de product aanvraag"
        private const val ZAAK_INFORMATIEOBJECT_REDEN =
            "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
    }

    @Suppress("TooGenericExceptionCaught")
    fun handleProductaanvraag(productaanvraagObjectUUID: UUID) {
        try {
            objectsClientService.readObject(productaanvraagObjectUUID).let {
                if (isProductaanvraagDimpact(it)) {
                    LOG.info { "Handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID" }
                    handleProductaanvraagDimpact(it)
                }
            }
        } catch (runtimeException: RuntimeException) {
            LOG.log(
                Level.WARNING,
                "Failed to handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID",
                runtimeException
            )
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
                // register our custom enum JSON adapters because by default enums are deserialized using the enum's name
                // instead of the value and this fails because in the generated model classes the enum names are
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

    fun pairProductaanvraagWithZaak(productaanvraag: ModelObject, zaakUrl: URI) {
        ZaakobjectProductaanvraag(zaakUrl, productaanvraag.url)
            .let(zrcClientService::createZaakobject)
    }

    fun pairAanvraagPDFWithZaak(productaanvraag: ProductaanvraagDimpact, zaakUrl: URI) {
        ZaakInformatieobject().apply {
            informatieobject = productaanvraag.pdf
            zaak = zaakUrl
            titel = AANVRAAG_PDF_TITEL
            beschrijving = AANVRAAG_PDF_BESCHRIJVING
        }.let {
            LOG.fine("Creating zaakinformatieobject: $it")
            zrcClientService.createZaakInformatieobject(it, ZAAK_INFORMATIEOBJECT_REDEN)
        }
    }

    fun pairBijlagenWithZaak(bijlageURIs: List<URI>, zaakUrl: URI) =
        bijlageURIs.map(drcClientService::readEnkelvoudigInformatieobject).forEach { bijlage ->
            ZaakInformatieobject().apply {
                informatieobject = bijlage.url
                zaak = zaakUrl
                titel = bijlage.titel
                beschrijving = bijlage.beschrijving
            }.run {
                zrcClientService.createZaakInformatieobject(this, ZAAK_INFORMATIEOBJECT_REDEN)
            }
        }

    /**
     * Adds all betrokkenen which are present in the provided productaanvraag to the zaak for the set
     * of provided role types, [Betrokkene.RoltypeOmschrijving] or [Betrokkene.roltypeOmschrijving], but only for those
     * role types which are defined in the zaaktype of the specified zaak.
     * An exception is made for betrokkenen of role type (behandelaar)[Betrokkene.RolOmschrijvingGeneriek.BEHANDELAAR]].
     * Behandelaar betrokkenen cannot be set from a productaanvraag.
     *
     * For all supported role types except for (initiator)[Betrokkene.RolOmschrijvingGeneriek.INITIATOR] there can be
     * multiple betrokkenen. Either a (BSN)[Betrokkene.inpBsn] or a (KVK vestigingsnummer)[Betrokkene.vestigingsNummer]
     * are supported as identification of the betrokkene.
     *
     * If a product request person specifies only a [Betrokkene.rolOmschrijvingGeneriek] field then that is used
     * If a product request person specifies only a [Betrokkene.roltypeOmschrijving] field then that is used
     * If a product request person specifies both [Betrokkene.rolOmschrijvingGeneriek] and
     * [Betrokkene.roltypeOmschrijving] fields, then the [Betrokkene.roltypeOmschrijving] field is used,
     * because it is more specific
     *
     * @param productaanvraag the productaanvraag to add the betrokkenen from
     * @param zaak the zaak to add the betrokkenen to
     */
    private fun addBetrokkenen(
        productaanvraag: ProductaanvraagDimpact,
        zaak: Zaak
    ) {
        var initiatorAdded = false
        productaanvraag.betrokkenen?.forEach {
            initiatorAdded = if (it.roltypeOmschrijving != null) {
                addBetrokkenenWithRole(it, initiatorAdded, zaak)
            } else {
                addBetrokkenenWithGenericRole(it, initiatorAdded, zaak)
            }
        }
    }

    private fun addBetrokkenenWithRole(
        betrokkene: Betrokkene,
        initiatorAdded: Boolean,
        zaak: Zaak
    ): Boolean {
        when (betrokkene.roltypeOmschrijving) {
            ROLTYPE_OMSCHRIJVING_INITIATOR -> {
                if (initiatorAdded) {
                    LOG.warning(
                        "Multiple initiator betrokkenen found in productaanvraag for zaak '$zaak'. " +
                            "Only the first one will be used."
                    )
                } else {
                    addBetrokkene(betrokkene, ROLTYPE_OMSCHRIJVING_INITIATOR, zaak)
                }
                return true
            }
            ROLTYPE_OMSCHRIJVING_BEHANDELAAR -> {
                LOG.warning(
                    "Betrokkene with role 'Behandelaar' is not supported in the mapping from a productaanvraag. " +
                        "No betrokkene role created for zaak '$zaak'."
                )
            }

            else -> {
                addBetrokkene(betrokkene, betrokkene.roltypeOmschrijving, zaak)
            }
        }

        return initiatorAdded
    }

    private fun addBetrokkenenWithGenericRole(
        betrokkene: Betrokkene,
        initiatorAdded: Boolean,
        zaak: Zaak
    ): Boolean {
        when (betrokkene.rolOmschrijvingGeneriek) {
            Betrokkene.RolOmschrijvingGeneriek.ADVISEUR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.ADVISEUR, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.BELANGHEBBENDE -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.BELANGHEBBENDE, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.BESLISSER -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.BESLISSER, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.INITIATOR -> {
                if (initiatorAdded) {
                    LOG.warning(
                        "Multiple initiator betrokkenen found in productaanvraag for zaak '$zaak'. " +
                            "Only the first one will be used."
                    )
                } else {
                    addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.INITIATOR, zaak)
                }
                return true
            }

            Betrokkene.RolOmschrijvingGeneriek.KLANTCONTACTER -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.KLANTCONTACTER, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.MEDE_INITIATOR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.MEDE_INITIATOR, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.ZAAKCOORDINATOR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.ZAAKCOORDINATOR, zaak)
            }

            else -> {
                LOG.warning(
                    "Betrokkene with generic role '${betrokkene.rolOmschrijvingGeneriek}' is not supported in the " +
                        "mapping from a productaanvraag. No role created for zaak '$zaak'."
                )
            }
        }

        return initiatorAdded
    }

    private fun addBetrokkeneGeneriek(
        betrokkene: Betrokkene,
        roltypeOmschrijvingGeneriek: OmschrijvingGeneriekEnum,
        zaak: Zaak
    ) {
        ztcClientService.findRoltypen(zaak.zaaktype, roltypeOmschrijvingGeneriek)
            .also { logRoltypenWarnings(it, zaak, roltypeOmschrijvingGeneriek.toString(), true) }
            .firstOrNull()?.let { addRoles(betrokkene, it, zaak, roltypeOmschrijvingGeneriek.toString(), true) }
    }

    private fun addBetrokkene(
        betrokkene: Betrokkene,
        roltypeOmschrijving: String?,
        zaak: Zaak
    ) {
        ztcClientService.findRoltypen(zaak.zaaktype, roltypeOmschrijving)
            .also { logRoltypenWarnings(it, zaak, roltypeOmschrijving) }
            .firstOrNull()?.let { addRoles(betrokkene, it, zaak, roltypeOmschrijving) }
            ?: LOG.warning(
                "Betrokkene with role '$roltypeOmschrijving' is not supported in the mapping from a " +
                    "productaanvraag. No betrokkene role created for zaak '$zaak'."
            )
    }

    private fun logRoltypenWarnings(
        types: List<RolType>,
        zaak: Zaak,
        roltypeOmschrijving: String?,
        generiek: Boolean = false
    ) {
        val prefix = if (generiek) "generic " else ""

        if (types.isEmpty()) {
            LOG.warning(
                "No roltypen found for zaaktype '${zaak.zaaktype}' and ${prefix}roltype description " +
                    "'$roltypeOmschrijving'. No betrokkene role created for zaak '$zaak'."
            )
        } else if (types.size > 1) {
            LOG.warning(
                "Multiple ${prefix}roltypen found for zaaktype '${zaak.zaaktype}', ${prefix}roltype description " +
                    "'$roltypeOmschrijving' and zaak '$zaak'. " +
                    "Using the first one (description: '${types.first().omschrijving}')."
            )
        }
    }

    private fun addRoles(
        betrokkene: Betrokkene,
        type: RolType,
        zaak: Zaak,
        roltypeOmschrijving: String?,
        generiek: Boolean = false
    ) {
        val prefix = if (generiek) "generic " else ""
        when {
            betrokkene.inpBsn != null -> {
                addNatuurlijkPersoonRole(
                    type,
                    betrokkene.inpBsn,
                    zaak.url
                )
            }

            betrokkene.vestigingsNummer != null -> {
                addVestigingRole(
                    type,
                    betrokkene.vestigingsNummer,
                    zaak.url
                )
            }

            else -> {
                LOG.warning(
                    "Betrokkene with ${prefix}roletype description `$roltypeOmschrijving` does not contain a BSN " +
                        "or KVK vestigingsnummer. No betrokkene role created for zaak '$zaak'."
                )
            }
        }
    }

    private fun addNatuurlijkPersoonRole(
        rolType: RolType,
        bsn: String,
        zaak: URI,
    ) = zrcClientService.createRol(
        RolNatuurlijkPersoon(
            zaak,
            rolType,
            ROL_TOELICHTING,
            NatuurlijkPersoon(bsn)
        )
    )

    private fun addVestigingRole(
        rolType: RolType,
        vestigingsNummer: String,
        zaak: URI
    ) = zrcClientService.createRol(
        RolVestiging(
            zaak,
            rolType,
            ROL_TOELICHTING,
            Vestiging(vestigingsNummer)
        )
    )

    private fun assignZaak(zaak: Zaak, zaakafhandelParameters: ZaakafhandelParameters) {
        zaakafhandelParameters.groepID?.let {
            LOG.info("Assigning zaak ${zaak.uuid} to group: '${zaakafhandelParameters.groepID}'")
            zrcClientService.createRol(creeerRolGroep(zaakafhandelParameters.groepID, zaak))
        }
        zaakafhandelParameters.gebruikersnaamMedewerker?.let {
            LOG.info("Assigning zaak ${zaak.uuid}: to assignee: '$it'")
            zrcClientService.createRol(creeerRolMedewerker(zaakafhandelParameters.gebruikersnaamMedewerker, zaak))
        }
    }

    private fun creeerRolGroep(groepID: String, zaak: Zaak): RolOrganisatorischeEenheid =
        identityService.readGroup(groepID).let {
            OrganisatorischeEenheid().apply {
                identificatie = it.id
                naam = it.name
            }
        }.let { organistatorischeEenheid ->
            RolOrganisatorischeEenheid(
                zaak.url,
                ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR),
                "Behandelend groep van de zaak",
                organistatorischeEenheid
            )
        }

    private fun creeerRolMedewerker(behandelaarGebruikersnaam: String, zaak: Zaak): RolMedewerker =
        identityService.readUser(behandelaarGebruikersnaam).let {
            Medewerker().apply {
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

    private fun deleteInboxDocument(documentUUID: UUID) =
        inboxDocumentenService.find(documentUUID).ifPresent { inboxDocumentenService.delete(it.id) }

    private fun findZaaktypeByIdentificatie(zaaktypeIdentificatie: String) =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .firstOrNull { it.identificatie == zaaktypeIdentificatie }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    private fun handleProductaanvraagDimpact(productaanvraagObject: ModelObject) {
        LOG.fine { "Start handling productaanvraag with object URL: ${productaanvraagObject.url}" }
        val productaanvraag = getProductaanvraag(productaanvraagObject)
        zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
            productaanvraag.type
        ).let { zaakafhandelparameters ->
            if (zaakafhandelparameters.isNotEmpty()) {
                try {
                    if (zaakafhandelparameters.size > 1) {
                        LOG.warning(
                            "Multiple zaakafhandelparameters found for productaanvraag type '${productaanvraag.type}'. " +
                                "This indicates that the zaakafhandelparameters are not configured correctly. " +
                                "There should be at most only one active zaakafhandelparameters for each productaanvraagtype. " +
                                "Using the first one with zaakttpe UUID: '${zaakafhandelparameters.first().zaakTypeUUID}' " +
                                "and zaaktype omschrijving: '${zaakafhandelparameters.first().zaaktypeOmschrijving}'."
                        )
                    }
                    val firstZaakafhandelparameters = zaakafhandelparameters.first()
                    LOG.fine { "Creating a zaak using a CMMN case with zaaktype UUID: '$firstZaakafhandelparameters'" }
                    startZaakWithCmmnProcess(
                        firstZaakafhandelparameters.zaakTypeUUID,
                        productaanvraag,
                        productaanvraagObject
                    )
                } catch (exception: RuntimeException) {
                    logZaakCouldNotBeCreatedWarning("CMMN", productaanvraag, exception)
                }
            } else {
                findZaaktypeByIdentificatie(productaanvraag.type)?.let {
                    try {
                        LOG.fine { "Creating a zaak using a BPMN proces with zaaktype: $it" }
                        startZaakWithBpmnProcess(it, productaanvraag, productaanvraagObject)
                    } catch (exception: RuntimeException) {
                        logZaakCouldNotBeCreatedWarning("BPMN", productaanvraag, exception)
                    }
                } ?: run {
                    LOG.info("No zaaktype found for productaanvraag-Dimpact type '${productaanvraag.type}'. No zaak was created.")
                    registreerInbox(productaanvraag, productaanvraagObject)
                }
            }
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

    private fun pairProductaanvraagInfoWithZaak(
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject,
        zaak: Zaak
    ) {
        pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
        pairAanvraagPDFWithZaak(productaanvraag, zaak.url)
        productaanvraag.bijlagen?.let { pairBijlagenWithZaak(it, zaak.url) }
        addBetrokkenen(productaanvraag, zaak)
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
            pdfUri.extractUuid().let {
                inboxProductaanvraag.aanvraagdocumentUUID = it
                deleteInboxDocument(it)
            }
        }
        productaanvraag.bijlagen?.let { bijlagen ->
            inboxProductaanvraag.aantalBijlagen = bijlagen.size
            bijlagen.forEach { deleteInboxDocument(it.extractUuid()) }
        }

        inboxProductaanvraagService.create(inboxProductaanvraag)
    }

    private fun startZaakWithBpmnProcess(
        zaaktype: ZaakType,
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val createdZaak = Zaak().apply {
            this.zaaktype = zaaktype.url
            bronorganisatie = configuratieService.readBronOrganisatie()
            verantwoordelijkeOrganisatie = configuratieService.readBronOrganisatie()
            startdatum = LocalDate.now()
        }.let(zgwApiService::createZaak)
        bpmnService.readProcessDefinitionByprocessDefinitionKey(zaaktype.referentieproces.naam).let {
            zrcClientService.createRol(creeerRolGroep(it.description, createdZaak))
        }
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, createdZaak)
        bpmnService.startProcess(
            createdZaak,
            zaaktype,
            getAanvraaggegevens(productaanvraagObject)
        )
    }

    private fun startZaakWithCmmnProcess(
        zaaktypeUuid: UUID,
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val formulierData = getAanvraaggegevens(productaanvraagObject)
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUuid)
        val createdZaak = Zaak().apply {
            this.zaaktype = zaaktype.url
            startdatum = productaanvraagObject.record.startAt
            bronorganisatie = configuratieService.readBronOrganisatie()
            communicatiekanaalNaam = ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER
            verantwoordelijkeOrganisatie = configuratieService.readBronOrganisatie()
            productaanvraag.zaakgegevens?.let { zaakgegevens ->
                // we currently only support 'POINT' geometries
                zaakgegevens.geometry?.takeIf { it.type == Geometry.Type.POINT }?.let {
                    zaakgeometrie = it.convertToZgwPoint()
                }
                zaakgegevens.omschrijving?.let { omschrijving = it }
            }
            toelichting = generateZaakExplanationFromProductaanvraag(productaanvraag)
        }.let(zgwApiService::createZaak)

        LOG.fine("Creating zaak using the ZGW API: $createdZaak")
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUuid)
        assignZaak(createdZaak, zaakafhandelParameters)
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, createdZaak)
        cmmnService.startCase(createdZaak, zaaktype, zaakafhandelParameters, formulierData)
    }

    private fun generateZaakExplanationFromProductaanvraag(productaanvraag: ProductaanvraagDimpact): String =
        (
            "Aangemaakt vanuit ${productaanvraag.bron.naam} met kenmerk '${productaanvraag.bron.kenmerk}'." +
                (productaanvraag.zaakgegevens?.toelichting?.let { " $it" } ?: "")
            )
            // truncate to maximum length allowed by the ZGW APIs
            .take(TOELICHTING_MAX_LENGTH)

    private fun logZaakCouldNotBeCreatedWarning(
        processType: String,
        productaanvraag: ProductaanvraagDimpact,
        exception: RuntimeException
    ) {
        LOG.log(
            Level.WARNING,
            "Failed to create a zaak of process type: '$processType' for productaanvraag with PDF '${productaanvraag.pdf}'",
            exception
        )
    }
}
