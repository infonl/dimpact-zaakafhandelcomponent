/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.util.UUID;

/**
 * Zaak UUID
 *
 * @param uuid Unieke resource identifier (UUID4)
 */
public record ZaakUuid(UUID uuid) {
}
