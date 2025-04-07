/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search.converter

import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService

@Suppress("LongParameterList")
fun createZaakZoekObjectConverter(
    zrcClientService: ZrcClientService,
    ztcClientService: ZtcClientService,
    zgwApiService: ZGWApiService,
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
