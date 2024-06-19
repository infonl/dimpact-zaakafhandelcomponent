package net.atos.zac.app.planitems.model

import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.mail.model.RESTMailGegevens
import net.atos.zac.app.zaken.model.createRESTGroup
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createRESTHumanTaskData(
    planItemInstanceId: String = "dummyPlanItemInstanceId",
    groep: RESTGroup = createRESTGroup(),
    medewerker: RESTUser? = null,
    fataledatum: LocalDate? = null,
    toelichting: String? = null,
    taakdata: Map<String, String>? = null,
    taakStuurGegevens: RESTTaakStuurGegevens = createRESTTaakStuurGegevens()
) = RESTHumanTaskData().apply {
    this.planItemInstanceId = planItemInstanceId
    this.groep = groep
    this.medewerker = medewerker
    this.fataledatum = fataledatum
    this.toelichting = toelichting
    this.taakdata = taakdata
    this.taakStuurGegevens = taakStuurGegevens
}

fun createRESTTaakStuurGegevens(
    sendMail: Boolean = false,
    mail: String? = null
) = RESTTaakStuurGegevens().apply {
    this.sendMail = sendMail
    this.mail = mail
}

fun createRESTUserEventListenerData(
    zaakUuid: UUID,
    actie: UserEventListenerActie,
    restMailGegevens: RESTMailGegevens
) = RESTUserEventListenerData().apply {
    this.zaakUuid = zaakUuid
    this.actie = actie
    this.restMailGegevens = restMailGegevens
}
