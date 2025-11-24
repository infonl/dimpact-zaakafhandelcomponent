/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  effect,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from "@angular/core";
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
export class InformatieObjectAddComponent implements OnChanges, OnInit {
  @Input()
  infoObject?: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input() zaak?: GeneratedType<"RestZaak">;
  @Input() taak?: GeneratedType<"RestTask">;
  isTaakObject = false;

  @Output() document = new EventEmitter<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();

  protected createDocumentMutation = injectMutation(() => ({
    ...this.informatieObjectenService.createEnkelvoudigInformatieobject(
      this.zaakUuid,
      this.documentReferenceId,
      this.zaakUuid !== this.documentReferenceId,
    ),
    onSuccess: (data) => {
      this.document.emit(data);
      if (this.form.controls.addOtherInfoObject.value === true) {
        this.form.reset(this.defaultFormValues);
      } else {
        this.resetAndClose();
      }
    },
    onError: () => {
      this.form.reset();
    },
  }));

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
      Validators.maxLength(200),
    ]),
    addOtherInfoObject: this.formBuilder.control(false, []),
  });

  protected defaultFormValues: {
    taal: GeneratedType<"RestTaal"> | null;
    creatiedatum: Moment;
    auteur: string | null;
    addOtherInfoObject: boolean;
  } = {
    taal: null,
    creatiedatum: moment(),
    auteur: null,
    addOtherInfoObject: true, // default set to true, since this whole object is only used when adding other info object, and so is checked (and so is true)
  };

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  constructor(
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly configuratieService: ConfiguratieService,
    private readonly identityService: IdentityService,
    private readonly formBuilder: FormBuilder,
  ) {
    effect(() => {
      this.defaultFormValues.auteur =
        this.loggedInUserQuery.data()?.naam ?? null;
      this.form.controls.auteur.setValue(this.defaultFormValues.auteur);
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

  ngOnInit() {
    this.configuratieService.readDefaultTaal().subscribe((defaultTaal) => {
      this.defaultFormValues.taal = defaultTaal;
      this.form.controls.taal.setValue(this.defaultFormValues.taal);
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
        changes?.zaak?.currentValue.uuid ?? changes?.taak?.currentValue.id;

      this.informatieObjectenService
        .listInformatieobjecttypesForZaak(this.zaakUuid)
        .subscribe((informatieObjectTypes) => {
          this.informatieObjectTypes = informatieObjectTypes;
        });
    }
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
    this.isTaakObject = this.zaakUuid !== this.documentReferenceId;

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
    void this.sideNav.close();
    this.form.reset();
  }
}
