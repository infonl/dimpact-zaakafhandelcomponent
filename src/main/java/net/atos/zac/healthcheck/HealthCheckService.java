/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.healthcheck;

import static java.nio.file.Files.readAllLines;
import static net.atos.client.zgw.ztc.util.InformatieObjectTypeUtil.isNuGeldig;
import static net.atos.zac.util.DateTimeConverterUtil.convertToLocalDateTime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import net.atos.client.vrl.VRLClientService;
import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.Afleidingswijze;
import net.atos.client.zgw.ztc.model.generated.BesluitType;
import net.atos.client.zgw.ztc.model.generated.BrondatumArchiefprocedure;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.client.zgw.ztc.model.generated.ResultaatType;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.healthcheck.model.BuildInformatie;
import net.atos.zac.healthcheck.model.ZaaktypeInrichtingscheck;
import net.atos.zac.util.LocalDateUtil;
import net.atos.zac.zaaksturing.ZaakafhandelParameterService;
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

@Singleton
public class HealthCheckService {

    private static final String BUILD_TIMESTAMP_FILE = "/build_timestamp.txt";

    @Inject private ZTCClientService ztcClientService;

    @Inject private VRLClientService vrlClientService;

    @Inject private ZaakafhandelParameterService zaakafhandelParameterBeheerService;

    @Inject
    @ConfigProperty(name = "BRANCH_NAME")
    private Optional<String> branchName;

    @Inject
    @ConfigProperty(name = "COMMIT_HASH")
    private Optional<String> commitHash;

    @Inject
    @ConfigProperty(name = "VERSION_NUMBER")
    private Optional<String> versionNumber;

    private BuildInformatie buildInformatie;

    public boolean bestaatCommunicatiekanaalEformulier() {
        return vrlClientService
                .findCommunicatiekanaal(ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER)
                .isPresent();
    }

