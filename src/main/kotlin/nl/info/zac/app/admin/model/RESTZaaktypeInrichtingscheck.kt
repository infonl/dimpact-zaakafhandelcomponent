/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import net.atos.zac.app.admin.model.RESTZaaktypeOverzicht

data class RESTZaaktypeInrichtingscheck(
    var zaaktype: RESTZaaktypeOverzicht,
    var statustypeIntakeAanwezig: Boolean = false,
    var statustypeInBehandelingAanwezig: Boolean = false,
    var statustypeHeropendAanwezig: Boolean = false,
    var statustypeAanvullendeInformatieVereist: Boolean = false,
    var statustypeAfgerondAanwezig: Boolean = false,
    var statustypeAfgerondLaatsteVolgnummer: Boolean = false,
    var resultaattypeAanwezig: Boolean = false,
    var aantalInitiatorroltypen: Int = 0,
    var aantalBehandelaarroltypen: Int = 0,
    var rolOverigeAanwezig: Boolean = false,
    var informatieobjecttypeEmailAanwezig: Boolean = false,
    var besluittypeAanwezig: Boolean = false,
    var resultaattypesMetVerplichtBesluit: MutableList<String?>? = null,
    var zaakafhandelParametersValide: Boolean = false,
    var valide: Boolean = false
)
