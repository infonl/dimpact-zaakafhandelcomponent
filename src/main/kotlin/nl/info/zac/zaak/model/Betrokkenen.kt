/*
* SPDX-FileCopyrightText: 2024 Lifely
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.zaak.model

import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.util.EnumSet

object Betrokkenen {
    /**
     * Defines the set of available 'betrokkenen' as role type descriptions which can be added to a zaak.
     * These role type descriptions are defined in the ZGW ZTC API.
     * The 'initiator' and 'behandelaar' roles types are not seen as 'betrokkenen' but rather as very specific
     * role types with their own business logic.
     */
    val BETROKKENEN_ENUMSET: EnumSet<OmschrijvingGeneriekEnum> =
        EnumSet.allOf(OmschrijvingGeneriekEnum::class.java).apply {
            this.removeAll(
                listOf(
                    OmschrijvingGeneriekEnum.INITIATOR,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            )
        }
}
