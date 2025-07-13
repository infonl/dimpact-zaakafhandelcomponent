/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import nl.info.zac.app.admin.model.RestZaakafhandelParameters;

public class RESTMailtemplateKoppeling {

    public Long id;

    public RestZaakafhandelParameters zaakafhandelParameters;

    public RestMailtemplate mailtemplate;
}
