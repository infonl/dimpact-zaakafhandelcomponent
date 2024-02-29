/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.websocket.Session;
import net.atos.zac.event.AbstractEventObserver;
import net.atos.zac.websocket.SessionRegistry;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This bean listens for {@link ScreenEvent}, converts them to a Websockets event and then forwards it to the browsers that have subscribed
 * to it.
 */
@Named
@ApplicationScoped
public class ScreenEventObserver extends AbstractEventObserver<ScreenEvent> {
    private static final Logger LOG = Logger.getLogger(ScreenEventObserver.class.getName());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private final SessionRegistry sessionRegistry;

    @Inject
    public ScreenEventObserver(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void onFire(final @ObservesAsync ScreenEvent event) {
        try {
            LOG.fine(() -> String.format("Scherm event ontvangen: %s", event.toString()));
            event.delay();
            sendToWebsocketSubscribers(event);
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "asynchronous guard", ex);
        }
    }

    private void sendToWebsocketSubscribers(final ScreenEvent event) {
        try {
            final Set<Session> subscribers = sessionRegistry.listSessions(event);
            if (!subscribers.isEmpty()) {
                final String json = JSON_MAPPER.writeValueAsString(event);
                subscribers.forEach(session -> session.getAsyncRemote().sendText(json));
            }
        } catch (final JsonProcessingException e) {
            LOG.log(Level.SEVERE, "Failed to convert the ScreenUpdateEvent to JSON.", e);
        }
    }
}
