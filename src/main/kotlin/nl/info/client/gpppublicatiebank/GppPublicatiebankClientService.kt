/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.gpppublicatiebank

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.gpppublicatiebank.Document
import net.atos.client.gpppublicatiebank.DocumentCreate
import net.atos.client.gpppublicatiebank.PublicationRead
import net.atos.client.gpppublicatiebank.PublicationWrite
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@NoArgConstructor
@AllOpen
class GppPublicatiebankClientService @Inject constructor(
    @RestClient
    private val gppPublicatiebankClient: GppPublicatiebankClient
) {
    fun createPublicatie(request: PublicationWrite): PublicationRead =
        gppPublicatiebankClient.createPublicatie(request)

    fun createDocument(request: DocumentCreate): Document =
        gppPublicatiebankClient.createDocument(request)
}
