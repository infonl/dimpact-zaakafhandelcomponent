/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.objecttype;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.or.objecttype.model.Objecttype;
import net.atos.client.or.objecttype.model.ObjecttypeVersion;

@ApplicationScoped
public class ObjecttypesClientService {

    @Inject @RestClient private ObjecttypesClient objecttypesClient;

    /**
     * List all instances of {@link Objecttype}.
     *
     * @return List of {@link Objecttype} instances.
     */
    public List<Objecttype> listObjecttypes() {
        return objecttypesClient.objecttypeList();
    }

    /**
     * List all instances of {@link ObjecttypeVersion} for a specific {@link Objecttype}.
     *
     * @param objecttypeUUID UUID of the {@link Objecttype}.
     * @return List of {@link ObjecttypeVersion} instances.
     */
    public List<ObjecttypeVersion> listObjecttypeVersions(final UUID objecttypeUUID) {
        return objecttypesClient.objectversionList(objecttypeUUID);
    }

    /**
     * Read an {@link Objecttype}
     *
     * @param objecttypeUUID UUID of the {@link Objecttype}
     * @return {@link Objecttype} throws an exception if not found,
     */
    public Objecttype readObjecttype(final UUID objecttypeUUID) {
        return objecttypesClient.objecttypeRead(objecttypeUUID);
    }
}
