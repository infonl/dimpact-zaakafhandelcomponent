/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package nl.info.client.or.`object`

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ObjectsClientService @Inject constructor(
    @RestClient private val objectsClient: ObjectsClient
) {
    fun readObject(objectUUID: UUID): ModelObject = objectsClient.objectRead(objectUUID = objectUUID)
}
