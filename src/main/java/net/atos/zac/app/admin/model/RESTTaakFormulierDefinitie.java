package net.atos.zac.app.admin.model;

import java.util.List;

public record RESTTaakFormulierDefinitie(
                                         String id,
                                         List<RESTTaakFormulierVeldDefinitie> veldDefinities
) {
}
