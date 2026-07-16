/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.webdav

import jakarta.enterprise.inject.spi.CDI
import jakarta.servlet.http.HttpSession
import nl.info.zac.util.time.DateTimeConverterUtil.convertToDate
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.webdav.ITransaction
import nl.info.webdav.IWebdavStore
import nl.info.webdav.StoredObject
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.authentication.setLoggedInUser
import nl.info.zac.util.toBase64String
import org.apache.commons.collections4.map.LRUMap
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.InputStream
import java.security.Principal
import java.util.Collections

/**
 * ZAC WebdavStore implementation.
 * The WebdavServlet instantiates this class via reflection, passing the configured rootpath File as the constructor argument.
 */
@Suppress("TooManyFunctions")
class WebdavStore(@Suppress("UNUSED_PARAMETER") ignoredFake: File) : IWebdavStore {
    companion object {
        private const val UPDATE_INHOUD_TOELICHTING = "Document bewerkt"
        private val folderStoredObject = StoredObject().apply { isFolder = true }

        // Caches token -> StoredObject to avoid re-fetching documents on each WebDAV property request
        private val fileStoredObjectMap: MutableMap<String, StoredObject> = Collections.synchronizedMap(LRUMap(100))
    }

    private val webdavHelper: WebdavHelper = CDI.current().select(WebdavHelper::class.java).get()
    private val drcClientService: DrcClientService = CDI.current().select(DrcClientService::class.java).get()
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService =
        CDI.current().select(EnkelvoudigInformatieObjectUpdateService::class.java).get()

    override fun begin(principal: Principal?) = null

    override fun checkAuthentication(transaction: ITransaction?) {
        // no-op
    }

    override fun commit(transaction: ITransaction?) {
        // no-op
    }

    override fun rollback(transaction: ITransaction?) {
        // no-op
    }

    override fun createFolder(transaction: ITransaction?, folderUri: String) {
        // no-op
    }

    override fun createResource(transaction: ITransaction?, resourceUri: String) {
        // no-op
    }

    override fun getResourceContent(transaction: ITransaction?, resourceUri: String): InputStream? {
        val token = extraheerToken(resourceUri).takeIf { it.isNotEmpty() } ?: return null
        return drcClientService.downloadEnkelvoudigInformatieobject(
            webdavHelper.readWebdavTokenData(token).enkelvoudigInformatieobjectUUID
        )
    }

    override fun setResourceContent(
        transaction: ITransaction?,
        resourceUri: String,
        content: InputStream,
        contentType: String?,
        characterEncoding: String?
    ): Long {
        val token = extraheerToken(resourceUri).takeIf { it.isNotEmpty() } ?: return 0L
        val webdavGegevens = webdavHelper.readWebdavTokenData(token)
        try {
            setLoggedInUser(
                CDI.current().select(HttpSession::class.java).get(),
                webdavGegevens.loggedInUser
            )
            val inhoud = content.readBytes()
            val update = EnkelvoudigInformatieObjectWithLockRequest().apply {
                this.inhoud = inhoud.toBase64String()
                bestandsomvang = inhoud.size
            }
            return enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                enkelvoudigInformatieObjectUUID = webdavGegevens.enkelvoudigInformatieobjectUUID,
                enkelvoudigInformatieObjectWithLockRequest = update,
                toelichting = UPDATE_INHOUD_TOELICHTING
            ).bestandsomvang?.toLong() ?: 0L
        } finally {
            fileStoredObjectMap.remove(token)
        }
    }

    override fun getChildrenNames(transaction: ITransaction?, folderUri: String) = null

    override fun getResourceLength(transaction: ITransaction?, resourceUri: String) = 0L

    override fun removeObject(transaction: ITransaction?, uri: String) {
        // no-op
    }

    override fun getStoredObject(transaction: ITransaction?, uri: String): StoredObject? {
        val token = extraheerToken(uri)
        return when {
            token.isEmpty() -> null
            token == WebdavHelper.FOLDER -> folderStoredObject
            else -> getFileStoredObject(token)
        }
    }

    override fun destroy() {
        // no-op
    }

    private fun extraheerToken(uri: String): String = FilenameUtils.getBaseName(File(uri).name)

    private fun getFileStoredObject(token: String): StoredObject =
        synchronized(fileStoredObjectMap) {
            fileStoredObjectMap[token] ?: run {
                val enkelvoudigInformatieobjectUUID = webdavHelper.readWebdavTokenData(token).enkelvoudigInformatieobjectUUID
                val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
                    enkelvoudigInformatieobjectUUID
                )
                StoredObject().apply {
                    isFolder = false
                    creationDate = convertToDate(enkelvoudigInformatieobject.creatiedatum)
                    lastModified = convertToDate(enkelvoudigInformatieobject.beginRegistratie.toZonedDateTime())
                    resourceLength = enkelvoudigInformatieobject.bestandsomvang?.toLong() ?: 0L
                }.also { storedObject ->
                    fileStoredObjectMap[token] = storedObject
                }
            }
        }
}
