/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

data class RESTZaaktypeInrichtingscheck(
    var zaaktype: RESTZaaktypeOverzicht,
    var statustypeIntakeAanwezig: Boolean = false,
    var statustypeInBehandelingAanwezig: Boolean = false,
    var statustypeHeropendAanwezig: Boolean = false,
    var statustypeAanvullendeInformatieVereist: Boolean = false,
    var statustypeAfgerondAanwezig: Boolean = false,
    var statustypeAfgerondLaatsteVolgnummer: Boolean = false,
    var resultaattypeAanwezig: Boolean = false,
    var rolInitiatorAanwezig: Boolean = false,
    var rolBehandelaarAanwezig: Boolean = false,
    var rolOverigeAanwezig: Boolean = false,
    var informatieobjecttypeEmailAanwezig: Boolean = false,
    var besluittypeAanwezig: Boolean = false,
    var resultaattypesMetVerplichtBesluit: MutableList<String?>? = null,
    var zaakafhandelParametersValide: Boolean = false,
    var valide: Boolean = false
)
