/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck.model

import nl.info.client.zgw.ztc.model.generated.ZaakType

/**
 * 4 statustype; Intake, In behandeling, Heropend, Afgerond: met Afgerond als laatste statustypevolgnummer
 * min 1 resultaattype
 * Roltypen, omschrijving generiek: initiator en behandelaar. 1 overig roltype
 * Informatieobjecttype: e-mail
 * indien zaak besluit heeft, Besluittype
 *
 * [isValide] covers what a zaaktype needs to be usable at all; [heeftWaarschuwingen] covers configuration
 * mistakes that only disable an optional feature.
 */
class ZaaktypeInrichtingscheck(val zaaktype: ZaakType) {
    var isStatustypeIntakeAanwezig: Boolean = false
    var isStatustypeInBehandelingAanwezig: Boolean = false
    var isStatustypeHeropendAanwezig: Boolean = false
    var isStatustypeAanvullendeInformatieVereist: Boolean = false
    var isStatustypeAfgerondAanwezig: Boolean = false
    var isStatustypeAfgerondLaatsteVolgnummer: Boolean = false
    var isResultaattypeAanwezig: Boolean = false
    var aantalInitiatorroltypen: Int = 0
    var aantalBehandelaarroltypen: Int = 0
    var isRolOverigeAanwezig: Boolean = false
    var isInformatieobjecttypeEmailAanwezig: Boolean = false
    var isBesluittypeAanwezig: Boolean = false
    val resultaattypesMetVerplichtBesluit: MutableList<String?> = ArrayList<String?>()
    var isZaakafhandelParametersValide: Boolean = false
    var isBrpInstellingenCorrect: Boolean = false
    var isZaakspecifiekeAutorisatieEigenschapAanwezig: Boolean = false
    var isZaakspecifiekeAutorisatieRoltypeAanwezig: Boolean = false

    fun addResultaattypesMetVerplichtBesluit(resultaattypeMetVerplichtBesluit: String?) {
        this.resultaattypesMetVerplichtBesluit.add(resultaattypeMetVerplichtBesluit)
    }

    /**
     * Marking a zaak as zaakspecifiek geautoriseerd needs both the eigenschap that marks it and the roltype
     * that grants an individual medewerker access to it. Having only one of the two is a configuration
     * mistake: the zaaktype either cannot be marked at all, or carries a roltype that is never used.
     */
    val isZaakspecifiekeAutorisatieOnvolledig: Boolean
        get() = isZaakspecifiekeAutorisatieEigenschapAanwezig != isZaakspecifiekeAutorisatieRoltypeAanwezig

    /**
     * Configuration mistakes that do not stop the zaaktype from being used, unlike [isValide].
     */
    val heeftWaarschuwingen: Boolean
        get() = isZaakspecifiekeAutorisatieOnvolledig

    val isValide: Boolean
        get() = this.isStatustypeIntakeAanwezig &&
            this.isStatustypeInBehandelingAanwezig &&
            this.isStatustypeHeropendAanwezig &&
            this.isStatustypeAfgerondAanwezig &&
            this.isStatustypeAfgerondLaatsteVolgnummer &&
            this.isStatustypeAanvullendeInformatieVereist &&
            this.aantalInitiatorroltypen == 1 &&
            this.aantalBehandelaarroltypen == 1 &&
            this.isRolOverigeAanwezig &&
            this.isInformatieobjecttypeEmailAanwezig &&
            this.isResultaattypeAanwezig &&
            this.isZaakafhandelParametersValide &&
            this.isBrpInstellingenCorrect &&
            (resultaattypesMetVerplichtBesluit.isEmpty() || this.isBesluittypeAanwezig)
}
