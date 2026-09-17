/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input, output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { ZacDate } from "../../shared/form/date/date";
import { ZacFile } from "../../shared/form/file/file";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../shared/form/input/input";
import { ZacSelect } from "../../shared/form/select/select";
import { PutBody } from "../../shared/http/http-client";
import { injectMutation } from "../../shared/http/inject-mutation";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { toDocumentFormData } from "../../shared/utils/file-upload";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";

@Component({
  selector: "zac-informatie-object-edit",
  templateUrl: "./informatie-object-edit.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatToolbarModule,
    TranslateModule,
    ZacDate,
    ZacFile,
    ZacFormActions,
    ZacInput,
    ZacSelect,
    MaterialFormBuilderModule,
  ],
})
export class InformatieObjectEditComponent {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly utilService = inject(UtilService);
  private readonly configuratieService = inject(ConfiguratieService);
  private readonly translateService = inject(TranslateService);
  private readonly identityService = inject(IdentityService);
  private readonly vertrouwelijkaanduidingToTranslationKeyPipe = inject(
    VertrouwelijkaanduidingToTranslationKeyPipe,
  );
  private readonly formBuilder = inject(FormBuilder);
  private readonly queryClient = inject(QueryClient);

  protected readonly infoObject =
    input<GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaakUuid = input.required<string>();

  protected readonly document =
    output<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  protected readonly updateDocumentMutation = injectMutation(
    () =>
      this.informatieObjectenService.updateEnkelvoudigInformatieobject(
        this.infoObject()?.uuid ?? "",
        this.zaakUuid(),
      ),
    {
      onSuccess: (document) => {
        this.document.emit(document);
        this.resetAndClose();
      },
    },
  );

  protected readonly informatieobjectStatussen =
    this.utilService.getEnumAsSelectListExceptFor(
      "informatieobject.status",
      InformatieobjectStatus,
      [InformatieobjectStatus.GEARCHIVEERD],
    );

  protected readonly vertrouwelijkheidsAanduidingen =
    VertrouwelijkaanduidingToTranslationKeyPipe.selectList;

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
    toelichting: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(1000),
    ]),
  });

  constructor() {
    effect(() => {
      const infoObject = this.infoObject();
      if (!infoObject) return;
      void this.initForm(infoObject);
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

  async initForm(
    infoObject: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">,
  ) {
    let auteur = infoObject.auteur;
    if (!auteur) {
      const { naam } = await this.queryClient.ensureQueryData(
        this.identityService.readLoggedInUser(),
      );
      auteur = naam;
    }

    this.form.patchValue({
      ...infoObject,
      status: infoObject.status
        ? {
            label: this.translateService.instant(
              "informatieobject.status." + infoObject.status,
            ),
            value: infoObject.status,
          }
        : null,
      verzenddatum: infoObject.verzenddatum
        ? moment(infoObject.verzenddatum)
        : null,
      ontvangstdatum: infoObject.ontvangstdatum
        ? moment(infoObject.ontvangstdatum)
        : null,
      vertrouwelijkheidaanduiding: infoObject.vertrouwelijkheidaanduiding
        ? {
            label: this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
              infoObject.vertrouwelijkheidaanduiding,
            ),
            value: infoObject.vertrouwelijkheidaanduiding,
          }
        : null,
      auteur: auteur,
    });

    this.informatieObjectenService
      .listInformatieobjecttypesForZaak(this.zaakUuid())
      .subscribe((informatieObjectTypes) => {
        this.informatieObjectTypes = informatieObjectTypes;
        this.form.controls.informatieobjectType.patchValue(
          informatieObjectTypes.find(
            (informatieObjectType) =>
              informatieObjectType.uuid === infoObject.informatieobjectTypeUUID,
          ) ?? null,
        );
      });

    if (infoObject.ontvangstdatum) {
      this.form.controls.verzenddatum.disable();
      this.form.controls.status.disable();
    }

    if (infoObject.verzenddatum) {
      this.form.controls.ontvangstdatum.disable();
    }
  }

  protected submit() {
    const value = this.form.getRawValue();
    const formData = toDocumentFormData({
      bestand: value.bestand,
      bestandsnaam: value.bestand?.name,
      formaat: value.bestand?.type,
      titel: value.titel,
      beschrijving: value.beschrijving,
      informatieobjectTypeUUID: value.informatieobjectType!.uuid!,
      status: value.status?.value,
      vertrouwelijkheidaanduiding: value.vertrouwelijkheidaanduiding?.value,
      verzenddatum: value.verzenddatum?.toISOString(),
      ontvangstdatum: value.ontvangstdatum?.toISOString(),
      taal: value.taal,
      auteur: value.auteur,
      toelichting: value.toelichting,
    });

    this.updateDocumentMutation.mutate(
      formData as unknown as PutBody<"/rest/informatieobjecten/informatieobject/{uuid}">,
    );
  }

  protected resetAndClose() {
    void this.sideNav().close();
    this.form.reset();
  }
}
