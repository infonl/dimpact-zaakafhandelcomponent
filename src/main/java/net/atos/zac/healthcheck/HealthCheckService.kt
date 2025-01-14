/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.healthcheck

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.Afleidingswijze
import net.atos.client.zgw.ztc.model.extensions.isNuGeldig
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ReferenceTable.Systeem
import net.atos.zac.admin.model.ReferenceTableValue
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.healthcheck.model.BuildInformatie
import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.util.time.LocalDateUtil
import org.apache.commons.collections4.CollectionUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Locale
import java.util.Optional
import java.util.function.Consumer

@Singleton
class HealthCheckService {
    private var referenceTableService: ReferenceTableService? = null
    private var zaakafhandelParameterBeheerService: ZaakafhandelParameterService? = null
    private var ztcClientService: ZtcClientService? = null

    @Inject
    constructor(
        referenceTableService: ReferenceTableService,
        zaakafhandelParameterBeheerService: ZaakafhandelParameterService,
        ztcClientService: ZtcClientService
    ) {
        this.referenceTableService = referenceTableService
        this.zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService
        this.ztcClientService = ztcClientService
    }

    /**
     * Default no-arg constructor, required by Weld.
     */
    constructor()

    @Inject
    @ConfigProperty(name = "BRANCH_NAME")
    private val branchName: Optional<String?>? = null

    @Inject
    @ConfigProperty(name = "COMMIT_HASH")
    private val commitHash: Optional<String?>? = null

    @Inject
    @ConfigProperty(name = "VERSION_NUMBER")
    private val versionNumber: Optional<String?>? = null

    private var buildInformatie: BuildInformatie? = null

    fun bestaatCommunicatiekanaalEformulier(): Boolean {
        return referenceTableService!!.readReferenceTable(Systeem.COMMUNICATIEKANAAL.name)
            .values
            .stream()
            .anyMatch { referentieWaarde: ReferenceTableValue? -> ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER == referentieWaarde!!.name }
    }

    fun controleerZaaktype(zaaktypeUrl: URI): ZaaktypeInrichtingscheck {
        ztcClientService!!.resetCacheTimeToNow()
        val zaaktype = ztcClientService!!.readZaaktype(zaaktypeUrl)
        val zaakafhandelParameters = zaakafhandelParameterBeheerService!!.readZaakafhandelParameters(
            zaaktype.getUrl().extractUuid()
        )
        val zaaktypeInrichtingscheck = ZaaktypeInrichtingscheck(zaaktype)
        zaaktypeInrichtingscheck.setZaakafhandelParametersValide(zaakafhandelParameters.isValide())
        controleerZaaktypeStatustypeInrichting(zaaktypeInrichtingscheck)
        controleerZaaktypeResultaattypeInrichting(zaaktypeInrichtingscheck)
        controleerZaaktypeBesluittypeInrichting(zaaktypeInrichtingscheck)
        controleerZaaktypeRoltypeInrichting(zaaktypeInrichtingscheck)
        controleerZaaktypeInformatieobjecttypeInrichting(zaaktypeInrichtingscheck)
        return zaaktypeInrichtingscheck
    }

    fun readBuildInformatie(): BuildInformatie {
        if (buildInformatie == null) {
            buildInformatie = createBuildInformatie()
        }
        return buildInformatie!!
    }

    private fun createBuildInformatie(): BuildInformatie {
        val buildDatumTijd: LocalDateTime?
        val buildDatumTijdFile: File = File(HealthCheckService.Companion.BUILD_TIMESTAMP_FILE)
        if (buildDatumTijdFile.exists()) {
            try {
                buildDatumTijd = DateTimeConverterUtil.convertToLocalDateTime(
                    ZonedDateTime.parse(
                        Files.readAllLines(buildDatumTijdFile.toPath()).getFirst()
                    )
                )
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        } else {
            buildDatumTijd = null
        }
        return BuildInformatie(
            commitHash!!.orElse(null),
            branchName!!.orElse(null),
            buildDatumTijd,
            versionNumber!!.orElse(null)
        )
    }

    private fun controleerZaaktypeStatustypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val statustypes: MutableList<StatusType> = ztcClientService!!.readStatustypen(
            zaaktypeInrichtingscheck.getZaaktype().getUrl()
        )
        var afgerondVolgnummer = 0
        var hoogsteVolgnummer = 0
        for (statustype in statustypes) {
            if (statustype.getVolgnummer() > hoogsteVolgnummer) {
                hoogsteVolgnummer = statustype.getVolgnummer()
            }
            when (statustype.getOmschrijving()) {
                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE -> zaaktypeInrichtingscheck.setStatustypeIntakeAanwezig(
                    true
                )

                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING -> zaaktypeInrichtingscheck
                    .setStatustypeInBehandelingAanwezig(true)

                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND -> zaaktypeInrichtingscheck.setStatustypeHeropendAanwezig(
                    true
                )

                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE -> zaaktypeInrichtingscheck
                    .setStatustypeAanvullendeInformatieVereist(true)

                ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AFGEROND -> {
                    afgerondVolgnummer = statustype.getVolgnummer()
                    zaaktypeInrichtingscheck.setStatustypeAfgerondAanwezig(true)
                }
            }
        }
        if (afgerondVolgnummer == hoogsteVolgnummer) {
            zaaktypeInrichtingscheck.setStatustypeAfgerondLaatsteVolgnummer(true)
        }
    }

    private fun controleerZaaktypeResultaattypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val resultaattypes: MutableList<ResultaatType?> = ztcClientService!!.readResultaattypen(
            zaaktypeInrichtingscheck.getZaaktype().getUrl()
        )
        if (CollectionUtils.isNotEmpty(resultaattypes)) {
            zaaktypeInrichtingscheck.setResultaattypeAanwezig(true)
            resultaattypes.forEach(Consumer { resultaattype: ResultaatType? ->
                val afleidingswijze = resultaattype!!.getBrondatumArchiefprocedure().getAfleidingswijze()
                // compare enum values and not the enums themselves because we have multiple functionally
                // identical enums in our Java client code generated by the OpenAPI Generator
                if (Afleidingswijze.VERVALDATUM_BESLUIT.toValue() == afleidingswijze.name.lowercase(Locale.getDefault()) ||
                    Afleidingswijze.INGANGSDATUM_BESLUIT.toValue() == afleidingswijze.name.lowercase(Locale.getDefault())
                ) {
                    zaaktypeInrichtingscheck.addResultaattypesMetVerplichtBesluit(resultaattype.getOmschrijving())
                }
            })
        }
    }

    private fun controleerZaaktypeBesluittypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val besluittypes = ztcClientService!!.readBesluittypen(
            zaaktypeInrichtingscheck.getZaaktype().getUrl()
        ).stream()
            .filter { besluittype: BesluitType? -> LocalDateUtil.dateNowIsBetween(besluittype) }
            .toList()
        if (CollectionUtils.isNotEmpty(besluittypes)) {
            zaaktypeInrichtingscheck.setBesluittypeAanwezig(true)
        }
    }

    private fun controleerZaaktypeRoltypeInrichting(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
        val roltypes: MutableList<RolType?> =
            ztcClientService!!.listRoltypen(zaaktypeInrichtingscheck.getZaaktype().getUrl())
        if (CollectionUtils.isNotEmpty(roltypes)) {
            roltypes.forEach(Consumer { roltype: RolType? ->
                when (roltype!!.getOmschrijvingGeneriek()) {
                    OmschrijvingGeneriekEnum.ADVISEUR, OmschrijvingGeneriekEnum.MEDE_INITIATOR, OmschrijvingGeneriekEnum.BELANGHEBBENDE, OmschrijvingGeneriekEnum.BESLISSER, OmschrijvingGeneriekEnum.KLANTCONTACTER, OmschrijvingGeneriekEnum.ZAAKCOORDINATOR -> zaaktypeInrichtingscheck
                        .setRolOverigeAanwezig(
                            true
                        )

                    OmschrijvingGeneriekEnum.BEHANDELAAR -> zaaktypeInrichtingscheck.setRolBehandelaarAanwezig(true)
                    OmschrijvingGeneriekEnum.INITIATOR -> zaaktypeInrichtingscheck.setRolInitiatorAanwezig(true)
                }
            })
        }
    }

    private fun controleerZaaktypeInformatieobjecttypeInrichting(
        zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck
    ) {
        val informatieobjecttypes: MutableList<InformatieObjectType?> = ztcClientService!!.readInformatieobjecttypen(
            zaaktypeInrichtingscheck.getZaaktype().getUrl()
        )
        informatieobjecttypes.forEach(Consumer { informatieobjecttype: InformatieObjectType? ->
            if (informatieobjecttype!!.isNuGeldig() && ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL == informatieobjecttype
                    .getOmschrijving()
            ) {
                zaaktypeInrichtingscheck.setInformatieobjecttypeEmailAanwezig(true)
            }
        })
    }

    companion object {
        private const val BUILD_TIMESTAMP_FILE = "/build_timestamp.txt"
    }
}
