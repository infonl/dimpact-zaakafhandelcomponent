/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.event;

public record SignaleringEventId<ID>(ID resource, ID detail) {

    @Override
    public String toString() {
        return detail != null ? String.format("%s;%s", resource, detail) : resource.toString();
    }
}
