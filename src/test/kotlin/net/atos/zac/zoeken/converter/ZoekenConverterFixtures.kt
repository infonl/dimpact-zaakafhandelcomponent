package net.atos.zac.zoeken.converter

import net.atos.client.vrl.VrlClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.identity.IdentityService

@Suppress("LongParameterList")
fun createZaakZoekObjectConverter(
    zrcClientService: ZRCClientService,
    ztcClientService: ZtcClientService,
    vrlClientService: VrlClientService,
    zgwApiService: ZGWApiService,
    identityService: IdentityService,
    flowableTaskService: FlowableTaskService
) =
    ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        vrlClientService,
        zgwApiService,
        identityService,
        flowableTaskService
    )
