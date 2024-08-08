/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.converter

import jakarta.inject.Inject
import net.atos.client.brp.BrpClientService
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
import java.net.URI
import java.util.Objects

@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class DocumentCreationDataConverter @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val brpClientService: BrpClientService,
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
        Data(
            gebruikerData = createGebruikerData(loggedInUser),
            zaakData = createZaakData(documentCreationData.zaak),
            aanvragerData = createAanvragerData(documentCreationData.zaak),
            startformulierData = createStartformulierData(documentCreationData.zaak.url),
            taakData = documentCreationData.taskId?.let { createTaakData(it) }
        )

    private fun createGebruikerData(loggedInUser: LoggedInUser) =
        GebruikerData(
            id = loggedInUser.id,
            naam = loggedInUser.fullName
        )

    private fun createZaakData(zaak: Zaak) =
        ZaakData(
            behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                .map { it.naam }
                .orElse(null),
            communicatiekanaal = zaak.communicatiekanaalNaam,
            einddatum = zaak.einddatum,
            einddatumGepland = zaak.einddatumGepland,
            groep = zgwApiService.findGroepForZaak(zaak)
                .map { it.naam }
                .orElse(null),
            identificatie = zaak.identificatie,
            omschrijving = zaak.omschrijving,
            opschortingReden = if (zaak.isOpgeschort) { zaak.opschorting.reden } else null,
            registratiedatum = zaak.registratiedatum,
            resultaat = zaak.resultaat?.let {
                zrcClientService.readResultaat(it).let { resultaat ->
                    ztcClientService.readResultaattype(resultaat.resultaattype).omschrijving
                }
            },
            startdatum = zaak.startdatum,
            status = zaak.status?.let { statusUri ->
                zrcClientService.readStatus(statusUri).let {
                    ztcClientService.readStatustype(it.statustype).omschrijving
                }
            },
            toelichting = zaak.toelichting,
            uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening,
            verlengingReden = if (zaak.isVerlengd) { zaak.verlenging.reden } else null,
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding?.toString(),
            zaaktype = ztcClientService.readZaaktype(zaak.zaaktype).omschrijving
        )

    private fun createAanvragerData(zaak: Zaak): AanvragerData? =
        zgwApiService.findInitiatorRoleForZaak(zaak)
            .map { convertToAanvragerData(it) }
            .orElse(null)

    private fun convertToAanvragerData(initiator: Rol<*>): AanvragerData? =
        when (initiator.betrokkeneType) {
            BetrokkeneType.NATUURLIJK_PERSOON -> createAanvragerDataNatuurlijkPersoon(initiator.identificatienummer)
            BetrokkeneType.VESTIGING -> createAanvragerDataVestiging(initiator.identificatienummer)
            BetrokkeneType.NIET_NATUURLIJK_PERSOON -> createAanvragerDataNietNatuurlijkPersoon(
                initiator.identificatienummer
            )
            else -> error(
                "Initiator of type '${initiator.betrokkeneType.toValue()}' is not supported"
            )
        }

    private fun createAanvragerDataNatuurlijkPersoon(bsn: String): AanvragerData? =
        brpClientService.findPersoon(bsn)?.let { convertToAanvragerDataPersoon(it) }

    private fun convertToAanvragerDataPersoon(persoon: Persoon) =
        AanvragerData(
            naam = persoon.naam?.volledigeNaam,
            straat = persoon.verblijfplaats?.let { it as? Adres }?.verblijfadres?.officieleStraatnaam,
            huisnummer = persoon.verblijfplaats?.let { it as? Adres }?.verblijfadres?.let { convertToHuisnummer(it) },
            postcode = persoon.verblijfplaats?.let { it as? Adres }?.verblijfadres?.postcode,
            woonplaats = persoon.verblijfplaats?.let { it as? Adres }?.verblijfadres?.woonplaats
        )

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
            AanvragerData(
                naam = resultaatItem.naam,
                straat = it.straatnaam,
                huisnummer = convertToHuisnummer(resultaatItem),
                postcode = it.postcode,
                woonplaats = it.plaats
            )
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

    private fun convertToStartformulierData(zaakobject: Zaakobject) =
        objectsClientService.readObject(URIUtil.getUUID(zaakobject.getObject())).let { productAaanvraagObject ->
            StartformulierData(
                productAanvraagtype = productaanvraagService.getProductaanvraag(productAaanvraagObject).type,
                data = productaanvraagService.getAanvraaggegevens(productAaanvraagObject)
            )
        }

    private fun createTaakData(taskId: String): TaakData =
        flowableTaskService.readTask(taskId).let { taskInfo ->
            TaakData(
                naam = taskInfo.name,
                behandelaar = taskInfo.assignee?.let { identityService.readUser(it).fullName },
                data = TaakVariabelenService.readTaskData(taskInfo)
            )
        }
}
