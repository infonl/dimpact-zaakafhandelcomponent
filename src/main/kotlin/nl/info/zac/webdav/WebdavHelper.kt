/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.webdav

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.UriInfo
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.DrcClientService
import nl.info.webdav.exceptions.WebdavException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.collections4.map.LRUMap
import org.apache.commons.io.FilenameUtils.getExtension
import java.net.URI
import java.util.Collections
import java.util.UUID

@Singleton
@NoArgConstructor
class WebdavHelper @Inject constructor(
    private val drcClientService: DrcClientService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        const val FOLDER = "folder"
        const val TOKEN_MAP_MAX_SIZE = 1000
        private val WEBDAV_WORD = setOf(
            MediaTypes.Application.MS_WORD.mediaType,
            MediaTypes.Application.MS_WORD_OPEN_XML.mediaType
        )
        private val WEBDAV_POWERPOINT = setOf(
            MediaTypes.Application.MS_POWER_POINT.mediaType,
            MediaTypes.Application.MS_POWER_POINT_OPEN_XML.mediaType
        )
        private val WEBDAV_EXCEL = setOf(
            MediaTypes.Application.MS_EXCEL.mediaType,
            MediaTypes.Application.MS_EXCEL_OPEN_XML.mediaType
        )
    }

    private val tokenMap: MutableMap<String, WebdavTokenData> = Collections.synchronizedMap(LRUMap(TOKEN_MAP_MAX_SIZE))

    fun createRedirectURL(enkelvoudigInformatieobjectUUID: UUID, uriInfo: UriInfo): URI {
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
            enkelvoudigInformatieobjectUUID
        )
        val scheme = "${getWebdavApp(enkelvoudigInformatieobject.formaat)}:${uriInfo.baseUri.scheme}"
        val token = "${createToken(
            enkelvoudigInformatieobjectUUID
        )}.${getExtension(enkelvoudigInformatieobject.bestandsnaam)}"
        return uriInfo.baseUriBuilder.scheme(scheme).replacePath("webdav/folder/{token}").build(token)
    }

    fun readWebdavTokenData(token: String): WebdavTokenData =
        tokenMap[token] ?: throw WebdavException("WebDAV token does not exist (anymore).")

    private fun createToken(enkelvoudigInformatieobjectUUID: UUID): String {
        val token = UUID.randomUUID().toString()
        tokenMap[token] = WebdavTokenData(
            enkelvoudigInformatieobjectUUID = enkelvoudigInformatieobjectUUID,
            loggedInUser = loggedInUserInstance.get()
        )
        return token
    }

    private fun getWebdavApp(formaat: String): String? = when {
        WEBDAV_WORD.contains(formaat) -> "ms-word"
        WEBDAV_EXCEL.contains(formaat) -> "ms-excel"
        WEBDAV_POWERPOINT.contains(formaat) -> "ms-powerpoint"
        else -> null
    }

    data class WebdavTokenData(val enkelvoudigInformatieobjectUUID: UUID, val loggedInUser: LoggedInUser)
}
