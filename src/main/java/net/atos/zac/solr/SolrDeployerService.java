/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.solr;

import static net.atos.zac.search.IndexingService.SOLR_CORE;
import static net.atos.zac.solr.FieldType.STRING;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.NAME;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.addField;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.deleteField;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.SolrException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import net.atos.zac.search.IndexingService;
import net.atos.zac.search.model.zoekobject.ZoekObjectType;

@Singleton
public class SolrDeployerService {
    private static final Logger LOG = Logger.getLogger(SolrDeployerService.class.getName());
    private static final String VERSION_FIELD_PREFIX = "schema_version_";
    private static final int SOLR_STATUS_OK = 0;
    private static final int WAIT_FOR_SOLR_SECONDS = 1;

    private String solrUrl;
    private ManagedExecutorService managedExecutor;
    private IndexingService indexingService;
    private SolrClient solrClient;
    private List<SolrSchemaUpdate> schemaUpdates;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public SolrDeployerService() {
    }

    @Inject
    public SolrDeployerService(
            @ConfigProperty(name = "SOLR_URL") final String solrUrl,
            final IndexingService indexingService
    ) {
        this.solrUrl = solrUrl;
        this.indexingService = indexingService;
    }

    @Inject
    public void setSchemaUpdates(final Instance<SolrSchemaUpdate> schemaUpdates) {
        this.schemaUpdates = schemaUpdates.stream()
                .sorted(Comparator.comparingInt(SolrSchemaUpdate::getVersie))
                .toList();
    }

    @Resource
    public void setManagedExecutorService(ManagedExecutorService managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object event) {
        solrClient = new Http2SolrClient.Builder("%s/solr/%s".formatted(solrUrl, SOLR_CORE)).build();
        waitForSolrAvailability();
        try {
            final int currentVersion = getCurrentVersion();
            LOG.info("Current version of Solr core '%s': %d".formatted(SOLR_CORE, currentVersion));
            if (currentVersion == schemaUpdates.getLast().getVersie()) {
                LOG.info("Solr core '%s' is up to date. No Solr schema migration needed.".formatted(SOLR_CORE));
            } else {
                schemaUpdates.stream()
                        .skip(currentVersion)
                        .forEach(this::apply);

                schemaUpdates.stream()
                        .skip(currentVersion)
                        .flatMap(schemaUpdate -> schemaUpdate.getTeHerindexerenZoekObjectTypes().stream())
                        .collect(Collectors.toSet())
                        .forEach(this::startReindexing);
            }
        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForSolrAvailability() {
        while (true) {
            try {
                if (new SolrPing().setActionPing().process(solrClient).getStatus() == SOLR_STATUS_OK) {
                    return;
                }
            } catch (final SolrServerException | IOException | SolrException e) {
                LOG.info(() -> "Solr core is not available yet. Exception: %s".formatted(e.getMessage()));
            }
            LOG.info("Waiting for %d seconds for Solr core '%s' to become available...".formatted(WAIT_FOR_SOLR_SECONDS, SOLR_CORE));
            try {
                Thread.sleep(Duration.ofSeconds(WAIT_FOR_SOLR_SECONDS).toMillis());
            } catch (InterruptedException e) {
                LOG.log(
                        Level.WARNING,
                        "Thread was interrupted while waiting for Solr core to become available. Re-interrupting thread.",
                        e
                );
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getCurrentVersion() throws SolrServerException, IOException {
        return new SchemaRequest.Fields().process(solrClient).getFields().stream()
                .map(field -> field.get(NAME).toString())
                .filter(fieldName -> fieldName.startsWith(VERSION_FIELD_PREFIX))
                .findAny()
                .map(versionFieldName -> Integer.valueOf(StringUtils.substringAfter(versionFieldName, VERSION_FIELD_PREFIX)))
                .orElse(0);
    }

    private void apply(final SolrSchemaUpdate schemaUpdate) {
        LOG.info("Updating Solr core '%s' to version: %d".formatted(SOLR_CORE, schemaUpdate.getVersie()));
        try {
            final List<SchemaRequest.Update> schemaUpdates = new LinkedList<>();
            schemaUpdates.addAll(schemaUpdate.getSchemaUpdates());
            schemaUpdates.addAll(updateVersionField(schemaUpdate.getVersie()));
            new SchemaRequest.MultiUpdate(schemaUpdates).process(solrClient);
        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SchemaRequest.Update> updateVersionField(final int version) {
        final List<SchemaRequest.Update> schemaUpdates = new LinkedList<>();
        if (version > 1) {
            schemaUpdates.add(deleteField(VERSION_FIELD_PREFIX + (version - 1)));
        }
        schemaUpdates.add(addField(VERSION_FIELD_PREFIX + version, STRING, false, false));
        return schemaUpdates;
    }

    private void startReindexing(final ZoekObjectType type) {
        managedExecutor.submit(() -> indexingService.reindex(type));
    }
}
