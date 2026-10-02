/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty

data class RestZaaktypeInrichtingscheck(
    var zaaktype: RestZaaktypeOverzicht,

    @get:JsonbProperty("isStatustypeIntakeAanwezig")
    @set:JsonbProperty("isStatustypeIntakeAanwezig")
    var isStatustypeIntakeAanwezig: Boolean = false,

    @get:JsonbProperty("isStatustypeInBehandelingAanwezig")
    @set:JsonbProperty("isStatustypeInBehandelingAanwezig")
    var isStatustypeInBehandelingAanwezig: Boolean = false,

    @get:JsonbProperty("isStatustypeHeropendAanwezig")
    @set:JsonbProperty("isStatustypeHeropendAanwezig")
    var isStatustypeHeropendAanwezig: Boolean = false,

    @get:JsonbProperty("isStatustypeAanvullendeInformatieVereist")
    @set:JsonbProperty("isStatustypeAanvullendeInformatieVereist")
    var isStatustypeAanvullendeInformatieVereist: Boolean = false,

    @get:JsonbProperty("isStatustypeAfgerondAanwezig")
    @set:JsonbProperty("isStatustypeAfgerondAanwezig")
    var isStatustypeAfgerondAanwezig: Boolean = false,

    @get:JsonbProperty("isStatustypeAfgerondLaatsteVolgnummer")
    @set:JsonbProperty("isStatustypeAfgerondLaatsteVolgnummer")
    var isStatustypeAfgerondLaatsteVolgnummer: Boolean = false,

    @get:JsonbProperty("isResultaattypeAanwezig")
    @set:JsonbProperty("isResultaattypeAanwezig")
    var isResultaattypeAanwezig: Boolean = false,

    var aantalInitiatorroltypen: Int = 0,
    var aantalBehandelaarroltypen: Int = 0,

    @get:JsonbProperty("isRolOverigeAanwezig")
    @set:JsonbProperty("isRolOverigeAanwezig")
    var isRolOverigeAanwezig: Boolean = false,

    @get:JsonbProperty("isInformatieobjecttypeEmailAanwezig")
    @set:JsonbProperty("isInformatieobjecttypeEmailAanwezig")
    var isInformatieobjecttypeEmailAanwezig: Boolean = false,

    @get:JsonbProperty("isBesluittypeAanwezig")
    @set:JsonbProperty("isBesluittypeAanwezig")
    var isBesluittypeAanwezig: Boolean = false,

    var resultaattypesMetVerplichtBesluit: MutableList<String?>? = null,

    @get:JsonbProperty("isZaakafhandelParametersValide")
    @set:JsonbProperty("isZaakafhandelParametersValide")
    var isZaakafhandelParametersValide: Boolean = false,

    @get:JsonbProperty("isBrpInstellingenCorrect")
    @set:JsonbProperty("isBrpInstellingenCorrect")
    var isBrpInstellingenCorrect: Boolean = false,

    @get:JsonbProperty("isZaakspecifiekeAutorisatieEigenschapAanwezig")
    var isZaakspecifiekeAutorisatieEigenschapAanwezig: Boolean = false,

    @get:JsonbProperty("isZaakspecifiekeAutorisatieRoltypeAanwezig")
    var isZaakspecifiekeAutorisatieRoltypeAanwezig: Boolean = false,

    var hasWaarschuwingen: Boolean = false,

    @get:JsonbProperty("isValide")
    @set:JsonbProperty("isValide")
    var isValide: Boolean = false
)
