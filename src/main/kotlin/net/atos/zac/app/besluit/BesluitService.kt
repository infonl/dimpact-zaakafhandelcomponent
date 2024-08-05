package net.atos.zac.app.besluit

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.zac.app.zaak.model.RestBesluitIntrekkenGegevens
import java.util.logging.Logger

class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(BrcClientService::class.java.name)
    }
    fun readBesluit(restBesluitIntrekkenGegevens: RestBesluitIntrekkenGegevens): Besluit =
        brcClientService.readBesluit(restBesluitIntrekkenGegevens.besluitUuid).apply {
            vervaldatum = restBesluitIntrekkenGegevens.vervaldatum
            vervalreden = VervalredenEnum.fromValue(restBesluitIntrekkenGegevens.vervalreden.lowercase())
        }

    fun withdrawBesluit(besluit: Besluit, reden: String): Besluit =
        brcClientService.updateBesluit(
            besluit,
            getBesluitWithdrawalExplanation(besluit.vervalreden)?.let { String.format(it, reden) }
        )

    private fun getBesluitWithdrawalExplanation(withdrawalReason: VervalredenEnum): String? {
        return when (withdrawalReason) {
            VervalredenEnum.INGETROKKEN_OVERHEID -> "Overheid: %s"
            VervalredenEnum.INGETROKKEN_BELANGHEBBENDE -> "Belanghebbende: %s"
            else -> {
                LOG.info("Unknown besluit withdrawal reason: '$withdrawalReason'. Returning 'null'.")
                null
            }
        }
    }
}
