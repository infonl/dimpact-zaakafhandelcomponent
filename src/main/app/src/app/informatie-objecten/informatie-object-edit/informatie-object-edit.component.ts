/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import moment, { Moment } from "moment";
import { lastValueFrom } from "rxjs";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

@Component({
  selector: "zac-informatie-object-edit",
  templateUrl: "./informatie-object-edit.component.html",
})
export class InformatieObjectEditComponent implements OnChanges {
  @Input()
  infoObject?: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) zaakUuid!: string;

  @Output() document = new EventEmitter<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();

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
    ]),
    toelichting: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(1000),
    ]),
  });

  constructor(
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly configuratieService: ConfiguratieService,
    private readonly translateService: TranslateService,
    private readonly identityService: IdentityService,
    private readonly vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
    private readonly formBuilder: FormBuilder,
  ) {
    this.form.controls.ontvangstdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value && !this.form.controls.verzenddatum.disabled) {
          this.form.controls.status.enable();
          this.form.controls.verzenddatum.enable();
          return;
        }

        if (value && this.form.controls.verzenddatum.enabled) {
          this.form.controls.status.disable();
          this.form.controls.verzenddatum.disable();
          this.form.controls.status.setValue(
            this.informatieobjectStatussen.find(
              (option) => option.value === InformatieobjectStatus.DEFINITIEF,
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

  ngOnChanges(changes: SimpleChanges) {
    if (changes.infoObject && changes.infoObject.currentValue) {
      this.infoObject = changes.infoObject.currentValue;
      if (!this.infoObject) return;
      void this.initForm(this.infoObject);
    }
  }

  async initForm(
    infoObject: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">,
  ) {
    const ingelogdeMedewerker = await lastValueFrom(
      this.identityService.readLoggedInUser(),
    );

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
            label: this.translateService.instant(
              this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
                infoObject.vertrouwelijkheidaanduiding as GeneratedType<"VertrouwelijkheidaanduidingEnum">, // TODO: `RestEnkelvoudigInformatieObjectVersieGegevens` has the wrong `vertrouwelijkheidaanduiding` type
              ),
            ),
            value: infoObject.vertrouwelijkheidaanduiding,
          }
        : null,
      auteur: infoObject.auteur ?? ingelogdeMedewerker?.naam,
    });

    this.informatieObjectenService
      .listInformatieobjecttypesForZaak(this.zaakUuid)
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

  submit() {
    const { value } = this.form;
    this.informatieObjectenService
      .updateEnkelvoudigInformatieobject(
        this.infoObject!.uuid!,
        this.zaakUuid,
        {
          ...value,
          informatieobjectTypeUUID: value.informatieobjectType!.uuid!,
          status: value.status?.value as unknown as GeneratedType<"StatusEnum">,
          vertrouwelijkheidaanduiding: value.vertrouwelijkheidaanduiding?.value,
          bestandsnaam: value.bestand?.name,
          verzenddatum: value.verzenddatum?.toISOString(),
          ontvangstdatum: value.ontvangstdatum?.toISOString(),
          file: value.bestand as unknown as string,
          formaat: value.bestand?.type,
        },
      )
      .subscribe((document) => {
        this.document.emit(document);
        this.utilService.openSnackbar("msg.document.nieuwe.versie.toegevoegd");
        this.resetAndClose();
      });
  }

  protected resetAndClose() {
    void this.sideNav.close();
    this.form.reset();
  }
}
