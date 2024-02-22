/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.event;

/**
 * Generic code for beans that listen to AbstractEvents.
 */
public abstract class AbstractEventObserver<EVENT extends AbstractEvent> {
  public abstract void onFire(final EVENT event);
}
