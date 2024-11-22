/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.webdav;

import static net.atos.zac.authentication.SecurityUtilKt.setLoggedInUser;
import static net.atos.zac.util.time.DateTimeConverterUtil.convertToDate;
import static nl.lifely.zac.util.Base64ConvertersKt.toBase64String;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest;
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService;
import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;


public class WebdavStore implements IWebdavStore {

    private static final StoredObject folderStoredObject;

    private static final String UPDATE_INHOUD_TOELICHTING = "Document bewerkt";

    static {
        folderStoredObject = new StoredObject();
        folderStoredObject.setFolder(true);
    }

    /*
     * Tijdens het ophalen van het document via WebDAV wordt er vaak een File StoredObject aangemaakt.
     * Om te voorkomen dat er vaak een Document moet worden opgehaald wordt er een map bijgeouden van token naar File StoredObject.
     */
    private static final Map<String, StoredObject> fileStoredObjectMap = Collections.synchronizedMap(new LRUMap<>(100));

    private final WebdavHelper webdavHelper;

    private final DrcClientService drcClientService;

    private final EnkelvoudigInformatieObjectUpdateService enkelvoudigInformatieObjectUpdateService;

    // De dummy parameter is nodig omdat de constructie waarmee deze class wordt geinstantieerd deze parameter verwacht
    public WebdavStore(final File ignoredDummy) {
        webdavHelper = CDI.current().select(WebdavHelper.class).get();
        drcClientService = CDI.current().select(DrcClientService.class).get();
        enkelvoudigInformatieObjectUpdateService = CDI.current().select(EnkelvoudigInformatieObjectUpdateService.class).get();
    }

    @Override
    public ITransaction begin(final Principal principal) {
        return null;
    }

    @Override
    public void checkAuthentication(final ITransaction transaction) {
    }

    @Override
    public void commit(final ITransaction transaction) {
    }

    @Override
    public void rollback(final ITransaction transaction) {
    }

    @Override
    public void createFolder(final ITransaction transaction, final String folderUri) {
    }

    @Override
    public void createResource(final ITransaction transaction, final String resourceUri) {
    }

    @Override
    public InputStream getResourceContent(final ITransaction transaction, final String resourceUri) {
        final String token = extraheerToken(resourceUri);
        if (StringUtils.isNotEmpty(token)) {
            final UUID enkelvoudigInformatieobjectUUID = webdavHelper.readGegevens(token)
                    .enkelvoudigInformatieibjectUUID();
            return drcClientService.downloadEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID);
        } else {
            return null;
        }
    }

    @Override
    public long setResourceContent(
            final ITransaction transaction,
            final String resourceUri,
            final InputStream content,
            final String contentType,
            final String characterEncoding
    ) {
        final String token = extraheerToken(resourceUri);
        if (StringUtils.isNotEmpty(token)) {
            final WebdavHelper.Gegevens webdavGegevens = webdavHelper.readGegevens(token);
            try {
                setLoggedInUser(
                        CDI.current().select(HttpSession.class).get(),
                        webdavGegevens.loggedInUser()
                );
                final var update = new EnkelvoudigInformatieObjectWithLockRequest();
                final byte[] inhoud = IOUtils.toByteArray(content);
                update.setInhoud(toBase64String(inhoud));
                update.setBestandsomvang(inhoud.length);
                return enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        webdavGegevens.enkelvoudigInformatieibjectUUID(),
                        update,
                        UPDATE_INHOUD_TOELICHTING
                )
                        .getBestandsomvang();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
                fileStoredObjectMap.remove(token);
            }
        } else {
            return 0;
        }
    }

    @Override
    public String[] getChildrenNames(final ITransaction transaction, final String folderUri) {
        return null;
    }

    @Override
    public long getResourceLength(final ITransaction transaction, final String resourceUri) {
        return 0;
    }

    @Override
    public void removeObject(final ITransaction transaction, final String uri) {
        // no-op
    }

    @Override
    public StoredObject getStoredObject(final ITransaction transaction, final String uri) {
        final String token = extraheerToken(uri);
        if (StringUtils.isEmpty(token)) {
            return null;
        } else if (StringUtils.equals(token, WebdavHelper.FOLDER)) {
            return folderStoredObject;
        } else {
            return getFileStoredObject(token);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    private String extraheerToken(final String uri) {
        if (uri != null) {
            final File url = new File(uri);
            return FilenameUtils.getBaseName(url.getName());
        } else {
            return null;
        }
    }

    private StoredObject getFileStoredObject(final String token) {
        return fileStoredObjectMap.computeIfAbsent(token, key -> {
            final UUID enkelvoudigInformatieobjectUUID = webdavHelper.readGegevens(token)
                    .enkelvoudigInformatieibjectUUID();
            final EnkelvoudigInformatieObject enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
                    enkelvoudigInformatieobjectUUID);
            final StoredObject storedObject = new StoredObject();
            storedObject.setFolder(false);
            storedObject.setCreationDate(convertToDate(enkelvoudigInformatieobject.getCreatiedatum()));
            storedObject.setLastModified(convertToDate(enkelvoudigInformatieobject.getBeginRegistratie().toZonedDateTime()));
            storedObject.setResourceLength(enkelvoudigInformatieobject.getBestandsomvang());
            return storedObject;
        });
    }
}
