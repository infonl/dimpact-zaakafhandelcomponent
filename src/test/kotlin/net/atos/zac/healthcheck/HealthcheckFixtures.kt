/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.healthcheck

import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakType

@Suppress("LongParameterList")
fun createZaaktypeInrichtingscheck(
    zaaktype: ZaakType = createZaakType(),
    statustypeIntakeAanwezig: Boolean = true,
    statustypeInBehandelingAanwezig: Boolean = true,
    statustypeHeropendAanwezig: Boolean = true,
    statustypeAfgerondAanwezig: Boolean = true,
    statustypeAfgerondLaatsteVolgnummer: Boolean = true,
    statustypeAanvullendeInformatieVereist: Boolean = true,
    rolInitiatorAanwezig: Boolean = true,
    rolBehandelaarAanwezig: Boolean = true,
    rolOverigeAanwezig: Boolean = true,
    informatieobjecttypeEmailAanwezig: Boolean = true,
    resultaattypeAanwezig: Boolean = true,
    zaakafhandelParametersValide: Boolean = true,
    besluittypeAanwezig: Boolean = true,
) = ZaaktypeInrichtingscheck(zaaktype).apply {
    isStatustypeIntakeAanwezig = statustypeIntakeAanwezig
    isStatustypeInBehandelingAanwezig = statustypeInBehandelingAanwezig
    isStatustypeHeropendAanwezig = statustypeHeropendAanwezig
    isStatustypeAfgerondAanwezig = statustypeAfgerondAanwezig
    isStatustypeAfgerondLaatsteVolgnummer = statustypeAfgerondLaatsteVolgnummer
    isStatustypeAanvullendeInformatieVereist = statustypeAanvullendeInformatieVereist
    isRolInitiatorAanwezig = rolInitiatorAanwezig
    isRolBehandelaarAanwezig = rolBehandelaarAanwezig
    isRolOverigeAanwezig = rolOverigeAanwezig
    isInformatieobjecttypeEmailAanwezig = informatieobjecttypeEmailAanwezig
    isResultaattypeAanwezig = resultaattypeAanwezig
    isZaakafhandelParametersValide = zaakafhandelParametersValide
    isBesluittypeAanwezig = besluittypeAanwezig
}
