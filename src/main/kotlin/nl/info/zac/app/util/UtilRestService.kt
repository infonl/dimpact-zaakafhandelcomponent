/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.shared.cache.Caching
import net.atos.zac.admin.ZaakafhandelParameterService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.jacobras.humanreadable.HumanReadable.fileSize
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import java.lang.Runtime.getRuntime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Path("admin/util")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_HTML)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class UtilRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val policyService: PolicyService
) {
    companion object {
        private val ZTC: String = h(2, "ztcClientService")
        private val ZHPS: String = h(2, "zaakafhandelParameterService")

        private fun links(url: List<String>) = ul(url.map { a("/rest/admin/util/$it", it) })

        private fun body(utils: List<String>) = body(utils.joinToString())

        private fun body(utils: String) = "<html></head><body>$utils</body></html>"

        private fun b(value: String) = "<b>" + escapeHtml4(value) + "</b>"

        private fun h(i: Int, label: String) = "<h$i>$label</h$i>"

        private fun ul(content: String) = "<ul>$content</ul>"

        private fun ul(li: List<String>) = ul(li.joinToString("</li><li>", "<li>", "</li>"))

        private fun a(url: String, label: String) = "<a href=\"$url\">$label</a>"
    }

    @GET
    fun index(): String {
        checkBeherenPolicy()
        return body(
            h(1, "Util") +
                h(2, "Caches") +
                links(listOf("cache", "cache/ztc", "cache/zhps")) +
                links(listOf("cache/clear", "cache/ztc/clear", "cache/zhps/clear")) +
                h(2, "System") +
                links(listOf("memory"))
        )
    }

    @GET
    @Path("cache")
    fun caches(): String {
        checkBeherenPolicy()
        return body(
            listOf(
                ztcClientCaches(),
                zaakafhandelParameterServiceCaches()
            )
        )
    }

    @GET
    @Path("cache/ztc")
    fun ztcCaches(): String {
        checkBeherenPolicy()
        return body(ztcClientCaches())
    }

    @GET
    @Path("cache/zhps")
    fun zhpsCaches(): String {
        checkBeherenPolicy()
        return body(zaakafhandelParameterServiceCaches())
    }

    @GET
    @Path("cache/clear")
    fun clearCaches(): String {
        checkBeherenPolicy()
        return body(listOf(clearZtcClientCaches(), clearAllZhpsCaches()))
    }

    @GET
    @Path("cache/ztc/clear")
    fun clearAllZtcClientCaches(): String {
        checkBeherenPolicy()
        return body(clearZtcClientCaches())
    }

    @GET
    @Path("cache/zhps/clear")
    fun clearAllZaakafhandelParameterServiceCaches(): String {
        checkBeherenPolicy()
        return body(clearAllZhpsCaches())
    }

    @GET
    @Path("memory")
    fun memory(): String {
        checkBeherenPolicy()
        val runtime = getRuntime()
        val freeMemory = runtime.freeMemory()
        val totalMemory = runtime.totalMemory()
        val maxMemory = runtime.maxMemory()
        return body(
            h(1, "Memory") +
                ul(
                    listOf(
                        "free: ${fileSize(freeMemory, decimals = 2)} ($freeMemory bytes)",
                        "used : ${fileSize(totalMemory - freeMemory, decimals = 2)} (${totalMemory - freeMemory} bytes)",
                        "total: ${fileSize(totalMemory, decimals = 2)} ($totalMemory bytes)",
                        "max  : ${fileSize(maxMemory, decimals = 2)} ($maxMemory bytes)"
                    )
                )
        )
    }

    private fun checkBeherenPolicy() = assertPolicy(policyService.readOverigeRechten().beheren)

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

    private fun ztcClientCaches() = getSeriviceCacheDetails(ZTC, ztcClientService)

    private fun zaakafhandelParameterServiceCaches() = getSeriviceCacheDetails(ZHPS, zaakafhandelParameterService)

    private fun getSeriviceCacheDetails(prefix: String, caching: Caching): String {
        val cacheStatistics = caching.cacheStatistics()
        val estimatedCacheSizes = caching.estimatedCacheSizes()
        val totalLoadTimeRegExp = Regex("totalLoadTime=\\d+")
        return prefix + ul(
            cacheStatistics.entries.map { (key, value) ->
                // replace totalLoadTime substring with human-readable value
                val totalLoadTimeHumanReadable = value
                    .totalLoadTime()
                    .toDuration(DurationUnit.NANOSECONDS)
                    .toString()
                """
                    <p>
                    ${b(key)}
                    ${ul(value.toString().replace(totalLoadTimeRegExp, "totalLoadTime=$totalLoadTimeHumanReadable"))}
                    ${ul("Estimated cache size: ${estimatedCacheSizes[key]}")}
                    </p>
                """.trimIndent()
            }
        )
    }
}
