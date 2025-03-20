/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.healthcheck

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.healthcheck.exception.BuildInformationException
import net.atos.zac.healthcheck.model.BuildInformatie
import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck
import net.atos.zac.util.time.LocalDateUtil
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.Afleidingswijze
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTable.Systeem
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.time.ZonedDateTime
import java.util.Locale
import java.util.Optional
import java.util.UUID

@Singleton
@NoArgConstructor
class HealthCheckService @Inject constructor(
    @ConfigProperty(name = "BRANCH_NAME")
    private val branchName: Optional<String?>,
    @ConfigProperty(name = "COMMIT_HASH")
    private val commitHash: Optional<String?>,
    @ConfigProperty(name = "VERSION_NUMBER")
    private val versionNumber: Optional<String?>,

    private val referenceTableService: ReferenceTableService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterService,
    private val ztcClientService: ZtcClientService,
) {
    companion object {
        private const val BUILD_TIMESTAMP_FILE = "/build_timestamp.txt"
        private const val DEV_BUILD_ID = "dev"
    }

    private var buildInformatie: BuildInformatie = createBuildInformatie()

    fun bestaatCommunicatiekanaalEformulier() =
        referenceTableService.readReferenceTable(Systeem.COMMUNICATIEKANAAL.name).values.any {
            ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER == it.name
        }

    fun controleerZaaktype(zaaktypeUrl: URI): ZaaktypeInrichtingscheck {
        ztcClientService.resetCacheTimeToNow()
        ztcClientService.readZaaktype(zaaktypeUrl).let { zaaktype ->
            zaaktype.url.extractUuid().let {
                return inrichtingscheck(it, zaaktype)
            }
        }
    }

    private fun inrichtingscheck(zaaktypeUuid: UUID, zaaktype: ZaakType): ZaaktypeInrichtingscheck =
        zaakafhandelParameterBeheerService.readZaakafhandelParameters(zaaktypeUuid).let { zaakafhandelParams ->
            return ZaaktypeInrichtingscheck(zaaktype).apply {
                isZaakafhandelParametersValide = zaakafhandelParams.isValide
            }.also {
                controleerZaaktypeStatustypeInrichting(it)
                controleerZaaktypeResultaattypeInrichting(it)
                controleerZaaktypeBesluittypeInrichting(it)
                controleerZaaktypeRoltypeInrichting(it)
                controleerZaaktypeInformatieobjecttypeInrichting(it)
            }
        }

    fun readBuildInformatie() = buildInformatie

    private fun createBuildInformatie(): BuildInformatie {
        val buildDatumTijdFile = File(BUILD_TIMESTAMP_FILE)
        val buildDateTime = if (buildDatumTijdFile.exists()) {
            try {
                ZonedDateTime.parse(Files.readAllLines(buildDatumTijdFile.toPath()).first())
            } catch (ioException: IOException) {
                throw BuildInformationException("Cannot read build timestamp", ioException)
            }
        } else {
            null
        }
        return BuildInformatie(
            commitHash.orElse(null),
            branchName.orElse(null),
            buildDateTime,
            versionNumber.orElse(DEV_BUILD_ID)
        )
    }

    private fun controleerZaaktypeStatustypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val statustypes = ztcClientService.readStatustypen(zaaktypeInrichtingscheck.zaaktype.url)
        var afgerondVolgnummer = 0
        var hoogsteVolgnummer = 0
        for (statustype in statustypes) {
            if (statustype.volgnummer > hoogsteVolgnummer) {
                hoogsteVolgnummer = statustype.volgnummer
            }
            when (statustype.getOmschrijving()) {
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE ->
                    zaaktypeInrichtingscheck.isStatustypeIntakeAanwezig = true
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING ->
                    zaaktypeInrichtingscheck.isStatustypeInBehandelingAanwezig = true
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND ->
                    zaaktypeInrichtingscheck.isStatustypeHeropendAanwezig = true
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE ->
                    zaaktypeInrichtingscheck.isStatustypeAanvullendeInformatieVereist = true
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AFGEROND -> {
                    afgerondVolgnummer = statustype.volgnummer
                    zaaktypeInrichtingscheck.isStatustypeAfgerondAanwezig = true
                }
            }
        }
        if (afgerondVolgnummer == hoogsteVolgnummer) {
            zaaktypeInrichtingscheck.isStatustypeAfgerondLaatsteVolgnummer = true
        }
    }

    private fun controleerZaaktypeResultaattypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val resultaattypes = ztcClientService.readResultaattypen(zaaktypeInrichtingscheck.zaaktype.url)
        if (resultaattypes.isNotEmpty()) {
            zaaktypeInrichtingscheck.isResultaattypeAanwezig = true
            resultaattypes.forEach {
                val afleidingswijzeName = it.brondatumArchiefprocedure.afleidingswijze.name.lowercase(
                    Locale.getDefault()
                )
                // compare enum values and not the enums themselves because we have multiple functionally
                // identical enums in our Java client code generated by the OpenAPI Generator
                if (Afleidingswijze.VERVALDATUM_BESLUIT.toValue() == afleidingswijzeName ||
                    Afleidingswijze.INGANGSDATUM_BESLUIT.toValue() == afleidingswijzeName
                ) {
                    zaaktypeInrichtingscheck.addResultaattypesMetVerplichtBesluit(it.omschrijving)
                }
            }
        }
    }

    private fun controleerZaaktypeBesluittypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val besluittypes = ztcClientService.readBesluittypen(
            zaaktypeInrichtingscheck.zaaktype.url
        ).filter { LocalDateUtil.dateNowIsBetween(it) }
        if (besluittypes.isNotEmpty()) {
            zaaktypeInrichtingscheck.isBesluittypeAanwezig = true
        }
    }

    private fun controleerZaaktypeRoltypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val roltypes = ztcClientService.listRoltypen(zaaktypeInrichtingscheck.zaaktype.url)
        if (roltypes.isNotEmpty()) {
            roltypes.forEach {
                when (it.omschrijvingGeneriek) {
                    OmschrijvingGeneriekEnum.ADVISEUR,
                    OmschrijvingGeneriekEnum.MEDE_INITIATOR,
                    OmschrijvingGeneriekEnum.BELANGHEBBENDE,
                    OmschrijvingGeneriekEnum.BESLISSER,
                    OmschrijvingGeneriekEnum.KLANTCONTACTER,
                    OmschrijvingGeneriekEnum.ZAAKCOORDINATOR ->
                        zaaktypeInrichtingscheck.isRolOverigeAanwezig = true
                    OmschrijvingGeneriekEnum.BEHANDELAAR ->
                        zaaktypeInrichtingscheck.isRolBehandelaarAanwezig = true
                    OmschrijvingGeneriekEnum.INITIATOR ->
                        zaaktypeInrichtingscheck.isRolInitiatorAanwezig = true
                }
            }
        }
    }

    private fun controleerZaaktypeInformatieobjecttypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val informatieobjecttypes = ztcClientService.readInformatieobjecttypen(zaaktypeInrichtingscheck.zaaktype.url)
        informatieobjecttypes.forEach {
            if (it.isNuGeldig() && ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL == it.omschrijving) {
                zaaktypeInrichtingscheck.isInformatieobjecttypeEmailAanwezig = true
            }
        }
    }
}
