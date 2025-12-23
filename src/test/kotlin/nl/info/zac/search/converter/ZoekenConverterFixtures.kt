/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search.converter

import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService

@Suppress("LongParameterList")
fun createZaakZoekObjectConverter(
    zrcClientService: ZrcClientService,
    ztcClientService: ZtcClientService,
    zgwApiService: ZgwApiService,
    identityService: IdentityService,
    flowableTaskService: FlowableTaskService
) =
    ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        zgwApiService,
        identityService,
        flowableTaskService
    )
