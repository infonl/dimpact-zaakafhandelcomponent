/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty

data class RESTZaaktypeInrichtingscheck(
    var zaaktype: RestZaaktypeOverzicht,
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
    var brpInstellingenCorrect: Boolean = false,

    @get:JsonbProperty("isZaakspecifiekeAutorisatieEigenschapAanwezig")
    var isZaakspecifiekeAutorisatieEigenschapAanwezig: Boolean = false,

    @get:JsonbProperty("isZaakspecifiekeAutorisatieRoltypeAanwezig")
    var isZaakspecifiekeAutorisatieRoltypeAanwezig: Boolean = false,

    @get:JsonbProperty("heeftWaarschuwingen")
    var heeftWaarschuwingen: Boolean = false,

    var valide: Boolean = false
)
