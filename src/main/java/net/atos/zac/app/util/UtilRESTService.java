/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util;

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.text.StringEscapeUtils;

import net.atos.client.zgw.shared.cache.Caching;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.zac.zaaksturing.ZaakafhandelParameterService;

@Path("util")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_HTML)
public class UtilRESTService {

    private static final String ZTC = h(2, "ztcClientService");
    private static final String ZHPS = h(2, "zaakafhandelParameterService");

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private ZaakafhandelParameterService zaakafhandelParameterService;

    @GET
    public String index() {
        return body(h(1, "Util") +
                    links(Stream.of("cache", "cache/ztc", "cache/zhps")) +
                    links(Stream.of("cache/clear", "cache/ztc/clear", "cache/zhps/clear")));
    }

    private String links(final Stream<String> url) {
        return ul(url.map(method -> a("/rest/util/" + method, method)));
    }

    @GET
    @Path("cache")
    public String getCaches() {
        return body(Stream.of(getZtcClientCaches(), getZaakafhandelParameterServiceCaches()));
    }

    @GET
    @Path("cache/ztc")
    public String getZtcCaches() {
        return body(getZtcClientCaches());
    }

    @GET
    @Path("cache/zhps")
    public String getZhpsCaches() {
        return body(getZaakafhandelParameterServiceCaches());
    }

    private String getZtcClientCaches() {
        return getSeriviceCacheStatistics(ZTC, ztcClientService);
    }

    private String getZaakafhandelParameterServiceCaches() {
        return getSeriviceCacheStatistics(ZHPS, zaakafhandelParameterService);
    }

    private String getSeriviceCacheStatistics(String prefix, Caching caching) {
        var statistics = caching.cacheStatistics();
        return prefix + ul(
                statistics.keySet().stream()
                        .map(cacheName -> String.format("%s %s<p/>", b(cacheName), ul(statistics.get(cacheName))))
        );
    }

    @GET
    @Path("cache/clear")
    public String clearCaches() {
        return body(Stream.of(clearZtcClientCaches(), clearAllZhpsCaches()));
    }

    @GET
    @Path("cache/ztc/clear")
    public String clearAllZtcClientCaches() {
        return body(clearZtcClientCaches());
    }

    @GET
    @Path("cache/zhps/clear")
    public String clearAllZaakafhandelParameterServiceCaches() {
        return body(clearAllZhpsCaches());
    }

    private String clearZtcClientCaches() {
        return ZTC + ul(Stream.of(ztcClientService.clearZaaktypeCache(),
                ztcClientService.clearStatustypeCache(),
                ztcClientService.clearResultaattypeCache(),
                ztcClientService.clearInformatieobjecttypeCache(),
                ztcClientService.clearZaaktypeInformatieobjecttypeCache(),
                ztcClientService.clearBesluittypeCache(),
                ztcClientService.clearRoltypeCache(),
                ztcClientService.clearCacheTime()));
    }

    private String clearAllZhpsCaches() {
        return ZHPS + ul(Stream.of(zaakafhandelParameterService.clearManagedCache(),
                zaakafhandelParameterService.clearListCache()));
    }

    private static String body(final Stream<String> utils) {
        return body(utils.collect(joining()));
    }

    private static String body(final String utils) {
        return "<html></head><body>" + utils + "</body></html>";
    }

    private static String b(final String value) {
        return "<b>" + StringEscapeUtils.escapeHtml4(value) + "</b>";
    }

    private static String h(final int i, final String label) {
        return "<h" + i + ">" + label + "</h" + i + ">";
    }

    private static String ul(final Object content) {
        return "<ul>" + content + "</ul>";
    }

    private static String ul(final Stream<String> li) {
        return ul(li.collect(joining("</li><li>", "<li>", "</li>")));
    }

    private static String a(final String url, final String label) {
        return "<a href=\"" + url + "\">" + label + "</a>";
    }
}
