/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.object;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.or.objects.model.generated.ModelObject;


@ApplicationScoped
public class ObjectsClientService {

    @Inject
    @RestClient
    private ObjectsClient objectsClient;

    /**
     * Create {@link net.atos.client.or.objects.model.generated.ModelObject}.
     *
     * @param object {@link ModelObject}.
     * @return Created {@link ModelObject}.
     */
    public ModelObject createObject(final ModelObject object) {
        return objectsClient.objectCreate(object);
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

    /**
     * Update {@link ModelObject}.
     * The given instance completely replaces the existing instance.
     *
     * @param object {@link ModelObject}.
     * @return Updated {@link ModelObject}.
     */
    public ModelObject replaceObject(final ModelObject object) {
        return objectsClient.objectUpdate(object.getUuid(), object);
    }
}
