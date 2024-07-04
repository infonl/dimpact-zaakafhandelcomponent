/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.ORObject
import net.atos.client.vrl.VrlClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.CMMNService
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
import net.atos.zac.util.UriUtil.uuidFromURI
import net.atos.zac.zaaksturing.ZaakafhandelParameterBeheerService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.collections4.ListUtils
import org.flowable.engine.repository.ProcessDefinition
import java.net.URI
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ProductaanvraagService @Inject constructor(
    private val objectsClientService: ObjectsClientService,
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZRCClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val vrlClientService: VrlClientService,
    private val identityService: IdentityService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val inboxDocumentenService: InboxDocumentenService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val cmmnService: CMMNService,
    private val bpmnService: BPMNService,
    private val configuratieService: ConfiguratieService
) {
    companion object {
        const val AANVRAAG_PDF_TITEL: String = "Aanvraag PDF"
        const val AANVRAAG_PDF_BESCHRIJVING: String = "PDF document met de aanvraag gegevens van de zaak"
        const val ZAAK_INFORMATIEOBJECT_REDEN: String =
            "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"

        private val LOG: Logger = Logger.getLogger(ProductaanvraagService::class.java.name)
        private const val ROL_TOELICHTING = "Overgenomen vanuit de product aanvraag"
        private const val PRODUCT_AANVRAAG_FORMULIER_DATA_VELD = "aanvraaggegevens"
        private const val ZAAK_DESCRIPTION_FORMAT = "Aangemaakt vanuit %s met kenmerk '%s'"

        /**
         * Maximum length of the description field in a zaak as defined by the ZGW ZRC API.
         */
        private const val ZAAK_DESCRIPTION_MAX_LENGTH = 80
    }

    fun handleProductaanvraag(productaanvraagObjectUUID: UUID?) {
        try {
            val productaanvraagObject = objectsClientService.readObject(productaanvraagObjectUUID)
            if (isProductaanvraagDimpact(productaanvraagObject)) {
                LOG.info { "Handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID" }
                verwerkProductaanvraag(productaanvraagObject)
            }
        } catch (exception: RuntimeException) {
            LOG.log(
                Level.WARNING,
                "Failed to handle productaanvraag-Dimpact object UUID: $productaanvraagObjectUUID",
                exception
            )
        }
    }

    private fun isProductaanvraagDimpact(productaanvraagObject: ORObject): Boolean {
        // check if the required attributes defined by the 'Productaanvraag Dimpact' JSON schema are present
        // this is a bit of a poor man's solution because we are currently 'misusing' the very generic Objects API
        // to store specific productaanvraag JSON data
        val productAanvraagData: Map<String, Any> = productaanvraagObject.record.data
        return productAanvraagData.containsKey("bron") &&
                productAanvraagData.containsKey("type") &&
                productAanvraagData.containsKey("aanvraaggegevens")
    }

    private fun verwerkProductaanvraag(productaanvraagObject: ORObject) {
        LOG.fine { "Start handling productaanvraag with object URL: ${productaanvraagObject.url}" }
        val productaanvraag = getProductaanvraag(productaanvraagObject)
        val zaaktypeUUID = zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(
            productaanvraag.type
        )
        if (zaaktypeUUID.isPresent) {
            try {
                LOG.fine { "Creating a zaak using a CMMN case. Zaaktype: ${zaaktypeUUID.get()}" }
                registreerZaakMetCMMNCase(zaaktypeUUID.get(), productaanvraag, productaanvraagObject)
            } catch (exception: RuntimeException) {
                warning("CMMN", productaanvraag, exception)
            }
        } else {
            val zaaktype = findZaaktypeByIdentificatie(productaanvraag.type)
            if (zaaktype.isPresent) {
                try {
                    LOG.fine { "Creating a zaak using a BPMN proces. Zaaktype: ${zaaktype.get()}" }
                    registreerZaakMetBPMNProces(zaaktype.get(), productaanvraag, productaanvraagObject)
                } catch (ex: RuntimeException) {
                    warning("BPMN", productaanvraag, ex)
                }
            } else {
                LOG.info(
                    message(
                        productaanvraag,
                        "No zaaktype found for productaanvraag-Dimpact type '${productaanvraag.type}'. No zaak was created."
                    )
                )
                registreerInbox(productaanvraag, productaanvraagObject)
            }
        }
    }

    private fun warning(type: String, productaanvraag: ProductaanvraagDimpact, exception: RuntimeException) {
        LOG.log(
            Level.WARNING,
            message(productaanvraag, "Er is iets fout gegaan bij het aanmaken van een $type-zaak."),
            exception
        )
    }

    private fun message(productaanvraag: ProductaanvraagDimpact, message: String) =
        "Productaanvraag ${productaanvraag.aanvraaggegevens}: $message"

    private fun registreerZaakMetBPMNProces(
        zaaktype: ZaakType,
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ORObject
    ) {
        val formulierData = getFormulierData(productaanvraagObject)
        var zaak = Zaak()
        zaak.zaaktype = zaaktype.url
        zaak.bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
        zaak.verantwoordelijkeOrganisatie = ConfiguratieService.BRON_ORGANISATIE
        zaak.startdatum = LocalDate.now()
        zaak = zgwApiService.createZaak(zaak)
        val PROCESS_DEFINITION_KEY = "test-met-proces"
        val processDefinition: ProcessDefinition =
            bpmnService.readProcessDefinitionByprocessDefinitionKey(PROCESS_DEFINITION_KEY)
        zrcClientService.createRol(creeerRolGroep(processDefinition.description, zaak))
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, zaak)
        bpmnService.startProcess(zaak, zaaktype, formulierData, PROCESS_DEFINITION_KEY)
    }

    fun getFormulierData(productaanvraagObject: ORObject): Map<String, Any> {
        val formulierData = mutableMapOf<String, Any>()
        (productaanvraagObject.record.data[PRODUCT_AANVRAAG_FORMULIER_DATA_VELD] as Map<*, *>)
            .forEach { (_, velden) ->
                formulierData.putAll(
                    velden as Map<String, Any>
                )
            }
        return formulierData
    }

    fun getProductaanvraag(productaanvraagObject: ORObject): ProductaanvraagDimpact {
        try {
            JsonbBuilder.create(
                JsonbConfig() // register our custom enum JSON adapters because by default enums are deserialized using the enum's name
                    // instead of the value and this fails because in the generated model classes the enum names are
                    // capitalized and the values are not
                    .withAdapters(
                        IndicatieMachtigingEnumJsonAdapter(),
                        RolOmschrijvingGeneriekEnumJsonAdapter(),
                        BetalingStatusEnumJsonAdapter(),
                        GeometryTypeEnumJsonAdapter()
                    )
            ).use {
                return it.fromJson(
                    JsonbUtil.JSONB.toJson(productaanvraagObject.record.data),
                    ProductaanvraagDimpact::class.java
                )
            }
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }

    private fun addInitiator(bsn: String, zaak: URI, zaaktype: URI) {
        val initiator = ztcClientService.readRoltype(OmschrijvingGeneriekEnum.INITIATOR, zaaktype)
        val rolNatuurlijkPersoon = RolNatuurlijkPersoon(
            zaak,
            initiator,
            ROL_TOELICHTING,
            NatuurlijkPersoon(bsn)
        )
        zrcClientService.createRol(rolNatuurlijkPersoon)
    }

    private fun registreerInbox(productaanvraag: ProductaanvraagDimpact, productaanvraagObject: ORObject) {
        val inboxProductaanvraag = InboxProductaanvraag()
        inboxProductaanvraag.productaanvraagObjectUUID = productaanvraagObject.uuid
        inboxProductaanvraag.type = productaanvraag.type
        inboxProductaanvraag.ontvangstdatum = productaanvraagObject.record.registrationAt
        ListUtils.emptyIfNull(productaanvraag.betrokkenen).stream()
            .filter { betrokkene -> betrokkene.rolOmschrijvingGeneriek == Betrokkene.RolOmschrijvingGeneriek.INITIATOR } // there can be at most only one initiator for a particular zaak so even if there are multiple (theorically possible)
            // we are only interested in the first one
            .findFirst()
            .ifPresent { betrokkene -> inboxProductaanvraag.initiatorID = betrokkene.inpBsn }

        if (productaanvraag.pdf != null) {
            val aanvraagDocumentUUID = uuidFromURI(productaanvraag.pdf)
            inboxProductaanvraag.aanvraagdocumentUUID = aanvraagDocumentUUID
            deleteInboxDocument(aanvraagDocumentUUID)
        }
        val bijlagen = ListUtils.emptyIfNull(productaanvraag.bijlagen)
        inboxProductaanvraag.aantalBijlagen = bijlagen.size
        bijlagen.forEach(Consumer { bijlage: URI? -> deleteInboxDocument(uuidFromURI(bijlage)) })
        inboxProductaanvraagService.create(inboxProductaanvraag)
    }

    private fun deleteInboxDocument(documentUUID: UUID) =
        inboxDocumentenService.find(documentUUID)
            .ifPresent { inboxDocumentenService.delete(it.id) }

    private fun registreerZaakMetCMMNCase(
        zaaktypeUuid: UUID,
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ORObject
    ) {
        val formulierData = getFormulierData(productaanvraagObject)
        val zaak = Zaak()
        val zaaktype: ZaakType = ztcClientService.readZaaktype(zaaktypeUuid)
        zaak.zaaktype = zaaktype.url
        val zaakOmschrijving = getZaakOmschrijving(productaanvraag)
        zaak.omschrijving = zaakOmschrijving
        // note that we leave the 'toelichting' field empty for a zaak created from a productaanvraag
        zaak.startdatum = productaanvraagObject.record.startAt
        zaak.bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
        zaak.verantwoordelijkeOrganisatie = ConfiguratieService.BRON_ORGANISATIE
        vrlClientService.findCommunicatiekanaal(ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER).ifPresent {
            zaak.communicatiekanaal = it.url
        }
        val zaakgegevens = productaanvraag.zaakgegevens
        if (zaakgegevens?.geometry != null && zaakgegevens.geometry.type == Geometry.Type.POINT) {
            zaak.zaakgeometrie = zaakgegevens.geometry.convertToZgwPoint()
        }

        LOG.fine("Creating zaak using the ZGW API: $zaak")
        val createdZaak = zgwApiService.createZaak(zaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUuid)
        toekennenZaak(createdZaak, zaakafhandelParameters)
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, createdZaak)
        cmmnService.startCase(createdZaak, zaaktype, zaakafhandelParameters, formulierData)
    }

    private fun pairProductaanvraagInfoWithZaak(
        productaanvraag: ProductaanvraagDimpact,
        productaanvraagObject: ORObject,
        zaak: Zaak
    ) {
        pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
        pairAanvraagPDFWithZaak(productaanvraag, zaak.url)
        productaanvraag.bijlagen?.let { pairBijlagenWithZaak(it, zaak.url) }
        ListUtils.emptyIfNull(productaanvraag.betrokkenen).stream()
            .filter { it.rolOmschrijvingGeneriek == Betrokkene.RolOmschrijvingGeneriek.INITIATOR }
            // there can be at most only one initiator for a particular zaak so even if there are multiple (theorically possible)
            // we are only interested in the first one
            .findFirst()
            .ifPresent {
                addInitiator(
                    it.inpBsn,
                    zaak.url,
                    zaak.zaaktype
                )
            }
    }

    fun pairProductaanvraagWithZaak(productaanvraag: ORObject, zaakUrl: URI) {
        val zaakobject = ZaakobjectProductaanvraag(zaakUrl, productaanvraag.url)
        zrcClientService.createZaakobject(zaakobject)
    }

    fun pairAanvraagPDFWithZaak(productaanvraag: ProductaanvraagDimpact, zaakUrl: URI?) {
        val zaakInformatieobject = ZaakInformatieobject()
        zaakInformatieobject.informatieobject = productaanvraag.pdf
        zaakInformatieobject.zaak = zaakUrl
        zaakInformatieobject.titel = AANVRAAG_PDF_TITEL
        zaakInformatieobject.beschrijving = AANVRAAG_PDF_BESCHRIJVING

        LOG.fine("Creating zaak informatieobject: $zaakInformatieobject")
        zrcClientService.createZaakInformatieobject(zaakInformatieobject, ZAAK_INFORMATIEOBJECT_REDEN)
    }

    fun pairBijlagenWithZaak(bijlageURIs: List<URI>, zaakUrl: URI) =
        bijlageURIs.forEach {
            val bijlage = drcClientService.readEnkelvoudigInformatieobject(it)
            val zaakInformatieobject = ZaakInformatieobject()
            zaakInformatieobject.informatieobject = bijlage.url
            zaakInformatieobject.zaak = zaakUrl
            zaakInformatieobject.titel = bijlage.titel
            zaakInformatieobject.beschrijving = bijlage.beschrijving
            zrcClientService.createZaakInformatieobject(zaakInformatieobject, ZAAK_INFORMATIEOBJECT_REDEN)
        }

    private fun toekennenZaak(zaak: Zaak, zaakafhandelParameters: ZaakafhandelParameters) {
        zaakafhandelParameters.groepID?.let {
            LOG.info(String.format("Zaak %s: toegekend aan groep '%s'", zaak.uuid, zaakafhandelParameters.groepID))
            zrcClientService.createRol(creeerRolGroep(zaakafhandelParameters.groepID, zaak))
        }
        zaakafhandelParameters.gebruikersnaamMedewerker?.let {
            LOG.info(
                String.format(
                    "Zaak %s: toegekend aan behandelaar '%s'",
                    zaak.uuid,
                    zaakafhandelParameters.gebruikersnaamMedewerker
                )
            )
            zrcClientService.createRol(creeerRolMedewerker(zaakafhandelParameters.gebruikersnaamMedewerker, zaak))
        }
    }

    private fun creeerRolGroep(groepID: String, zaak: Zaak): RolOrganisatorischeEenheid {
        val group = identityService.readGroup(groepID)
        val groep = OrganisatorischeEenheid()
        groep.identificatie = group.id
        groep.naam = group.name
        val roltype = ztcClientService.readRoltype(OmschrijvingGeneriekEnum.BEHANDELAAR, zaak.zaaktype)
        return RolOrganisatorischeEenheid(zaak.url, roltype, "Behandelend groep van de zaak", groep)
    }

    private fun creeerRolMedewerker(behandelaarGebruikersnaam: String, zaak: Zaak): RolMedewerker {
        val user = identityService.readUser(behandelaarGebruikersnaam)
        val medewerker = Medewerker()
        medewerker.identificatie = user.id
        medewerker.voorletters = user.firstName
        medewerker.achternaam = user.lastName
        val roltype = ztcClientService.readRoltype(OmschrijvingGeneriekEnum.BEHANDELAAR, zaak.zaaktype)
        return RolMedewerker(zaak.url, roltype, "Behandelaar van de zaak", medewerker)
    }

    private fun findZaaktypeByIdentificatie(zaaktypeIdentificatie: String): Optional<ZaakType> {
        return ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI()).stream()
            .filter { it.identificatie == zaaktypeIdentificatie }
            .findFirst()
    }

    private fun getZaakOmschrijving(productaanvraag: ProductaanvraagDimpact): String {
        val zaakOmschrijving = String.format(
            ZAAK_DESCRIPTION_FORMAT,
            productaanvraag.bron.naam,
            productaanvraag.bron.kenmerk
        )
        if (zaakOmschrijving.length > ZAAK_DESCRIPTION_MAX_LENGTH) {
            // we truncate the zaak description to the maximum length allowed by the ZGW ZRC API
            // or else it will not be accepted by the ZGW API implementation component
            LOG.warning(
                String.format(
                    "Truncating zaak description '%s' to the maximum length allowed by the ZGW ZRC API",
                    zaakOmschrijving
                )
            )
            return zaakOmschrijving.substring(0, ZAAK_DESCRIPTION_MAX_LENGTH)
        } else {
            return zaakOmschrijving
        }
    }
}

