/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.healthcheck

import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.healthcheck.model.BuildInformation
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import java.time.ZonedDateTime

@Suppress("LongParameterList")
fun createZaaktypeInrichtingscheck(
    zaaktype: ZaakType = createZaakType(),
    statustypeIntakeAanwezig: Boolean = true,
    statustypeInBehandelingAanwezig: Boolean = true,
    statustypeHeropendAanwezig: Boolean = true,
    statustypeAfgerondAanwezig: Boolean = true,
    statustypeAfgerondLaatsteVolgnummer: Boolean = true,
    statustypeAanvullendeInformatieVereist: Boolean = true,
    initiatorrollen: Int = 1,
    behandelaarrollen: Int = 1,
    rolOverigeAanwezig: Boolean = true,
    informatieobjecttypeEmailAanwezig: Boolean = true,
    resultaattypeAanwezig: Boolean = true,
    zaakafhandelParametersValide: Boolean = true,
    besluittypeAanwezig: Boolean = true,
    brpDoelbindingenAanwezig: Boolean = true,
) = ZaaktypeInrichtingscheck(zaaktype).apply {
    isStatustypeIntakeAanwezig = statustypeIntakeAanwezig
    isStatustypeInBehandelingAanwezig = statustypeInBehandelingAanwezig
    isStatustypeHeropendAanwezig = statustypeHeropendAanwezig
    isStatustypeAfgerondAanwezig = statustypeAfgerondAanwezig
    isStatustypeAfgerondLaatsteVolgnummer = statustypeAfgerondLaatsteVolgnummer
    isStatustypeAanvullendeInformatieVereist = statustypeAanvullendeInformatieVereist
    aantalInitiatorrollen = initiatorrollen
    aantalBehandelaarrollen = behandelaarrollen
    isRolOverigeAanwezig = rolOverigeAanwezig
    isInformatieobjecttypeEmailAanwezig = informatieobjecttypeEmailAanwezig
    isResultaattypeAanwezig = resultaattypeAanwezig
    isZaakafhandelParametersValide = zaakafhandelParametersValide
    isBesluittypeAanwezig = besluittypeAanwezig
    isBrpDoelbindingenAanwezig = brpDoelbindingenAanwezig
}

fun createBuildInformation(
    commit: String = "fakeCommit",
    buildId: String = "fakeBuildId",
    buildDatumTijd: ZonedDateTime = ZonedDateTime.now(),
    versienummer: String = "fakeVersion",
) = BuildInformation(
    commit = commit,
    buildId = buildId,
    buildDateTime = buildDatumTijd,
    versionNumber = versienummer
)
