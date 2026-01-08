/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.spi.CDI
import jakarta.inject.Inject
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.TaakVariabelenService
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.mail.MailService
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import org.flowable.engine.HistoryService

/**
 * A Helper for Flowable CMMN and BPMN LifecycleListener's, Interceptors etc. in order to get access to CDI resources.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class FlowableHelper @Inject constructor(
    val eventingService: EventingService,
    val identityService: IdentityService,
    val indexeerService: IndexingService,
    val mailService: MailService,
    val mailTemplateService: MailTemplateService,
    val suspensionZaakHelper: SuspensionZaakHelper,
    val taakVariabelenService: TaakVariabelenService,
    val zaakService: ZaakService,
    val zaakVariabelenService: ZaakVariabelenService,
    val zgwApiService: ZgwApiService,
    val zrcClientService: ZrcClientService,
    val ztcClientService: ZtcClientService,
    val flowableHistoryService: HistoryService
) {
    companion object FlowableHelperProvider {
        fun getInstance(): FlowableHelper = CDI.current().select(FlowableHelper::class.java).get()
    }
}