    public ZaaktypeInrichtingscheck controleerZaaktype(final URI zaaktypeUrl) {
        ztcClientService.readCacheTime();
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaaktypeUrl);
        final ZaakafhandelParameters zaakafhandelParameters =
                zaakafhandelParameterBeheerService.readZaakafhandelParameters(
                        URIUtil.parseUUIDFromResourceURI(zaaktype.getUrl()));
        final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck =
                new ZaaktypeInrichtingscheck(zaaktype);
        zaaktypeInrichtingscheck.setZaakafhandelParametersValide(zaakafhandelParameters.isValide());
        controleerZaaktypeStatustypeInrichting(zaaktypeInrichtingscheck);
        controleerZaaktypeResultaattypeInrichting(zaaktypeInrichtingscheck);
        controleerZaaktypeBesluittypeInrichting(zaaktypeInrichtingscheck);
        controleerZaaktypeRoltypeInrichting(zaaktypeInrichtingscheck);
        controleerZaaktypeInformatieobjecttypeInrichting(zaaktypeInrichtingscheck);
        return zaaktypeInrichtingscheck;
    }

    public BuildInformatie readBuildInformatie() {
        if (buildInformatie == null) {
            buildInformatie = createBuildInformatie();
        }
        return buildInformatie;
    }

    private BuildInformatie createBuildInformatie() {
        final LocalDateTime buildDatumTijd;
        final File buildDatumTijdFile = new File(BUILD_TIMESTAMP_FILE);
        if (buildDatumTijdFile.exists()) {
            try {
                buildDatumTijd =
                        convertToLocalDateTime(
                                ZonedDateTime.parse(
                                        readAllLines(buildDatumTijdFile.toPath()).get(0)));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            buildDatumTijd = null;
        }
        return new BuildInformatie(
                commitHash.orElse(null),
                branchName.orElse(null),
                buildDatumTijd,
                versionNumber.orElse(null));
    }

    private void controleerZaaktypeStatustypeInrichting(
            final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck) {
        final List<StatusType> statustypes =
                ztcClientService.readStatustypen(zaaktypeInrichtingscheck.getZaaktype().getUrl());
        int afgerondVolgnummer = 0;
        int hoogsteVolgnummer = 0;
        for (StatusType statustype : statustypes) {
            if (statustype.getVolgnummer() > hoogsteVolgnummer) {
                hoogsteVolgnummer = statustype.getVolgnummer();
            }
            switch (statustype.getOmschrijving()) {
                case ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE ->
                        zaaktypeInrichtingscheck.setStatustypeIntakeAanwezig(true);
                case ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING ->
                        zaaktypeInrichtingscheck.setStatustypeInBehandelingAanwezig(true);
                case ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND ->
                        zaaktypeInrichtingscheck.setStatustypeHeropendAanwezig(true);
                case ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AFGEROND -> {
                    afgerondVolgnummer = statustype.getVolgnummer();
                    zaaktypeInrichtingscheck.setStatustypeAfgerondAanwezig(true);
                }
            }
        }
        if (afgerondVolgnummer == hoogsteVolgnummer) {
            zaaktypeInrichtingscheck.setStatustypeAfgerondLaatsteVolgnummer(true);
        }
    }

    private void controleerZaaktypeResultaattypeInrichting(
            final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck) {
        final List<ResultaatType> resultaattypes =
                ztcClientService.readResultaattypen(
                        zaaktypeInrichtingscheck.getZaaktype().getUrl());
        if (CollectionUtils.isNotEmpty(resultaattypes)) {
            zaaktypeInrichtingscheck.setResultaattypeAanwezig(true);
            resultaattypes.forEach(
                    resultaattype -> {
                        final BrondatumArchiefprocedure.AfleidingswijzeEnum afleidingswijze =
                                resultaattype.getBrondatumArchiefprocedure().getAfleidingswijze();
                        // compare enum values and not the enums themselves because we have multiple
                        // functionally
                        // identical enums in our Java client code generated by the OpenAPI
                        // Generator
                        if (Afleidingswijze.VERVALDATUM_BESLUIT
                                        .toValue()
                                        .equals(afleidingswijze.value())
                                || Afleidingswijze.INGANGSDATUM_BESLUIT
                                        .toValue()
                                        .equals(afleidingswijze.value())) {
                            zaaktypeInrichtingscheck.addResultaattypesMetVerplichtBesluit(
                                    resultaattype.getOmschrijving());
                        }
                    });
        }
    }

    private void controleerZaaktypeBesluittypeInrichting(
            final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck) {
        final List<BesluitType> besluittypes =
                ztcClientService
                        .readBesluittypen(zaaktypeInrichtingscheck.getZaaktype().getUrl())
                        .stream()
                        .filter(LocalDateUtil::dateNowIsBetween)
                        .toList();
        if (CollectionUtils.isNotEmpty(besluittypes)) {
            zaaktypeInrichtingscheck.setBesluittypeAanwezig(true);
        }
    }

    private void controleerZaaktypeRoltypeInrichting(
            final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck) {
        final List<RolType> roltypes =
                ztcClientService.listRoltypen(zaaktypeInrichtingscheck.getZaaktype().getUrl());
        if (CollectionUtils.isNotEmpty(roltypes)) {
            roltypes.forEach(
                    roltype -> {
                        switch (roltype.getOmschrijvingGeneriek()) {
                            case ADVISEUR,
                                            MEDE_INITIATOR,
                                            BELANGHEBBENDE,
                                            BESLISSER,
                                            KLANTCONTACTER,
                                            ZAAKCOORDINATOR ->
                                    zaaktypeInrichtingscheck.setRolOverigeAanwezig(true);
                            case BEHANDELAAR ->
                                    zaaktypeInrichtingscheck.setRolBehandelaarAanwezig(true);
                            case INITIATOR ->
                                    zaaktypeInrichtingscheck.setRolInitiatorAanwezig(true);
                        }
                    });
        }
    }

    private void controleerZaaktypeInformatieobjecttypeInrichting(
            final ZaaktypeInrichtingscheck zaaktypeInrichtingscheck) {
        final List<InformatieObjectType> informatieobjecttypes =
                ztcClientService.readInformatieobjecttypen(
                        zaaktypeInrichtingscheck.getZaaktype().getUrl());
        informatieobjecttypes.forEach(
                informatieobjecttype -> {
                    if (isNuGeldig(informatieobjecttype)
                            && ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL.equals(
                                    informatieobjecttype.getOmschrijving())) {
                        zaaktypeInrichtingscheck.setInformatieobjecttypeEmailAanwezig(true);
                    }
                });
    }
}
