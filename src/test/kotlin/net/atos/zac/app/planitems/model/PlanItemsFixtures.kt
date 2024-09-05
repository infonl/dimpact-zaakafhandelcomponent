package net.atos.zac.app.planitems.model

import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.mail.model.RESTMailGegevens
import net.atos.zac.app.zaak.model.createRestGroup
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createRESTHumanTaskData(
    planItemInstanceId: String = "dummyPlanItemInstanceId",
    groep: RestGroup = createRestGroup(),
    medewerker: RestUser? = null,
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
