/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * There is no SignaleringEventType. [SignaleringType] is used for that instead (i.e. the Type enum in it).
 *
 * This file provides the mapping- and factory-functions that would have been in SignaleringEventType.
 */
package nl.info.zac.signalering.event

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.identity.model.User
import nl.info.zac.notification.Action
import nl.info.zac.notification.Channel
import nl.info.zac.notification.Notification
import nl.info.zac.notification.Resource
import nl.info.zac.signalering.model.SignaleringType
import org.flowable.task.api.TaskInfo
import java.net.URI

fun signaleringEvent(
    signaleringType: SignaleringType.Type,
    zaak: Zaak,
    actor: User?
) = SignaleringEvent(signaleringType, SignaleringEventId(zaak.url, null), actor)

fun signaleringEvent(
    signaleringType: SignaleringType.Type,
    enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
    actor: User?
) = SignaleringEvent(signaleringType, SignaleringEventId(enkelvoudigInformatieobject.url, null), actor)

fun signaleringEvent(
    signaleringType: SignaleringType.Type,
    taak: TaskInfo,
    actor: User?
) = SignaleringEvent(signaleringType, SignaleringEventId(taak.id, null), actor)

// There is no actor information in notifications
private fun signaleringEvent(
    signaleringType: SignaleringType.Type,
    resource: Notification.ResourceInfo,
    detail: Notification.ResourceInfo?
) = SignaleringEvent(signaleringType, SignaleringEventId(resource.url, detail?.url), null)

/**
 * This is the mapping.
 *
 * @param channel the channel the notification came in on
 * @param mainResource the involved main resource (maybe equal to the resource)
 * @param resource the actually modified resource
 * @return the set of events that the parameters map to
 */
fun getSignaleringEvents(
    channel: Channel,
    mainResource: Notification.ResourceInfo,
    resource: Notification.ResourceInfo
): Set<SignaleringEvent<URI>> {
    val events = mutableSetOf<SignaleringEvent<URI>>()
    if (channel == Channel.ZAKEN && resource.action == Action.CREATE) {
        when (resource.type) {
            Resource.ZAAKINFORMATIEOBJECT ->
                events.add(signaleringEvent(SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD, mainResource, resource))
            Resource.ROL -> events.add(signaleringEvent(SignaleringType.Type.ZAAK_OP_NAAM, resource, null))
            else -> Unit
        }
    }
    return events
}
