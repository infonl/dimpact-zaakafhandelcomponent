/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input, output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { PostBody } from "../../shared/http/http-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

@Component({
  selector: "zac-informatie-object-add",
  templateUrl: "./informatie-object-add.component.html",
})
export class InformatieObjectAddComponent {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly utilService = inject(UtilService);
  private readonly configuratieService = inject(ConfiguratieService);
  private readonly identityService = inject(IdentityService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly infoObject =
    input<GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaakUuid = input.required<string>();
  protected readonly taakId = input<string>();

  protected readonly document =
    output<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  private readonly defaultTaalQuery = injectQuery(() =>
    this.configuratieService.readDefaultTaal(),
  );

  protected createDocumentMutation = injectMutation(() => ({
    ...this.informatieObjectenService.createEnkelvoudigInformatieobject(
      this.zaakUuid(),
      this.taakId() ?? this.zaakUuid(),
      !!this.taakId(),
    ),
    onSuccess: (data) => {
      this.document.emit(data);
      this.utilService.openSnackbar("msg.document.nieuwe.versie.toegevoegd");
      if (this.form.controls.addOtherInfoObject.value) {
        this.form.reset({
          taal: this.defaultTaalQuery.data(),
          auteur: this.loggedInUserQuery.data()?.naam ?? null,
          creatiedatum: moment(),
          addOtherInfoObject: true,
        });
        return;
      }
      this.resetAndClose();
    },
    onError: () => {
      this.form.reset();
    },
  }));

  protected readonly informatieobjectStatussen =
    this.utilService.getEnumAsSelectListExceptFor(
      "informatieobject.status",
      InformatieobjectStatus,
      [InformatieobjectStatus.GEARCHIVEERD],
    );

  protected readonly vertrouwelijkheidsAanduidingen =
    this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );

  protected informatieObjectTypes: GeneratedType<"RestInformatieobjecttype">[] =
    [];

  protected readonly talen = this.configuratieService.listTalen();

  protected readonly form = this.formBuilder.group({
    bestand: this.formBuilder.control<File | null>(null, []),
    titel: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(100),
    ]),
    beschrijving: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
    taal: this.formBuilder.control<GeneratedType<"RestTaal"> | null>(null, [
      Validators.required,
    ]),
    status: this.formBuilder.control<
      (typeof this.informatieobjectStatussen)[number] | null
    >(null, [Validators.required]),
    creatiedatum: this.formBuilder.control<Moment | null>(moment(), [
      Validators.required,
    ]),
    verzenddatum: this.formBuilder.control<Moment | null>(null),
    ontvangstdatum: this.formBuilder.control<Moment | null>(null),
    informatieobjectType:
      this.formBuilder.control<GeneratedType<"RestInformatieobjecttype"> | null>(
        null,
        [Validators.required],
      ),
    vertrouwelijkheidaanduiding: this.formBuilder.control<
      (typeof this.vertrouwelijkheidsAanduidingen)[number] | null
    >(null, [Validators.required]),
    auteur: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
    addOtherInfoObject: this.formBuilder.control(false, []),
  });

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  constructor() {
    effect(
      () => {
        this.form.controls.auteur.setValue(
          this.loggedInUserQuery.data()?.naam ?? null,
        );
      },
      { allowSignalWrites: true },
    );

    effect(
      () => {
        this.form.controls.taal.setValue(this.defaultTaalQuery.data() ?? null);
      },
      { allowSignalWrites: true },
    );

    effect(() => {
      this.informatieObjectenService
        .listInformatieobjecttypesForZaak(this.zaakUuid())
        .subscribe((informatieObjectTypes) => {
          this.informatieObjectTypes = informatieObjectTypes;
        });
    });

    this.form.controls.ontvangstdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value && this.form.controls.verzenddatum.disabled) {
          this.form.controls.status.enable();
          this.form.controls.verzenddatum.enable();
          return;
        }

        if (value && this.form.controls.verzenddatum.enabled) {
          this.form.controls.status.disable();
          this.form.controls.verzenddatum.disable();
          this.form.controls.status.setValue(
            this.informatieobjectStatussen.find(
              (option) =>
                option.value.toLowerCase() ===
                InformatieobjectStatus.DEFINITIEF,
            ) ?? null,
          );
          return;
        }
      });

    this.form.controls.verzenddatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value && this.form.controls.ontvangstdatum.disabled) {
          this.form.controls.ontvangstdatum.enable();
          return;
        }

        if (value && this.form.controls.ontvangstdatum.enabled) {
          this.form.controls.ontvangstdatum.disable();
        }
      });

    this.form.controls.bestand.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        this.form.controls.titel.setValue(
          value?.name?.replace(/\.[^/.]+$/, "") || "",
        );
      });
  }

  private toInformatieobjectFormData(
    infoObject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      bestand: File;
    },
  ): FormData {
    const formData = new FormData();
    for (const [key, value] of Object.entries(infoObject)) {
      if (value === undefined || value === null) continue;
      switch (key) {
        case "creatiedatum":
        case "ontvangstdatum":
        case "verzenddatum":
          formData.append(
            key,
            moment(value.toString()).format("YYYY-MM-DDThh:mmZ"),
          );
          break;
        case "bestand":
          formData.append("file", value as Blob, infoObject.bestandsnaam!);
          break;
        default:
          formData.append(key, value.toString());
          break;
      }
    }
    return formData;
  }

  submit() {
    const { value } = this.form;
    const payload = {
      bestand: value.bestand!,
      bestandsnaam: value.bestand?.name,
      formaat: value.bestand?.type,
      titel: value.titel!,
      beschrijving: value.beschrijving,
      informatieobjectTypeUUID: value.informatieobjectType!.uuid!,
      status: value.status?.value as unknown as GeneratedType<"StatusEnum">,
      vertrouwelijkheidaanduiding: value.vertrouwelijkheidaanduiding?.value,
      creatiedatum: value.creatiedatum?.toISOString(),
      verzenddatum: value.verzenddatum?.toISOString(),
      ontvangstdatum: value.ontvangstdatum?.toISOString(),
      taal: value.taal!.code,
      auteur: value.auteur!,
    };
    const formData = this.toInformatieobjectFormData(payload);

    this.createDocumentMutation.mutate(
      formData as unknown as PostBody<"/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}">,
    );
  }

  protected resetAndClose() {
    void this.sideNav().close();
    this.form.reset();
  }
}
