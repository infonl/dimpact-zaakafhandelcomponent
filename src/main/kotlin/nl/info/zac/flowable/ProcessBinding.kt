/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ProcessEngine
import java.net.URI
import java.util.UUID

/**
 * The operations on the process of a zaak in one process engine. Each engine has one implementation, which
 * [ZaakProcessService] selects by the engine of the zaaktype configuration.
 */
interface ProcessBinding {
    val processEngine: ProcessEngine

    fun start(zaak: Zaak, zaaktype: ZaakType, definitionKey: String, processStartData: ProcessStartData)

    /**
     * Whether the zaaktype in Open Zaak is set up the way this engine needs to create its zaken.
     */
    fun isZaaktypeReady(zaaktypeUri: URI): Boolean

    fun terminate(zaakUuid: UUID)

    /**
     * Deletes the process of the zaak and its history. Does nothing when the zaak has no process in this engine.
     */
    fun delete(zaakUuid: UUID)
}
