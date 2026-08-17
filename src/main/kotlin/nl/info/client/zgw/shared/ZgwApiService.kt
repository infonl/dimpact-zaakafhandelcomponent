/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolListParameters
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.RolOrganisatorischeEenheid
import nl.info.client.zgw.zrc.model.zaakUUID
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.shared.exception.ResultTypeNotFoundException
import nl.info.client.zgw.shared.exception.StatusTypeNotFoundException
import nl.info.client.zgw.util.convertToDateTime
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
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObjectRequest
import nl.info.client.zgw.zrc.util.toZaakSub
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.NotSupportedException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.logging.Logger

/**
 * Service class for logic that involves using multiple ZGW API's.
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
        private val LOG = Logger.getLogger(ZgwApiService::class.java.getName())

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

    fun getResultaatType(zaakTypeURI: URI, resultaatTypeDescription: String): ResultaatType {
        val resultaattypen = ztcClientService.readResultaattypen(zaakTypeURI)
        return filterResultaattype(
            resultaattypes = resultaattypen,
            description = resultaatTypeDescription,
            zaaktypeURI = zaakTypeURI
        )
    }

    fun getResultaatType(resultaatTypeUUID: UUID): ResultaatType = ztcClientService.readResultaattype(resultaatTypeUUID)

    /**
     * Closes a [Zaak].
     *
     * This function will also process the brondatum procedure when needed for
     * the given [resultaatTypeUUID].
     *
     * @param zaak [Zaak] to be closed.
     * @param resultaatTypeUUID [UUID] the UUID of the resultaat for closing the [Zaak].
     * @param description [String] of the [Resultaat] and [Status].
     * @param brondatum [LocalDate]
     */
    fun closeZaak(zaak: Zaak, resultaatTypeUUID: UUID, description: String?, brondatum: LocalDate? = null) {
        val resultaatType = getResultaatType(resultaatTypeUUID)
        val resultaat = ResultaatSub().apply {
            resultaattype = resultaatType.url
            this.toelichting = description
        }
        val statusType = getStatusTypeEind(zaak.zaaktype)
        val status = StatusSub().apply {
            statustype = statusType.url
            datumStatusGezet = ZonedDateTime.now().toOffsetDateTime()
            statustoelichting = description
        }

        val zaakSub = zaak.toZaakSub()

        val zaakAfsluiten = ZaakAfsluiten().apply {
            this.zaak = zaakSub
            this.resultaat = resultaat
            this.status = status
        }
        processBrondatumProcedure(zaak, resultaatType, brondatum)
        zrcClientService.closeCase(zaak.uuid, zaakAfsluiten)
    }

    fun setBrondatum(zaak: Zaak, brondatum: LocalDate?) {
        getResultaatType(zaak)?.let {
            when (it.brondatumArchiefprocedure?.afleidingswijze) {
                AfleidingswijzeEnum.EIGENSCHAP -> processBrondatumProcedure(zaak, it, brondatum)
                else -> {
                    LOG.warning {
                        "Failed to set brondatum to $brondatum for afleidingswijze brondatum " +
                        "${it.brondatumArchiefprocedure?.afleidingswijze} for zaak ${zaak.identificatie}"
                    }
                    throw NotSupportedException(ErrorCode.ERROR_CODE_AFLEIDINGSWIJZE_BRONDATUM_NOT_SUPPORTED)
                }
            }
        }
    }

    private fun getResultaatType(zaak: Zaak): ResultaatType? {
        if (zaak.resultaat == null) return null
        return zrcClientService.readResultaat(zaak.resultaat).let {
            ztcClientService.readResultaattype(it.resultaattype)
        }
    }

  private fun processBrondatumProcedure(zaak: Zaak, resultaatType: ResultaatType, brondatum: LocalDate?) {
        val brondatumArchiefprocedure = resultaatType.brondatumArchiefprocedure ?: return

        when (brondatumArchiefprocedure.afleidingswijze) {
            AfleidingswijzeEnum.EIGENSCHAP -> {
                if (brondatumArchiefprocedure.datumkenmerk.isNullOrBlank() || brondatum == null) {
                    return
                }
                this.upsertEigenschapToZaak(
                    brondatumArchiefprocedure.datumkenmerk,
                    brondatum.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    zaak
                )
            }
            else -> Unit
        }
    }

    private fun upsertEigenschapToZaak(eigenschap: String, waarde: String, zaak: Zaak) {
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
     * Create [EnkelvoudigInformatieObject] and [ZaakInformatieObject] for [Zaak].
     *
     * @param zaak [Zaak].
     * @param enkelvoudigInformatieObjectCreateLockRequest [EnkelvoudigInformatieObject] to be created.
     * @param titel Titel of the new [ZaakInformatieObject].
     * @param beschrijving Beschrijving of the new [ZaakInformatieObject].
     * @param omschrijvingVoorwaardenGebruiksrechten Used to create the [Gebruiksrechten] for the to be created
     * [EnkelvoudigInformatieObject]
     * @return Created [ZaakInformatieObject].
     */
    fun createZaakInformatieobjectForZaak(
        zaak: Zaak,
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest,
        titel: String,
        beschrijving: String?,
        omschrijvingVoorwaardenGebruiksrechten: String?
    ): ZaakInformatieObject {
        val newInformatieObjectData = drcClientService.createEnkelvoudigInformatieobject(
            enkelvoudigInformatieObjectCreateLockRequest
        )
        // Gebruiksrechten are required for every created zaakinformatieobject or else
        // the zaak in question can no longer be aborted or closed (OpenZaak will return a 400 error on aborting or closing in that case).
        val gebruiksrechten = Gebruiksrechten().apply {
            informatieobject = newInformatieObjectData.url
            startdatum = newInformatieObjectData.creatiedatum.convertToDateTime().toOffsetDateTime()
            omschrijvingVoorwaarden = omschrijvingVoorwaardenGebruiksrechten
        }
        drcClientService.createGebruiksrechten(gebruiksrechten)

        val zaakInformatieObjectRequest = ZaakInformatieObjectRequest().apply {
            informatieobject = newInformatieObjectData.url
            this.zaak = zaak.url
            this.titel = titel
            this.beschrijving = beschrijving
        }
        return zrcClientService.createZaakInformatieobject(zaakInformatieObjectRequest)
    }

    /**
     * Delete [ZaakInformatieObject] which relates [EnkelvoudigInformatieObject] and [Zaak] with zaakUUID. When the
     * [EnkelvoudigInformatieObject] has no other related [ZaakInformatieObject]s then it is also deleted.
     *
     * @param enkelvoudigInformatieobject [EnkelvoudigInformatieObject]
     * @param zaakUUID UUID of a [Zaak]
     * @param reason Explanation why the [EnkelvoudigInformatieObject] is to be removed; may be null.
     */
    fun removeEnkelvoudigInformatieObjectFromZaak(
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        zaakUUID: UUID,
        reason: String?
    ) {
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieobject)
        // delete the relationship of the EnkelvoudigInformatieobject with the zaak.
        zaakInformatieobjecten
            .filter { it.zaakUUID == zaakUUID }
            .forEach { zrcClientService.deleteZaakInformatieobject(it.uuid, reason, ZAAK_OBJECT_DELETION_PREFIX) }

        // if the EnkelvoudigInformatieobject has no relationship(s) with other zaken, it can be deleted.
        if (zaakInformatieobjecten.all { it.zaakUUID == zaakUUID }) {
            drcClientService.deleteEnkelvoudigInformatieobject(enkelvoudigInformatieobject.url.extractUuid())
        }
    }

    /**
     * Find [RolOrganisatorischeEenheid] for [Zaak] with initiator [OmschrijvingGeneriekEnum].
     *
     * @param zaak [Zaak].
     * @param roles pre-fetched roles for [zaak], to avoid a redundant `listRollen` call when the caller
     * already fetched all roles for the zaak. When 'null', the roles are fetched here.
     * @return [RolOrganisatorischeEenheid] or 'null'.
     */
    fun findGroepForZaak(zaak: Zaak, roles: List<Rol<*>>? = null): RolOrganisatorischeEenheid? =
        findBehandelaarRoleForZaak(zaak, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID, roles)?.let {
            it as RolOrganisatorischeEenheid
        }

    /**
     * Find [RolMedewerker] for [Zaak] with initiator [OmschrijvingGeneriekEnum].
     *
     * @param zaak [Zaak]
     * @param roles pre-fetched roles for [zaak], to avoid a redundant `listRollen` call when the caller
     * already fetched all roles for the zaak. When 'null', the roles are fetched here.
     * @return [RolMedewerker] or 'null' if the rol medewerker could not be found.
     */
    fun findBehandelaarMedewerkerRoleForZaak(zaak: Zaak, roles: List<Rol<*>>? = null): RolMedewerker? =
        findBehandelaarRoleForZaak(zaak, BetrokkeneTypeEnum.MEDEWERKER, roles)?.let {
            it as RolMedewerker
        }

    /**
     * @param roles pre-fetched roles for [zaak], to avoid a redundant `listRollen` call when the caller
     * already fetched all roles for the zaak. When 'null', the roles are fetched here.
     */
    fun findInitiatorRoleForZaak(zaak: Zaak, roles: List<Rol<*>>? = null): Rol<*>? {
        val roleTypes = ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR).also {
            if (it.size > 1) {
                LOG.warning(
                    "Multiple initiator role types found for zaaktype: '${zaak.zaaktype}', using the first one."
                )
            }
        }
        return roleTypes.firstOrNull()?.let { rolType ->
            val matchingRoles = (
                roles?.filter { it.roltype == rolType.url }
                    ?: zrcClientService.listRollen(RolListParameters(zaak.url, rolType.url)).results()
                ).also {
                check(it.size <= 1) {
                    "More than one initiator role found for zaak with UUID: '${zaak.uuid}' (count: ${it.size})"
                }
            }
            matchingRoles.firstOrNull()
        }
    }

    private fun findBehandelaarRoleForZaak(
        zaak: Zaak,
        betrokkeneType: BetrokkeneTypeEnum,
        roles: List<Rol<*>>? = null
    ): Rol<*>? {
        val roleTypes = ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR).also {
            if (it.size > 1) {
                LOG.warning(
                    "Multiple behandelaar role types found for zaaktype: '${zaak.zaaktype}', using the first one."
                )
            }
        }
        return roleTypes.firstOrNull()?.let { roleType ->
            val matchingRoles = (
                roles?.filter { it.roltype == roleType.url && it.betrokkeneType == betrokkeneType }
                    ?: zrcClientService.listRollen(
                        RolListParameters(zaak.url, roleType.url, betrokkeneType)
                    ).results()
                ).also {
                check(it.size <= 1) {
                    "More than one behandelaar role found for zaak with UUID: '${zaak.uuid}' (count: ${it.size})"
                }
            }
            matchingRoles.firstOrNull()
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
