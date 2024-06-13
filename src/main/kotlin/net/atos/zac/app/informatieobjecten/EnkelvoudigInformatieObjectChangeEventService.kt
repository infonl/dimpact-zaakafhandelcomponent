package net.atos.zac.app.informatieobjecten

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import kotlinx.coroutines.delay
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class EnkelvoudigInformatieObjectChangeEventService @Inject constructor(
    private val eventingService: EventingService,
    private val configuration: EnkelvoudigInformatieObjectChangeEventServiceConfiguration
) {
    companion object {
        private val LOG = Logger.getLogger(EnkelvoudigInformatieObjectChangeEventService::class.java.name)
    }

    suspend fun sendChangeEvent(informationObjectUUID: UUID, predicate: (UUID) -> Boolean) {
        repeat(configuration.retries) {
            if (predicate(informationObjectUUID)) {
                // No notification by OpenZaak for lock/unlock, so we send this on our own
                eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID))

                LOG.fine("Change event for for information object with UUID $informationObjectUUID sent")
                return
            }
            delay(configuration.waitTimeoutMillis)
        }
        LOG.warning(
            "Change event was not sent for information object with UUID $informationObjectUUID as desired " +
                "state was not reached after ${configuration.retries * configuration.waitTimeoutMillis } ms"
        )
    }
}

data class EnkelvoudigInformatieObjectChangeEventServiceConfiguration(
    val retries: Int = 5,
    val waitTimeoutMillis: Long = 200
)
