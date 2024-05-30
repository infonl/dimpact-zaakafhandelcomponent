/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zoeken;

import static net.atos.client.zgw.shared.ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS;
import static net.atos.client.zgw.shared.model.Results.NUM_ITEMS_PER_PAGE;
import static net.atos.zac.util.UriUtil.uuidFromURI;
import static net.atos.zac.zoeken.model.index.ZoekObjectType.DOCUMENT;
import static net.atos.zac.zoeken.model.index.ZoekObjectType.TAAK;
import static net.atos.zac.zoeken.model.index.ZoekObjectType.ZAAK;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.eclipse.microprofile.config.ConfigProvider;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakListParameters;
import net.atos.zac.app.taken.model.TaakSortering;
import net.atos.zac.flowable.FlowableTaskService;
import net.atos.zac.shared.model.SorteerRichting;
import net.atos.zac.zoeken.converter.AbstractZoekObjectConverter;
import net.atos.zac.zoeken.model.ZoekObject;
import net.atos.zac.zoeken.model.index.ZoekObjectType;

@Singleton
@Transactional
public class IndexeerService {
    public static final String SOLR_CORE = "zac";

    private static final Logger LOG = Logger.getLogger(IndexeerService.class.getName());
    private static final int SOLR_MAX_RESULT = 100;
    private static final int TAKEN_MAX_RESULTS = 50;

    private final Instance<AbstractZoekObjectConverter<? extends ZoekObject>> converterInstances;
    private final ZRCClientService zrcClientService;
    private final DRCClientService drcClientService;
    private final FlowableTaskService flowableTaskService;
    private final SolrClient solrClient;
    private final Set<ZoekObjectType> herindexerenBezig = new HashSet<>();

    @Inject
    IndexeerService(
            @Any Instance<AbstractZoekObjectConverter<? extends ZoekObject>> converterInstances,
            ZRCClientService zrcClientService,
            DRCClientService drcClientService,
            FlowableTaskService flowableTaskService
    ) {
        this.converterInstances = converterInstances;
        this.zrcClientService = zrcClientService;
        this.drcClientService = drcClientService;
        this.flowableTaskService = flowableTaskService;

        solrClient = createSolrClient(
                String.format(
                        "%s/solr/%s",
                        ConfigProvider.getConfig().getValue("solr.url", String.class),
                        SOLR_CORE
                )
        );
    }

    public static SolrClient createSolrClient(final String baseSolrUrl) {
        return new Http2SolrClient.Builder(baseSolrUrl).build();
    }

    /**
     * Adds objectId to the Solr index and optionally performs a (hard) Solr commit so
     * that the Solr index is updated immediately.
     * Beware that hard Solr commits are relavitely expensive operations.
     *
     * @param objectId      the object id to be indexed
     * @param objectType    the object type
     * @param performCommit whether to perform a hard Solr commit
     */
    public void indexeerDirect(final String objectId, final ZoekObjectType objectType, final boolean performCommit) {
        addToSolrIndex(Stream.of(getConverter(objectType).convert(objectId)), performCommit);
    }

    /**
     * Add a list of objectIds to the Solr index and optionally performs a (hard) Solr commit so
     * that the Solr index is updated immediately.
     * Beware that hard Solr commits are relavitely expensive operations.
     *
     * @param objectIds     the list of object ids to be indexed
     * @param objectType    the object type
     * @param performCommit whether to perform a hard Solr commit
     */
    public void indexeerDirect(final Stream<String> objectIds, final ZoekObjectType objectType, final boolean performCommit) {
        addToSolrIndex(objectIds.map(objectId -> getConverter(objectType).convert(objectId)), performCommit);
    }

    @Transactional(Transactional.TxType.NEVER)
    public void herindexeren(final ZoekObjectType objectType) {
        if (herindexerenBezig.contains(objectType)) {
            log(objectType, "Markeren voor herindexeren niet gestart, is nog bezig");
            return;
        }
        herindexerenBezig.add(objectType);
        try {
            log(objectType, "Markeren voor herindexeren gestart...");
            markSolrEntitiesForRemoval(objectType);
            switch (objectType) {
                case ZAAK -> markAllZakenForReindexing();
                case DOCUMENT -> markAllInformatieobjectenForReindexing();
                case TAAK -> markAllTakenForReindexing();
            }
            log(objectType, "Markeren voor herindexeren gestopt");
        } finally {
            herindexerenBezig.remove(objectType);
        }
    }

    public void addOrUpdateZaak(final UUID zaakUUID, boolean inclusiefTaken) {
        indexeerDirect(zaakUUID.toString(), ZAAK, false);
        if (inclusiefTaken) {
            flowableTaskService.listOpenTasksForZaak(zaakUUID).stream()
                    .map(TaskInfo::getId)
                    .forEach(this::addOrUpdateTaak);
        }
    }

    public void addOrUpdateInformatieobject(final UUID informatieobjectUUID) {
        indexeerDirect(informatieobjectUUID.toString(), DOCUMENT, false);
    }

