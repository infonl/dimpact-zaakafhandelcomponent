/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model;

public class Paging {

    private final int page;

    private final int maxResults;

    public Paging(final int page, final int count) {
        this.page = page;
        this.maxResults = count;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public int getFirstResult() {
        return page * maxResults;
    }
}
