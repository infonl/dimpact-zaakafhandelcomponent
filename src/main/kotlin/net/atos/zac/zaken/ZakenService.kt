package net.atos.zac.zaken

import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.event.EventingService
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import java.util.*
import java.util.logging.Logger

class ZakenService @Inject constructor(
    private val zrcClientService: ZRCClientService,
    private val ztcClientService: ZTCClientService,
    private val indexeerService: IndexeerService,
    private var eventingService: EventingService
) {
    companion object {
        private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
        private val LOG = Logger.getLogger(ZakenService::class.java.name)
    }

    /**
     * Asynchronously assigns a list of zaken to a group and/or user and updates the search index on the fly.
     */
    @Suppress("LongParameterList")
    fun assignZakenAsync(
        screenEventType: ScreenEventType,
        jobUUID: UUID,
        zaakUUIDs: List<UUID>,
        explanation: String? = null,
        group: Group? = null,
        user: User? = null
    ) = defaultCoroutineScope.launch(CoroutineName("AssignZakenCoroutine")) {
        LOG.fine {
            "Started asynchronous job with UUID: $jobUUID to assign " +
                "${zaakUUIDs.size} zaken to group and/or user"
        }
        val zakenAssignedList = mutableListOf<UUID>()
        zaakUUIDs.forEach { zaakUUID ->
            withContext(Dispatchers.IO) {
                val zaak = zrcClientService.readZaak(zaakUUID)
                group?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolGroep(it, zaak),
                        explanation
                    )
                }
                user?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolMedewerker(it, zaak),
                        explanation
                    )
                }
                indexeerService.indexeerDirect(
                    zaakUUID.toString(),
                    ZoekObjectType.ZAAK
                )
                zakenAssignedList.add(zaakUUID)
            }
        }
        assignZakenAsyncResult(
            screenEventType,
            jobUUID = jobUUID,
            zakenAssignedList = zakenAssignedList
        )
    }

    private fun assignZakenAsyncResult(
        screenEventType: ScreenEventType,
        jobUUID: UUID,
        zakenAssignedList: List<UUID>
    ) {
        LOG.fine(
            "Asynchronous process with job UUID '$jobUUID' finished. " +
                "Succesfully assigned ${zakenAssignedList.size} zaken to group and/or user"
        )
        // send 'zaken_verdelen' screen event with job UUID so that it can be picked up by a client (e.g. a web browser)
        // that has a websocket subscription to this event
        eventingService.send(screenEventType.updated(jobUUID))
    }

    fun bepaalRolGroep(group: Group, zaak: Zaak) =
        RolOrganisatorischeEenheid(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelend groep van de zaak",
            OrganisatorischeEenheid().apply {
                identificatie = group.id
                naam = group.name
            }
        )

    fun bepaalRolMedewerker(user: User, zaak: Zaak) =
        RolMedewerker(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelaar van de zaak",
            Medewerker().apply {
                identificatie = user.id
                voorletters = user.firstName
                achternaam = user.lastName
            }
        )
}
