/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

/**
 * 5 statustype; Intake, In behandeling, Heropend, Wacht op aanvullende informatie,
 * Afgerond: met Afgerond als laatste statustypevolgnummer
 * <br></br>
 * min 1 resultaattype
 * <br></br>
 * Roltypen, omschrijving generiek: initiator en behandelaar. 1 overig roltype
 * <br></br>
 * Informatieobjecttype: e-mail
 * <br></br>
 * indien zaak besluit heeft, Besluittype
 */
class RESTZaaktypeInrichtingscheck {
    var zaaktype: RESTZaaktypeOverzicht? = null

    var statustypeIntakeAanwezig: Boolean = false

    var statustypeInBehandelingAanwezig: Boolean = false

    var statustypeHeropendAanwezig: Boolean = false

    var statustypeAanvullendeInformatieVereist: Boolean = false

    var statustypeAfgerondAanwezig: Boolean = false

    var statustypeAfgerondLaatsteVolgnummer: Boolean = false

    var resultaattypeAanwezig: Boolean = false

    var rolInitiatorAanwezig: Boolean = false

    var rolBehandelaarAanwezig: Boolean = false

    var rolOverigeAanwezig: Boolean = false

    var informatieobjecttypeEmailAanwezig: Boolean = false

    var besluittypeAanwezig: Boolean = false

    var resultaattypesMetVerplichtBesluit: MutableList<String?>? = null

    var zaakafhandelParametersValide: Boolean = false

    var valide: Boolean = false
}
