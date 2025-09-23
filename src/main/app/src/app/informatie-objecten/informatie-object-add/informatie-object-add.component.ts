/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import moment, { Moment } from "moment";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

@Component({
  selector: "zac-informatie-object-add",
  templateUrl: "./informatie-object-add.component.html",
})
export class InformatieObjectAddComponent implements OnChanges {
  @Input()
  infoObject?: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input() zaak?: GeneratedType<"RestZaak">;
  @Input() taak?: GeneratedType<"RestTask">;

  @Output() document = new EventEmitter<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();

  protected zaakUuid!: string;
  protected documentReferenceId!: string;

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
    ]),
    repeat: this.formBuilder.control(false, []),
  });

  constructor(
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly configuratieService: ConfiguratieService,
    private readonly identityService: IdentityService,
    private readonly vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
    private readonly formBuilder: FormBuilder,
  ) {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.form.controls.auteur.setValue(ingelogdeMedewerker.naam);
    });

    this.configuratieService.readDefaultTaal().subscribe((defaultTaal) => {
      this.form.controls.taal.setValue(defaultTaal);
    });

    this.form.controls.ontvangstdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        console.log(
          "ontvangstdatumontvangstdatumontvangstdatumontvangstdatum",
          value,
        );
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
        console.log("VERZENDDATUM", value);
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
    if (
      (changes.zaak && changes.zaak.currentValue) ||
      (changes.taak && changes.taak.currentValue)
    ) {
      this.zaakUuid =
        changes?.zaak?.currentValue.uuid ??
        changes?.taak?.currentValue.zaakUuid;
      this.documentReferenceId =
        changes?.taak?.currentValue.zaakUuid ?? this.zaakUuid;

      this.informatieObjectenService
        .listInformatieobjecttypesForZaak(this.zaakUuid)
        .subscribe((informatieObjectTypes) => {
          this.informatieObjectTypes = informatieObjectTypes;
        });
    }
  }

  submit() {
    const { value } = this.form;

    this.informatieObjectenService
      .createEnkelvoudigInformatieobject(
        this.zaakUuid,
        this.documentReferenceId,
        {
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
        },
        this.zaakUuid !== this.documentReferenceId,
      )
      .subscribe({
        next: (document) => {
          this.document.emit(document);
          this.utilService.openSnackbar(
            "msg.document.nieuwe.versie.toegevoegd",
          );
          if (value.repeat) {
            this.form.reset();
            return;
          }
          this.resetAndClose();
        },
        error: (err) => {
          console.error(err);
        },
      });
  }

  protected resetAndClose() {
    void this.sideNav.close();
    this.form.reset();
  }
}
