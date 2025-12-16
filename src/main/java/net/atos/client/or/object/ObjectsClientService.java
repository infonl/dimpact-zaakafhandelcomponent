/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.object;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import nl.info.client.or.objects.model.generated.ModelObject;

@ApplicationScoped
public class ObjectsClientService {
    private ObjectsClient objectsClient;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public ObjectsClientService() {
    }

    @Inject
    public ObjectsClientService(
            @RestClient ObjectsClient objectsClient
    ) {
        this.objectsClient = objectsClient;
    }

    /**
     * Read {@link ModelObject} via its UUID.
     * Throws a RuntimeException if the {@link ModelObject} can not be read.
     *
     * @param object UUID of {@link ModelObject}.
     * @return {@link ModelObject}. Never 'null'!
     */
    public ModelObject readObject(final UUID object) {
        return objectsClient.objectRead(object);
    }
}
