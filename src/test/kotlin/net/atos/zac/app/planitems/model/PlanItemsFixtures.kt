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
) = RESTHumanTaskData(
    planItemInstanceId = planItemInstanceId,
    groep = groep,
    medewerker = medewerker,
    fataledatum = fataledatum,
    toelichting = toelichting,
    taakdata = taakdata,
    taakStuurGegevens = taakStuurGegevens
)

fun createRESTTaakStuurGegevens(
    sendMail: Boolean = false,
    mail: String? = null
) = RESTTaakStuurGegevens(
    sendMail = sendMail,
    mail = mail
)

fun createRESTUserEventListenerData(
    zaakUuid: UUID,
    actie: UserEventListenerActie,
    restMailGegevens: RESTMailGegevens
) = RESTUserEventListenerData(
    zaakUuid = zaakUuid,
    actie = actie,
    restMailGegevens = restMailGegevens
)
