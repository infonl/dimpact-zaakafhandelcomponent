/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.webdav;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriInfo;

import org.apache.commons.collections4.map.LRUMap;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.zac.util.MediaTypes;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.zac.authentication.LoggedInUser;

@Singleton
public class WebdavHelper {
    public static final String FOLDER = "folder";
    private static final Set<String> WEBDAV_WORD = Set.of(
            MediaTypes.Application.MS_WORD.getMediaType(),
            MediaTypes.Application.MS_WORD_OPEN_XML.getMediaType()
    );
    private static final Set<String> WEBDAV_POWERPOINT = Set.of(
            MediaTypes.Application.MS_POWER_POINT.getMediaType(),
            MediaTypes.Application.MS_POWER_POINT_OPEN_XML.getMediaType()
    );
    private static final Set<String> WEBDAV_EXCEL = Set.of(
            MediaTypes.Application.MS_EXCEL.getMediaType(),
            MediaTypes.Application.MS_EXCEL_OPEN_XML.getMediaType()
    );

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    private final Map<String, Gegevens> tokenMap = Collections.synchronizedMap(new LRUMap<>(1000));

    public URI createRedirectURL(final UUID enkelvoudigInformatieobjectUUID, final UriInfo uriInfo) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
                enkelvoudigInformatieobjectUUID);
        final String scheme = format(
                "%s:%s",
                getWebDAVApp(enkelvoudigInformatieobject.getFormaat()),
                uriInfo.getBaseUri().getScheme()
        );
        final String token = format(
                "%s.%s",
                createToken(enkelvoudigInformatieobjectUUID),
                getExtension(enkelvoudigInformatieobject.getBestandsnaam())
        );
        return uriInfo.getBaseUriBuilder().scheme(scheme).replacePath("webdav/folder/{token}").build(token);
    }

    public Gegevens readGegevens(final String token) {
        if (tokenMap.containsKey(token)) {
            return tokenMap.get(token);
        } else {
            throw new RuntimeException("WebDAV token does not exist (anymore).");
        }
    }

    private String createToken(final UUID enkelvoudigInformatieobjectUUID) {
        final String token = UUID.randomUUID().toString();
        tokenMap.put(token, new Gegevens(enkelvoudigInformatieobjectUUID, loggedInUserInstance.get()));
        return token;
    }

    private String getWebDAVApp(final String formaat) {
        if (WEBDAV_WORD.contains(formaat)) {
            return "ms-word";
        }
        if (WEBDAV_EXCEL.contains(formaat)) {
            return "ms-excel";
        }
        if (WEBDAV_POWERPOINT.contains(formaat)) {
            return "ms-powerpoint";
        }
        return null;
    }

    public record Gegevens(UUID enkelvoudigInformatieibjectUUID, LoggedInUser loggedInUser) {
    }
}
