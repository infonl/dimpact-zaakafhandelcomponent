/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.converter

import jakarta.inject.Inject
import net.atos.client.brp.BRPClientService
import net.atos.client.brp.model.generated.Adres
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.VerblijfadresBinnenland
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.shared.util.URIUtil
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.documentcreation.model.AanvragerData
import net.atos.zac.documentcreation.model.Data
import net.atos.zac.documentcreation.model.DocumentCreationData
import net.atos.zac.documentcreation.model.GebruikerData
import net.atos.zac.documentcreation.model.StartformulierData
import net.atos.zac.documentcreation.model.TaakData
import net.atos.zac.documentcreation.model.ZaakData
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.identity.IdentityService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.util.StringUtil
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.NotImplementedException
import java.net.URI
import java.util.Objects

@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class DocumentCreationDataConverter @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val brpClientService: BRPClientService,
    private val kvkClientService: KvkClientService,
    private val objectsClientService: ObjectsClientService,
    private val flowableTaskService: FlowableTaskService,
    private val identityService: IdentityService,
    private val productaanvraagService: ProductaanvraagService
) {
    companion object {
        const val DATE_FORMAT: String = "dd-MM-yyyy"
    }

    fun createData(documentCreationData: DocumentCreationData, loggedInUser: LoggedInUser) =
        Data().apply {
            gebruikerData = createGebruikerData(loggedInUser)
            zaakData = createZaakData(documentCreationData.zaak)
            aanvragerData = createAanvragerData(documentCreationData.zaak)
            startformulierData = createStartformulierData(documentCreationData.zaak.url)
            taakData = documentCreationData.taskId?.let { createTaakData(it) }
        }

    private fun createGebruikerData(loggedInUser: LoggedInUser) =
        GebruikerData().apply {
            id = loggedInUser.id
            naam = loggedInUser.fullName
        }

    private fun createZaakData(zaak: Zaak): ZaakData {
        val zaakData = ZaakData().apply {
            communicatiekanaal = zaak.communicatiekanaalNaam
            identificatie = zaak.identificatie
            einddatum = zaak.einddatum
            einddatumGepland = zaak.einddatumGepland
            omschrijving = zaak.omschrijving
            registratiedatum = zaak.registratiedatum
            startdatum = zaak.startdatum
            toelichting = zaak.toelichting
            zaaktype = ztcClientService.readZaaktype(zaak.zaaktype).omschrijving
            uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
            zaak.status?.let {
                zrcClientService.readStatus(it).let { status ->
                    this.status = ztcClientService.readStatustype(status.statustype).omschrijving
                }
            }
            zaak.resultaat?.let {
                zrcClientService.readResultaat(it).let { resultaat ->
                    this.resultaat = ztcClientService.readResultaattype(resultaat.resultaattype).omschrijving
                }
            }
            if (zaak.isOpgeschort) {
                opschortingReden = zaak.opschorting.reden
            }
            if (zaak.isVerlengd) {
                verlengingReden = zaak.verlenging.reden
            }
            zaak.vertrouwelijkheidaanduiding?.let {
                vertrouwelijkheidaanduiding = it.toString()
            }
        }
        zgwApiService.findGroepForZaak(zaak)
            .map { it.naam }
            .ifPresent { zaakData.groep = it }
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            .map { it.naam }
            .ifPresent { zaakData.behandelaar = it }
        return zaakData
    }

    private fun createAanvragerData(zaak: Zaak): AanvragerData? =
        zgwApiService.findInitiatorRoleForZaak(zaak)
            .map { convertToAanvragerData(it) }
            .orElse(null)

    private fun convertToAanvragerData(initiator: Rol<*>): AanvragerData =
        when (initiator.betrokkeneType) {
            BetrokkeneType.NATUURLIJK_PERSOON -> createAanvragerDataNatuurlijkPersoon(initiator.identificatienummer)!!
            BetrokkeneType.VESTIGING -> createAanvragerDataVestiging(initiator.identificatienummer)!!
            BetrokkeneType.NIET_NATUURLIJK_PERSOON -> createAanvragerDataNietNatuurlijkPersoon(
                initiator.identificatienummer
            )!!
            else -> throw NotImplementedException(
                "Initiator of type '${initiator.betrokkeneType.toValue()}' is not supported"
            )
        }

    private fun createAanvragerDataNatuurlijkPersoon(bsn: String): AanvragerData? =
        brpClientService.findPersoon(bsn)
            .map { convertToAanvragerDataPersoon(it) }
            .orElse(null)

    private fun convertToAanvragerDataPersoon(persoon: Persoon) =
        AanvragerData().apply {
            persoon.naam?.let { naam = it.volledigeNaam }
            (persoon.verblijfplaats as? Adres)?.verblijfadres?.let {
                straat = it.officieleStraatnaam
                huisnummer = convertToHuisnummer(it)
                postcode = it.postcode
                woonplaats = it.woonplaats
            }
        }

    private fun convertToHuisnummer(verblijfadres: VerblijfadresBinnenland) =
        StringUtil.joinNonBlank(
            Objects.toString(verblijfadres.huisnummer, null),
            verblijfadres.huisnummertoevoeging,
            verblijfadres.huisletter
        )

    private fun createAanvragerDataVestiging(vestigingsnummer: String): AanvragerData? =
        kvkClientService.findVestiging(vestigingsnummer)
            .map { this.convertToAanvragerDataBedrijf(it) }
            .orElse(null)

    private fun createAanvragerDataNietNatuurlijkPersoon(rsin: String): AanvragerData? =
        kvkClientService.findRechtspersoon(rsin)
            .map { this.convertToAanvragerDataBedrijf(it) }
            .orElse(null)

    private fun convertToAanvragerDataBedrijf(resultaatItem: ResultaatItem) =
        resultaatItem.adres.binnenlandsAdres.let {
            AanvragerData().apply {
                naam = resultaatItem.naam
                straat = it.straatnaam
                huisnummer = convertToHuisnummer(resultaatItem)
                postcode = it.postcode
                woonplaats = it.plaats
            }
        }

    private fun convertToHuisnummer(resultaatItem: ResultaatItem) =
        resultaatItem.adres.binnenlandsAdres.let {
            StringUtil.joinNonBlank(
                Objects.toString(it.huisnummer, null),
                it.huisletter
            )
        }

    private fun createStartformulierData(zaakUri: URI): StartformulierData? =
        ZaakobjectListParameters().apply {
            zaak = zaakUri
            objectType = Objecttype.OVERIGE
        }.let { zrcClientService.listZaakobjecten(it) }.results
            .filter { ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE == it.objectTypeOverige }
            .map { convertToStartformulierData(it) }
            .singleOrNull()

    private fun convertToStartformulierData(zaakobject: Zaakobject): StartformulierData {
        val productAaanvraagObject = objectsClientService.readObject(URIUtil.getUUID(zaakobject.getObject()))
        val productAanvraag = productaanvraagService.getProductaanvraag(productAaanvraagObject)
        return StartformulierData().apply {
            productAanvraagtype = productAanvraag.type
            data = productaanvraagService.getAanvraaggegevens(productAaanvraagObject)
        }
    }

    private fun createTaakData(taskId: String): TaakData =
        flowableTaskService.readTask(taskId).let { taskInfo ->
            TaakData().apply {
                naam = taskInfo.name
                taskInfo.assignee?.let { behandelaar = identityService.readUser(it).fullName }
                data = TaakVariabelenService.readTaskData(taskInfo)
            }
        }
}
