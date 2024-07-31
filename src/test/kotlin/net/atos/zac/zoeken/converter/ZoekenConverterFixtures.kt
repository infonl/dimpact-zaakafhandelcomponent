package net.atos.zac.zoeken.converter

import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.identity.IdentityService

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
