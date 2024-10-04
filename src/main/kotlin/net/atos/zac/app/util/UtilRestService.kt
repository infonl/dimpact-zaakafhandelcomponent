/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util

import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.shared.cache.Caching
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.ZaakafhandelParameterService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.text.StringEscapeUtils

@Path("util")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_HTML)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class UtilRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService
) {

    @GET
    fun index() =
        body(
            h(1, "Util") +
                h(2, "Caches") +
                links(listOf("cache", "cache/ztc", "cache/zhps")) +
                links(listOf("cache/clear", "cache/ztc/clear", "cache/zhps/clear")) +
                h(2, "System") +
                links(listOf("memory"))
        )

    @GET
    @Path("cache")
    fun caches(): String = body(
        listOf(
            ztcClientCaches(),
            zaakafhandelParameterServiceCaches()
        )
    )

    @GET
    @Path("cache/ztc")
    fun ztcCaches(): String =
        body(ztcClientCaches())

    @GET
    @Path("cache/zhps")
    fun zhpsCaches(): String =
        body(zaakafhandelParameterServiceCaches())

    private fun ztcClientCaches() =
        getSeriviceCacheDetails(ZTC, ztcClientService)

    private fun zaakafhandelParameterServiceCaches() =
        getSeriviceCacheDetails(ZHPS, zaakafhandelParameterService)

    private fun getSeriviceCacheDetails(prefix: String, caching: Caching) =
        caching.cacheStatistics().let { statistics ->
            caching.cacheSizes().let { sizes ->
                prefix + ul(
                    statistics.keys.map {
                        "${b(it)} ${ul(statistics.get(it))} ${ul(sizes.get(it).toString() + " objects")}<p/>"
                    }
                )
            }
        }

    @GET
    @Path("cache/clear")
    fun clearCaches(): String =
        body(listOf(clearZtcClientCaches(), clearAllZhpsCaches()))

    @GET
    @Path("cache/ztc/clear")
    fun clearAllZtcClientCaches() =
        body(clearZtcClientCaches())

    @GET
    @Path("cache/zhps/clear")
    fun clearAllZaakafhandelParameterServiceCaches() =
        body(clearAllZhpsCaches())

    private fun clearZtcClientCaches() =
        ZTC + ul(
            listOf(
                ztcClientService.clearZaaktypeCache(),
                ztcClientService.clearStatustypeCache(),
                ztcClientService.clearResultaattypeCache(),
                ztcClientService.clearInformatieobjecttypeCache(),
                ztcClientService.clearZaaktypeInformatieobjecttypeCache(),
                ztcClientService.clearBesluittypeCache(),
                ztcClientService.clearRoltypeCache(),
                ztcClientService.clearCacheTime()
            )
        )

    private fun clearAllZhpsCaches() =
        ZHPS + ul(
            listOf(
                zaakafhandelParameterService.clearManagedCache(),
                zaakafhandelParameterService.clearListCache()
            )
        )

    @GET
    @Path("memory")
    fun memory() =
        Runtime.getRuntime().freeMemory().let { freeMemory ->
            Runtime.getRuntime().totalMemory().let { totalMemory ->
                body(
                    h(1, "Memory") +
                        ul(
                            listOf(
                                "free: $freeMemory bytes",
                                "used : ${totalMemory - freeMemory} bytes",
                                "total: $totalMemory bytes",
                                "max  : ${Runtime.getRuntime().maxMemory()} bytes"
                            )
                        )
                )
            }
        }

    companion object {
        private val ZTC: String = h(2, "ztcClientService")
        private val ZHPS: String = h(2, "zaakafhandelParameterService")

        private fun links(url: List<String>) = ul(url.map { a("/rest/util/$it", it) })

        private fun body(utils: List<String>) = body(utils.joinToString())

        private fun body(utils: String) = "<html></head><body>$utils</body></html>"

        private fun b(value: String) = "<b>" + StringEscapeUtils.escapeHtml4(value) + "</b>"

        private fun h(i: Int, label: String) = "<h$i>$label</h$i>"

        private fun ul(content: Any?) = "<ul>$content</ul>"

        private fun ul(li: List<String>) = ul(li.joinToString("</li><li>", "<li>", "</li>"))

        private fun a(url: String, label: String) = "<a href=\"$url\">$label</a>"
    }
}
