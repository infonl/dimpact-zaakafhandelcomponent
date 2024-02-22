package net.atos.client.zgw.ztc.util;

import java.time.LocalDate;
import java.time.Period;

import net.atos.client.zgw.ztc.model.generated.ZaakType;

public class ZaakTypeUtil {
    private ZaakTypeUtil() {
    }

    public static boolean isServicenormBeschikbaar(ZaakType zaakType) {
        return zaakType.getServicenorm() != null &&
                !Period.parse(zaakType.getServicenorm()).normalized().isZero();
    }

    public static boolean isNuGeldig(ZaakType zaakType) {
        final LocalDate eindeGeldigheid = zaakType.getEindeGeldigheid();
        return zaakType.getBeginGeldigheid().isBefore(
                LocalDate.now().plusDays(1)
        ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()));
    }
}
