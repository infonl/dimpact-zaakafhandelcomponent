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
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.mail.MailService
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * A Helper for Flowable CMMN and BPMN LifecycleListener's, Interceptors etc. in order to get access to CDI resources.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class FlowableHelper @Inject constructor(
    val zaakVariabelenService: ZaakVariabelenService,
    val taakVariabelenService: TaakVariabelenService,
    val zgwApiService: ZGWApiService,
    val ztcClientService: ZtcClientService,
    val zrcClientService: ZrcClientService,
    val eventingService: EventingService,
    val indexeerService: IndexingService,
    val identityService: IdentityService,
    val mailService: MailService,
    val mailTemplateService: MailTemplateService,
    val suspensionZaakHelper: SuspensionZaakHelper
) {
    companion object FlowableHelperProvider {
        fun getInstance(): FlowableHelper = CDI.current().select(FlowableHelper::class.java).get()
    }
}