    public void addOrUpdateInformatieobjectByZaakinformatieobject(final UUID zaakinformatieobjectUUID) {
        addOrUpdateInformatieobject(uuidFromURI(
                zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).getInformatieobject()));
    }

    public void addOrUpdateTaak(final String taskID) {
        indexeerDirect(taskID, TAAK, false);
    }

    public void removeZaak(final UUID zaakUUID) {
        removeFromSolrIndex(zaakUUID.toString());
    }

    public void removeInformatieobject(final UUID informatieobjectUUID) {
        removeFromSolrIndex(informatieobjectUUID.toString());
    }

    public void removeTaak(final String taskID) {
        removeFromSolrIndex(taskID.toString());
    }

    public void commit() {
        try {
            // this overload waits until the solr searcher is done, which is what we want
            solrClient.commit(null, true, true);
        } catch (SolrServerException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void log(final ZoekObjectType objectType, final String message) {
        LOG.info("[%s] %s".formatted(objectType.toString(), message));
    }

    private AbstractZoekObjectConverter<? extends ZoekObject> getConverter(ZoekObjectType objectType) {
        for (AbstractZoekObjectConverter<? extends ZoekObject> converter : converterInstances) {
            if (converter.supports(objectType)) {
                return converter;
            }
        }
        throw new RuntimeException("[%s] No converter found".formatted(objectType.toString()));
    }

    private long addToSolrIndex(final Stream<ZoekObject> zoekObjecten, final boolean performCommit) {
        final List<ZoekObject> beansToBeAdded = zoekObjecten
                .filter(Objects::nonNull)
                .toList();
        if (CollectionUtils.isNotEmpty(beansToBeAdded)) {
            try {
                solrClient.addBeans(beansToBeAdded);
                if (performCommit) {
                    commit();
                }
            } catch (final IOException | SolrServerException e) {
                throw new RuntimeException(e);
            }
        }
        return beansToBeAdded.size();
    }

    private void removeFromSolrIndex(final Stream<String> ids) {
        final List<String> idsToBeDeleted = ids.toList();
        if (CollectionUtils.isNotEmpty(idsToBeDeleted)) {
            try {
                solrClient.deleteById(idsToBeDeleted);
            } catch (final IOException | SolrServerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void removeFromSolrIndex(String id) {
        try {
            solrClient.deleteById(id);
        } catch (final IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private void logProgress(final ZoekObjectType objectType, final long voortgang, final long grootte) {
        log(objectType, "gemarkeerd: %d / %d".formatted(voortgang, grootte));
    }

    private void markSolrEntitiesForRemoval(final ZoekObjectType objectType) {
        final SolrQuery query = new SolrQuery("*:*");
        query.setFields("id");
        query.addFilterQuery("type:%s".formatted(objectType.toString()));
        query.addSort("id", SolrQuery.ORDER.asc);
        query.setRows(SOLR_MAX_RESULT);
        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            final QueryResponse response;
            try {
                response = solrClient.query(query);
            } catch (final SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
            removeFromSolrIndex(
                    response.getResults().stream()
                            .map(document -> document.get("id"))
                            .filter(Objects::nonNull)
                            .map(Object::toString));
            final String nextCursorMark = response.getNextCursorMark();
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            } else {
                cursorMark = nextCursorMark;
            }
        }
    }

    private void markAllZakenForReindexing() {
        final ZaakListParameters listParameters = new ZaakListParameters();
        listParameters.setOrdering("-identificatie");
        listParameters.setPage(FIRST_PAGE_NUMBER_ZGW_APIS);
        boolean hasMore;
        do {
            hasMore = markZakenForReindexing(listParameters);
            listParameters.setPage(listParameters.getPage() + 1);
        } while (hasMore);
    }

    private void markAllInformatieobjectenForReindexing() {
        final EnkelvoudigInformatieobjectListParameters listParameters = new EnkelvoudigInformatieobjectListParameters();
        listParameters.setPage(FIRST_PAGE_NUMBER_ZGW_APIS);
        boolean hasMore;
        do {
            hasMore = markInformatieobjectenForReindexing(listParameters);
            listParameters.setPage(listParameters.getPage() + 1);
        } while (hasMore);
    }

    private void markAllTakenForReindexing() {
        final long numberOfTasks = flowableTaskService.countOpenTasks();
        int page = 0;
        boolean hasMore;
        do {
            hasMore = markTakenForReindexing(page, numberOfTasks);
            page++;
        } while (hasMore);
    }

    private boolean markZakenForReindexing(final ZaakListParameters listParameters) {
        final Results<Zaak> results = zrcClientService.listZaken(listParameters);
        indexeerDirect(results.getResults().stream()
                .map(Zaak::getUuid)
                .map(UUID::toString), ZAAK, false);
        logProgress(ZAAK,
                (listParameters.getPage() - FIRST_PAGE_NUMBER_ZGW_APIS) * NUM_ITEMS_PER_PAGE + results.getResults()
                        .size(), results.getCount());
        return results.getNext() != null;
    }

    private boolean markInformatieobjectenForReindexing(
            final EnkelvoudigInformatieobjectListParameters listParameters
    ) {
        final Results<EnkelvoudigInformatieObject> results = drcClientService.listEnkelvoudigInformatieObjecten(listParameters);
        indexeerDirect(
                results.getResults().stream()
                        .map(enkelvoudigInformatieObject -> URIUtil.parseUUIDFromResourceURI(enkelvoudigInformatieObject.getUrl()))
                        .map(UUID::toString),
                DOCUMENT,
                false
        );
        logProgress(DOCUMENT,
                (listParameters.getPage() - FIRST_PAGE_NUMBER_ZGW_APIS) * NUM_ITEMS_PER_PAGE + results.getResults()
                        .size(), results.getCount());
        return results.getNext() != null;
    }

    private boolean markTakenForReindexing(final int page, final long numberOfTasks) {
        final int firstResult = page * TAKEN_MAX_RESULTS;
        final List<Task> tasks = flowableTaskService.listOpenTasks(TaakSortering.CREATIEDATUM, SorteerRichting.DESCENDING,
                firstResult, TAKEN_MAX_RESULTS);
        indexeerDirect(tasks.stream().map(TaskInfo::getId), TAAK, false);
        if (!tasks.isEmpty()) {
            logProgress(TAAK, (long) firstResult + tasks.size(), numberOfTasks);
            return tasks.size() == TAKEN_MAX_RESULTS;
        }
        return false;
    }
}
