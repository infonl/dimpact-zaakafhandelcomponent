/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.NotFoundException
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import net.atos.zac.productaanvraag.util.BetalingStatusEnumJsonAdapter
import net.atos.zac.productaanvraag.util.GeometryTypeEnumJsonAdapter
import net.atos.zac.productaanvraag.util.IndicatieMachtigingEnumJsonAdapter
import net.atos.zac.productaanvraag.util.RolOmschrijvingGeneriekEnumJsonAdapter
import net.atos.zac.util.JsonbUtil
import nl.info.client.kvk.util.KVK_VESTIGINGSNUMMER_LENGTH
import nl.info.client.kvk.util.isValidKvkVestigingsnummer
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.app.zaak.exception.ExplanationRequiredException
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import nl.info.zac.productaanvraag.exception.ProductaanvraagNotSupportedException
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.Geometry
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.toGeoJSONGeometry
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

const val TOELICHTING_MAX_LENGTH = 1000

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
    private val productaanvraagEmailService: ProductaanvraagEmailService,
    private val cmmnService: CMMNService,
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

    fun handleProductaanvraag(productaanvraagObjectUUID: UUID) {
        productaanvraagObjectUUID
            .runCatching(objectsClientService::readObject)
            .onFailure { LOG.fine("Unable to read object with UUID: $productaanvraagObjectUUID") }
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
     * Adds all betrokkenen that are present in the provided productaanvraag to the zaak for the set
     * of provided role types, [Betrokkene.rolOmschrijvingGeneriek] or [Betrokkene.roltypeOmschrijving], but only for those
     * role types which are defined in the zaaktype of the specified zaak.
     * An exception is made for betrokkenen of role type (behandelaar)[Betrokkene.RolOmschrijvingGeneriek.BEHANDELAAR]].
     * Behandelaar betrokkenen cannot be set from a productaanvraag.
     *
     * For all supported role types except for (initiator)[Betrokkene.RolOmschrijvingGeneriek.INITIATOR] there can be
     * multiple betrokkenen. Either a (BSN)[Betrokkene.inpBsn] or a (KVK vestigingsnummer)[Betrokkene.vestigingsNummer]
     * are supported as identification of the betrokkene.
     *
     * The following logic applies for adding betrokkenen:
     * - If a betrokkene only specifies a [Betrokkene.rolOmschrijvingGeneriek] field then that is used
     * - If a betrokkene only specifies a [Betrokkene.roltypeOmschrijving] field then that is used
     * - If a betrokkene specifies both a [Betrokkene.rolOmschrijvingGeneriek] and a
     * [Betrokkene.roltypeOmschrijving], then the [Betrokkene.roltypeOmschrijving] field is used,
     * because it is more specific
     *
     * @param productaanvraag the productaanvraag to add the betrokkenen from
     * @param zaak the zaak to add the betrokkenen to
     * @return betrokkene added as initiator
     */
    private fun addInitiatorAndBetrokkenenToZaak(
        productaanvraag: ProductaanvraagDimpact,
        zaak: Zaak
    ): Betrokkene? {
        var initiatorBetrokkene: Betrokkene? = null
        productaanvraag.betrokkenen?.forEach {
            val betrokkeneAddedAsInitiator = if (it.roltypeOmschrijving != null) {
                addBetrokkenenWithRole(it, initiatorBetrokkene != null, zaak)
            } else {
                addBetrokkenenWithGenericRole(it, initiatorBetrokkene != null, zaak)
            }
            if (initiatorBetrokkene == null && betrokkeneAddedAsInitiator) {
                initiatorBetrokkene = it
            }
        }
        return initiatorBetrokkene
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
        roltypeOmschrijving: String,
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
        when {
            types.isEmpty() -> LOG.warning(
                "No roltypen found for zaaktype '${zaak.zaaktype}' and ${prefix}roltype description " +
                    "'$roltypeOmschrijving'. No betrokkene role created for zaak '$zaak'."
            )
            types.size > 1 -> LOG.warning(
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
        roltypeOmschrijving: String,
        genericRolType: Boolean = false
    ) {
        when {
            betrokkene.inpBsn != null -> addNatuurlijkPersoonRole(type, betrokkene.inpBsn, zaak.url)
            betrokkene.vestigingsNummer != null -> addVestigingRole(type, betrokkene.vestigingsNummer, zaak.url)
            else -> {
                val prefix = if (genericRolType) "generic " else ""
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
            NatuurlijkPersoonIdentificatie().apply { this.inpBsn = bsn }
        )
    )

    private fun addVestigingRole(
        rolType: RolType,
        vestigingsNummer: String,
        zaak: URI
    ): Rol<*> {
        if (!vestigingsNummer.isValidKvkVestigingsnummer()) {
            throw ProductaanvraagNotSupportedException(
                "Invalid KVK vestigingsnummer: '$vestigingsNummer'. " +
                    "It should be a $KVK_VESTIGINGSNUMMER_LENGTH-digit number."
            )
        }
        return zrcClientService.createRol(
            // note that niet-natuurlijk persoon roles can be used both for KVK niet-natuurlijk personen (with an RSIN)
            // as well as for KVK vestigingen
            RolNietNatuurlijkPersoon(
                zaak,
                rolType,
                ROL_TOELICHTING,
                NietNatuurlijkPersoonIdentificatie().apply { this.vestigingsNummer = vestigingsNummer }
            )
        )
    }

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

    private fun creeerRolGroep(groepID: String, zaak: Zaak): RolOrganisatorischeEenheid {
        val group = identityService.readGroup(groepID)
        val organisatieEenheid = OrganisatorischeEenheidIdentificatie().apply {
            identificatie = group.id
            naam = group.name
        }
        return RolOrganisatorischeEenheid(
            zaak.url,
            ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR),
            "Behandelend groep van de zaak",
            organisatieEenheid
        )
    }

    private fun creeerRolMedewerker(behandelaarGebruikersnaam: String, zaak: Zaak): RolMedewerker =
        identityService.readUser(behandelaarGebruikersnaam).let {
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

    private fun deleteInboxDocument(documentUUID: UUID) =
        inboxDocumentenService.find(documentUUID).ifPresent { inboxDocumentenService.delete(it.id) }

    /**
     * Handles a productaanvraag-Dimpact [ModelObject] by creating a zaak and starting a CMMN process for it
     * if there is a zaakafhandelparameters configured for the productaanvraagtype.
     * Else it will create an 'inbox productaanvraag'.
     * Note that starting a BPMN process from a productaanvraag is not supported yet.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleProductaanvraagDimpact(productaanvraagObject: ModelObject) {
        LOG.fine { "Start handling productaanvraag with object URL: ${productaanvraagObject.url}" }
        val productaanvraag = getProductaanvraag(productaanvraagObject)
        val zaakafhandelparameters = zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
            productaanvraag.type
        )
        if (zaakafhandelparameters.isEmpty()) {
            LOG.info(
                "No zaaktype found for productaanvraag-Dimpact type '${productaanvraag.type}'. No zaak was created."
            )
            registreerInbox(productaanvraag, productaanvraagObject)
            return
        }

        try {
            if (zaakafhandelparameters.size > 1) {
                LOG.warning(
                    "Multiple zaakafhandelparameters found for productaanvraag type '${productaanvraag.type}'. " +
                        "Using the first one with zaaktype UUID: '${zaakafhandelparameters.first().zaakTypeUUID}' " +
                        "and zaaktype omschrijving: '${zaakafhandelparameters.first().zaaktypeOmschrijving}'."
                )
            }
            val firstZaakafhandelparameters = zaakafhandelparameters.first()
            productaanvraag.betrokkenen?.run {
                validateBetrokkenenForZaakafhandelparameters(
                    betrokkenen = this,
                    productaanvraagObject = productaanvraagObject,
                    zaakafhandelparameters = firstZaakafhandelparameters
                )
            }
            LOG.fine {
                "Creating a zaak using a CMMN case with zaaktype UUID: '${firstZaakafhandelparameters.zaakTypeUUID}'"
            }
            startZaakWithCmmnProcess(
                zaaktypeUuid = firstZaakafhandelparameters.zaakTypeUUID,
                productaanvraag = productaanvraag,
                productaanvraagObject = productaanvraagObject
            )
        } catch (exception: ExplanationRequiredException) {
            logZaakCouldNotBeCreatedWarning("CMMN", productaanvraag, exception)
        } catch (exception: RuntimeException) {
            logZaakCouldNotBeCreatedWarning("CMMN", productaanvraag, exception)
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
                // note that ZAC currently only supports 'POINT' zaakgeometries
                zaakgegevens.geometry?.takeIf { it.type == Geometry.Type.POINT }?.let {
                    zaakgeometrie = it.toGeoJSONGeometry()
                }
                zaakgegevens.omschrijving?.let { omschrijving = it }
            }
            toelichting = generateZaakExplanationFromProductaanvraag(productaanvraag)
        }.let(zgwApiService::createZaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUuid)
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, createdZaak)
        assignZaak(createdZaak, zaakafhandelParameters)
        val betrokkene = addInitiatorAndBetrokkenenToZaak(productaanvraag, createdZaak)
        cmmnService.startCase(createdZaak, zaaktype, zaakafhandelParameters, formulierData)
        try {
            productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(
                createdZaak,
                betrokkene,
                zaakafhandelParameters
            )
        } catch (exception: NotFoundException) {
            LOG.log(Level.WARNING, "Failed to send confirmation email for zaak ${createdZaak.uuid}", exception)
        }
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

    private fun validateBetrokkenenForZaakafhandelparameters(
        betrokkenen: List<Betrokkene>,
        productaanvraagObject: ModelObject,
        zaakafhandelparameters: ZaakafhandelParameters
    ) {
        betrokkenen.forEach {
            if (!zaakafhandelparameters.betrokkeneKoppelingen.brpKoppelen) {
                it.inpBsn?.run {
                    throw ProductaanvraagNotSupportedException(
                        "The BRP koppeling is not enabled for zaakafhandelparameters with zaaktype UUID " +
                            "'${zaakafhandelparameters.zaakTypeUUID}'. " +
                            "Productaanvraag with URL '${productaanvraagObject.url}' cannot be processed because " +
                            "it contains one or more betrokkenen with a BSN identifier."
                    )
                }
            }
            if (!zaakafhandelparameters.betrokkeneKoppelingen.kvkKoppelen) {
                it.vestigingsNummer?.run {
                    throw ProductaanvraagNotSupportedException(
                        "The KVK koppeling is not enabled for zaakafhandelparameters with zaaktype UUID " +
                            "'${zaakafhandelparameters.zaakTypeUUID}'. " +
                            "Productaanvraag with URL '${productaanvraagObject.url}' cannot be processed because " +
                            "it contains one or more betrokkenen with a KVK vestigingsnummer identifier."
                    )
                }
            }
        }
    }
}
