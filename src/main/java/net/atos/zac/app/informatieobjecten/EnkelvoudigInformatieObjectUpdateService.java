package net.atos.zac.app.informatieobjecten;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObjectWithLockData;
import net.atos.client.zgw.drc.model.Ondertekening;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;

@ApplicationScoped
@Transactional
public class EnkelvoudigInformatieObjectUpdateService {

    private static final String VERZEND_TOELICHTING_PREFIX = "Per post";

    private static final String ONDERTEKENEN_TOELICHTING = "Door ondertekenen";

    @Inject
    private EnkelvoudigInformatieObjectLockService enkelvoudigInformatieObjectLockService;

    @Inject
    private DRCClientService drcClientService;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    public void verzendEnkelvoudigInformatieObject(final UUID uuid, final LocalDate verzenddatum, final String toelichting) {
        final var update = new EnkelvoudigInformatieObjectWithLockData();
        update.setVerzenddatum(verzenddatum);
        updateEnkelvoudigInformatieObjectWithLockData(uuid, update, isNotEmpty(toelichting) ? "%s: %s".formatted(
                VERZEND_TOELICHTING_PREFIX, toelichting) :
                VERZEND_TOELICHTING_PREFIX);
    }

    public void ondertekenEnkelvoudigInformatieObject(final UUID uuid) {
        final var update = new EnkelvoudigInformatieObjectWithLockData();
        final Ondertekening ondertekening = new Ondertekening();
        ondertekening.setSoort(Ondertekening.SoortEnum.DIGITAAL);
        ondertekening.setDatum(LocalDate.now());
        update.setOndertekening(ondertekening);
        update.setStatus(EnkelvoudigInformatieObjectWithLockData.StatusEnum.DEFINITIEF);
        updateEnkelvoudigInformatieObjectWithLockData(uuid, update, ONDERTEKENEN_TOELICHTING);
    }

    public EnkelvoudigInformatieObjectWithLockData updateEnkelvoudigInformatieObjectWithLockData(
            final UUID uuid,
            final EnkelvoudigInformatieObjectWithLockData update,
            final String toelichting
    ) {
        EnkelvoudigInformatieObjectLock tempLock = null;
        try {
            final var existingLock = enkelvoudigInformatieObjectLockService.findLock(uuid);
            if (existingLock.isPresent()) {
                update.setLock(existingLock.get().getLock());
            } else {
                tempLock = enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUserInstance.get().getId());
                update.setLock(tempLock.getLock());
            }
            return drcClientService.updateEnkelvoudigInformatieobject(uuid, update, toelichting);
        } finally {
            if (tempLock != null) {
                enkelvoudigInformatieObjectLockService.deleteLock(uuid);
            }
        }
    }
}
