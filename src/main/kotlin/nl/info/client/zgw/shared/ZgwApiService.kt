/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.util.DateTimeUtil.convertToDateTime
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolListParameters
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.shared.exception.ResultTypeNotFoundException
import nl.info.client.zgw.shared.exception.StatusTypeNotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.zrc.model.generated.ResultaatSub
import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.zrc.model.generated.StatusSub
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakAfsluiten
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.zrc.model.generated.ZaakSub
import nl.info.client.zgw.zrc.util.toZaakSub
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.Period
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Logger

/**
 * Service class for ZGW API's.
 */
@ApplicationScoped
@Suppress("TooManyFunctions")
@AllOpen
@NoArgConstructor
class ZgwApiService @Inject constructor(
    val ztcClientService: ZtcClientService,
    val zrcClientService: ZrcClientService,
    val drcClientService: DrcClientService
) {
    companion object {
        private val LOG: Logger = Logger.getLogger(ZgwApiService::class.java.getName())

        // Page numbering in ZGW APIs starts with 1
        const val FIRST_PAGE_NUMBER_ZGW_APIS: Int = 1
        const val ZAAK_OBJECT_DELETION_PREFIX = "Verwijderd"
    }

    /**
     * Creates a new [Zaak].
     *
     * @param zaak [Zaak]
     * @return Created [Zaak]
     */
    fun createZaak(zaak: Zaak): Zaak {
        calculateDoorlooptijden(zaak)
        return zrcClientService.createZaak(zaak)
    }

    /**
     * Create [Status] for a given [Zaak] based on [StatusType].omschrijving and with [Status].toelichting.
     *
     * @param zaak [Zaak]
     * @param statusTypeOmschrijving Omschrijving of the [StatusType] of the required [Status].
     * @param statusToelichting Toelichting for thew [Status].
     * @return Created [Status].
     */
    fun createStatusForZaak(
        zaak: Zaak,
        statusTypeOmschrijving: String,
        statusToelichting: String?
    ): StatusSub {
        val statustype = readStatustype(
            statustypes = ztcClientService.readStatustypen(zaak.zaaktype),
            omschrijving = statusTypeOmschrijving,
            zaaktypeURI = zaak.zaaktype
        )
        return createStatusForZaak(zaak.uuid, statustype.url, statusToelichting)
    }

    fun getStatusTypeEind(zaakTypeURI: URI): StatusType {
        val statustypes = ztcClientService.readStatustypen(zaakTypeURI)
        return readStatustypeEind(
            statustypes = statustypes,
            zaaktypeURI = zaakTypeURI
        )
    }

    fun getResultaat(zaakTypeURI: URI, resultaatTypeOmschrijving: String): ResultaatType {
        val resultaattypen = ztcClientService.readResultaattypen(zaakTypeURI)
        return filterResultaattype(
            resultaattypen,
            resultaatTypeOmschrijving,
            zaakTypeURI
        )
    }

    fun getResultaatType(resultaatTypeUUID: UUID): ResultaatType {
        return ztcClientService.readResultaattype(resultaatTypeUUID)
    }

    /**
     * End [Zaak]. Creating a new Eind [Status] for the [Zaak].
     *
     * @param zaak [Zaak]
     * @param eindstatusToelichting Toelichting for the Eind [Status].
     */
    fun endZaak(zaak: Zaak, resultaatTypeOmschrijving: String, eindstatusToelichting: String) {
        val resultaattype = getResultaat(zaak.zaaktype, resultaatTypeOmschrijving)

        closeZaak(zaak, resultaattype.url.extractUuid(), eindstatusToelichting)
    }

    /**
     * End [Zaak]. Creating a new Eind [Status] for the [Zaak].
     *
     * @param zaakUUID UUID of the [Zaak]
     * @param eindstatusToelichting Toelichting for the Eind [Status].
     */
    fun endZaak(zaakUUID: UUID, resultaatTypeOmschrijving: String, eindstatusToelichting: String) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        endZaak(zaak, resultaatTypeOmschrijving, eindstatusToelichting)
    }

    /**
     * Close [Zaak].
     *
     * This method will also process the brondatum procedure when needed for
     * the given `Zaak.resultaattype.brondatumArchiefprocedure.afleidingswijze`.
     *
     * @param zaak [Zaak] to be closed.
     * @param resultaatTypeUUID [UUID] the UUID of the resultaat for closing the [Zaak].
     * @param toelichting [String] of the [Resultaat] and [Status].
     */
    fun closeZaak(zaak: Zaak, resultaatTypeUUID: UUID, toelichting: String?) {
        val resultaatType = getResultaatType(resultaatTypeUUID)
        val resultaat = ResultaatSub().apply {
            resultaattype = resultaatType.url
            this.toelichting = toelichting
        }
        val statusType = getStatusTypeEind(zaak.zaaktype)
        val status = StatusSub().apply {
            statustype = statusType.url
            datumStatusGezet = ZonedDateTime.now().toOffsetDateTime()
            statustoelichting = toelichting
        }

        val zaakSub = zaak.toZaakSub()

        val zaakAfsluiten = ZaakAfsluiten().apply {
            this.zaak = zaakSub
            this.resultaat = resultaat
            this.status = status
        }
        this.processBrondatumProcedure(zaakAfsluiten)
        zrcClientService.closeCase(zaak.uuid, zaakAfsluiten)
    }

    private fun processBrondatumProcedure(zaakAfsluiten: ZaakAfsluiten) {
        val resultaatTypeUUID = zaakAfsluiten.resultaat.resultaattype.extractUuid()
        val resultaattype = ztcClientService.readResultaattype(resultaatTypeUUID)

        val brondatumArchiefprocedure = resultaattype.brondatumArchiefprocedure

        when (brondatumArchiefprocedure.afleidingswijze) {
            AfleidingswijzeEnum.EIGENSCHAP -> {
                if (brondatumArchiefprocedure.datumkenmerk.isNullOrBlank()) {
                    throw InputValidationFailedException(
                        errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
                        message = """
                    'brondatumEigenschap' moet gevuld zijn bij het afhandelen van een zaak met een resultaattype dat
                    een 'brondatumArchiefprocedure' heeft met 'afleidingswijze' 'EIGENSCHAP'.
                        """.trimIndent()
                    )
                }
                this.upsertEigenschapToZaak(
                    brondatumArchiefprocedure.datumkenmerk,
                    brondatumArchiefprocedure.datumkenmerk,
                    zaakAfsluiten.zaak
                )
            }
            else -> null
        }
    }

    private fun upsertEigenschapToZaak(eigenschap: String, waarde: String, zaak: ZaakSub) {
        zrcClientService.listZaakeigenschappen(zaak.uuid).firstOrNull { it.naam == eigenschap }?.let {
            zrcClientService.updateZaakeigenschap(
                zaak.uuid, it.uuid,
                it.apply {
                    this.waarde = waarde
                }
            )
        } ?: run {
            ztcClientService.readEigenschap(zaak.zaaktype, eigenschap).let {
                val zaakEigenschap = ZaakEigenschap().apply {
                    this.eigenschap = it.url
                    this.zaak = zaak.url
                    this.waarde = waarde
                }
                zrcClientService.createEigenschap(zaak.uuid, zaakEigenschap)
            }
        }
    }

    /**
     * Create [EnkelvoudigInformatieObject] and [ZaakInformatieobject] for [Zaak].
     *
     * @param zaak [Zaak].
     * @param enkelvoudigInformatieObjectCreateLockRequest [EnkelvoudigInformatieObject] to be created.
     * @param titel Titel of the new [ZaakInformatieobject].
     * @param beschrijving Beschrijving of the new [ZaakInformatieobject].
     * @param omschrijvingVoorwaardenGebruiksrechten Used to create the [Gebruiksrechten] for the to be created
     * [EnkelvoudigInformatieObject]
     * @return Created [ZaakInformatieobject].
     */
    fun createZaakInformatieobjectForZaak(
        zaak: Zaak,
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest,
        titel: String,
        beschrijving: String?,
        omschrijvingVoorwaardenGebruiksrechten: String?
    ): ZaakInformatieobject {
        val newInformatieObjectData = drcClientService.createEnkelvoudigInformatieobject(
            enkelvoudigInformatieObjectCreateLockRequest
        )
        // Gebruiksrechten are required for every created zaak-informatieobject or else
        // the zaak in question can no longer be aborted or closed (OpenZaak will return a 400 error on aborting or closing in that case).
        val gebruiksrechten = Gebruiksrechten().apply {
            informatieobject = newInformatieObjectData.url
            startdatum = convertToDateTime(newInformatieObjectData.creatiedatum).toOffsetDateTime()
            omschrijvingVoorwaarden = omschrijvingVoorwaardenGebruiksrechten
        }
        drcClientService.createGebruiksrechten(gebruiksrechten)

        val zaakInformatieObject = ZaakInformatieobject().apply {
            this.zaak = zaak.url
            informatieobject = newInformatieObjectData.url
            this.titel = titel
            this.beschrijving = beschrijving
        }
        return zrcClientService.createZaakInformatieobject(zaakInformatieObject)
    }

    /**
     * Delete [ZaakInformatieobject] which relates [EnkelvoudigInformatieObject] and [Zaak] with zaakUUID. When the
     * [EnkelvoudigInformatieObject] has no other related [ZaakInformatieobject]s then it is also deleted.
     *
     * @param enkelvoudigInformatieobject [EnkelvoudigInformatieObject]
     * @param zaakUUID UUID of a [Zaak]
     * @param toelichting Explanation why the [EnkelvoudigInformatieObject] is to be removed; may be null.
     */
    fun removeEnkelvoudigInformatieObjectFromZaak(
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        zaakUUID: UUID,
        toelichting: String?
    ) {
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
            enkelvoudigInformatieobject
        )
        // delete the relationship of the EnkelvoudigInformatieobject with the zaak.
        zaakInformatieobjecten
            .filter { it.zaakUUID == zaakUUID }
            .forEach { zrcClientService.deleteZaakInformatieobject(it.uuid, toelichting, ZAAK_OBJECT_DELETION_PREFIX) }

        // if the EnkelvoudigInformatieobject has no relationship(s) with other zaken it can be deleted.
        if (zaakInformatieobjecten.all { it.zaakUUID == zaakUUID }) {
            drcClientService.deleteEnkelvoudigInformatieobject(enkelvoudigInformatieobject.url.extractUuid())
        }
    }

    /**
     * Find [RolOrganisatorischeEenheid] for [Zaak] with initiator [OmschrijvingGeneriekEnum].
     *
     * @param zaak [Zaak].
     * @return [RolOrganisatorischeEenheid] or 'null'.
     */
    fun findGroepForZaak(zaak: Zaak): RolOrganisatorischeEenheid? =
        findBehandelaarRoleForZaak(zaak, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID)?.let {
            it as RolOrganisatorischeEenheid
        }

    /**
     * Find [RolMedewerker] for [Zaak] with initiator [OmschrijvingGeneriekEnum].
     *
     * @param zaak [Zaak]
     * @return [RolMedewerker] or 'null' if the rol medewerker could not be found.
     */
    fun findBehandelaarMedewerkerRoleForZaak(zaak: Zaak): RolMedewerker? =
        findBehandelaarRoleForZaak(zaak, BetrokkeneTypeEnum.MEDEWERKER)?.let {
            it as RolMedewerker
        }

    fun findInitiatorRoleForZaak(zaak: Zaak): Rol<*>? {
        val roleTypes = ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR).also {
            if (it.size > 1) {
                LOG.warning(
                    "Multiple initiator role types found for zaaktype: '${zaak.zaaktype}', using the first one."
                )
            }
        }
        return roleTypes.firstOrNull()?.let { rolType ->
            val roles = zrcClientService.listRollen(RolListParameters(zaak.url, rolType.url)).results().also {
                check(it.size <= 1) {
                    "More than one initiator role found for zaak with UUID: '${zaak.uuid}' (count: ${it.size})"
                }
            }
            roles.firstOrNull()
        }
    }

    private fun findBehandelaarRoleForZaak(
        zaak: Zaak,
        betrokkeneType: BetrokkeneTypeEnum
    ): Rol<*>? {
        val roleTypes = ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR).also {
            if (it.size > 1) {
                LOG.warning(
                    "Multiple behandelaar role types found for zaaktype: '${zaak.zaaktype}', using the first one."
                )
            }
        }
        return roleTypes.firstOrNull()?.let { roleType ->
            val roles = zrcClientService.listRollen(
                RolListParameters(zaak.url, roleType.url, betrokkeneType)
            ).results().also {
                check(it.size <= 1) {
                    "More than one behandelaar role found for zaak with UUID: '${zaak.uuid}' (count: ${it.size})"
                }
            }
            roles.firstOrNull()
        }
    }

    private fun createStatusForZaak(zaakUUID: UUID, statustypeURI: URI, toelichting: String?): StatusSub {
        val status = StatusSub().apply {
            statustype = statustypeURI
            datumStatusGezet = ZonedDateTime.now().toOffsetDateTime()
            statustoelichting = toelichting
        }
        return zrcClientService.createStatus(zaakUUID, status)
    }

    private fun calculateDoorlooptijden(zaak: Zaak) {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        if (zaaktype.isServicenormAvailable()) {
            zaak.einddatumGepland = zaak.startdatum.plus(Period.parse(zaaktype.servicenorm))
        }
        zaak.uiterlijkeEinddatumAfdoening = zaak.startdatum.plus(Period.parse(zaaktype.doorlooptijd))
    }

    private fun filterResultaattype(
        resultaattypes: List<ResultaatType>,
        description: String,
        zaaktypeURI: URI
    ): ResultaatType = resultaattypes
        .firstOrNull { it.omschrijving == description }
        ?: throw ResultTypeNotFoundException(
            "Resultaattype with description '$description' not found for zaaktype with URI: '$zaaktypeURI'."
        )

    private fun readStatustype(
        statustypes: List<StatusType>,
        omschrijving: String,
        zaaktypeURI: URI
    ): StatusType = statustypes
        .firstOrNull { omschrijving == it.omschrijving }
        ?: throw StatusTypeNotFoundException(
            "Status type with description '$omschrijving' not found for zaaktype with URI: '$zaaktypeURI'."
        )

    private fun readStatustypeEind(
        statustypes: List<StatusType>,
        zaaktypeURI: URI
    ): StatusType = statustypes
        .firstOrNull { it.isEindstatus }
        ?: throw StatusTypeNotFoundException(
            "No status type with 'end state' found for zaaktype with URI: '$zaaktypeURI'."
        )
}
