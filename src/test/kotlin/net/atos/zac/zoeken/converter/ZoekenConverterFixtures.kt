package net.atos.zac.zoeken.converter

import net.atos.client.vrl.VRLClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.flowable.TakenService
import net.atos.zac.identity.IdentityService

@Suppress("LongParameterList")
fun createZaakZoekObjectConverter(
    zrcClientService: ZRCClientService,
    ztcClientService: ZTCClientService,
    vrlClientService: VRLClientService,
    zgwApiService: ZGWApiService,
    identityService: IdentityService,
    takenService: TakenService
) =
    ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        vrlClientService,
        zgwApiService,
        identityService,
        takenService
    )
