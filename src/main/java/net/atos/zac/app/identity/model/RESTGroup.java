/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.identity.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RESTGroup {
    /**
     * Since this is used for the 'identificatie' field in
     * {@link net.atos.client.zgw.zrc.model.OrganisatorischeEenheid}
     * we need to make sure it adheres to the same constraints.
     */
    @NotNull @Size(max = 24)
    public String id;

    /**
     * Since this is used for the 'naam' field in
     * {@link net.atos.client.zgw.zrc.model.OrganisatorischeEenheid}
     * we need to make sure it adheres to the same constraints.
     */
    @NotNull @Size(max = 50)
    public String naam;
}
