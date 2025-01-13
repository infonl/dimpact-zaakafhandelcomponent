/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.objecttype;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.or.objecttypes.model.generated.ObjectType;
import net.atos.client.or.objecttypes.model.generated.ObjectVersion;

@ApplicationScoped
public class ObjecttypesClientService {

    @Inject
    @RestClient
    private ObjecttypesClient objecttypesClient;

    /**
     * List all instances of {@link ObjectType}.
     *
     * @return List of {@link ObjectType} instances.
     */
    public List<ObjectType> listObjecttypes() {
        return objecttypesClient.objecttypeList();
    }

    /**
     * List all instances of {@link ObjectVersion} for a specific {@link ObjectVersion}.
     *
     * @param objecttypeUUID UUID of the {@link ObjectVersion}.
     * @return List of {@link ObjectVersion} instances.
     */
    public List<ObjectVersion> listObjecttypeVersions(final UUID objecttypeUUID) {
        return objecttypesClient.objectversionList(objecttypeUUID);
    }

    /**
     * Read an {@link ObjectType}
     *
     * @param objecttypeUUID UUID of the {@link ObjectType}
     * @return {@link ObjectType} throws an exception if not found,
     */
    public ObjectType readObjecttype(final UUID objecttypeUUID) {
        return objecttypesClient.objecttypeRead(objecttypeUUID);
    }
}
