/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatSelectChange } from "@angular/material/select";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute, Router } from "@angular/router";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { FormulierDefinitieService } from "../formulier-defintie.service";
import { FormulierVeldDefinitie } from "../model/formulieren/formulier-veld-definitie";
import { FormulierVeldtype } from "../model/formulieren/formulier-veld-type.enum";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { TekstvlakEditDialogComponent } from "./tekstvlak-edit-dialog/tekstvlak-edit-dialog.component";

@Component({
  templateUrl: "./formulier-definitie-edit.component.html",
  styleUrls: ["./formulier-definitie-edit.component.less"],
})
export class FormulierDefinitieEditComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  definitie?: GeneratedType<"RESTFormulierDefinitie">;
  definitieFormGroup?: FormGroup;
  veldColumns = [
    "label",
    "systeemnaam",
    "beschrijving",
    "helptekst",
    "veldtype",
    "defaultWaarde",
    "verplicht",
    "meerkeuzeOpties",
    "volgorde",
    "acties",
  ] as const;
  vorigeSysteemnaam?: string | null = null;
  bezigMetOpslaan = false;
  referentieLijsten: string[] = [];

  dataSource = new MatTableDataSource<AbstractControl>();

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private service: FormulierDefinitieService,
    private referentieService: ReferentieTabelService,
    private route: ActivatedRoute,
    private formBuilder: FormBuilder,
    private router: Router,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.referentieService.listReferentieTabellen().subscribe((tabellen) => {
      this.referentieLijsten = tabellen.map((value) => value.code);
    });

    this.route.data.subscribe((data) => {
      this.init(data.definitie);
    });
  }

  private init(definitie: GeneratedType<"RESTFormulierDefinitie">) {
    if (definitie.id) {
      this.setupMenu("title.formulierdefinitie.edit");
    } else {
      this.setupMenu("title.formulierdefinitie.add");
    }

    this.vorigeSysteemnaam = definitie.systeemnaam;

    if (!definitie.veldDefinities?.length) {
      definitie.veldDefinities = [];
    }

    this.definitieFormGroup = this.formBuilder.group({
      id: [definitie.id],
      naam: [definitie.naam, [Validators.required]],
      systeemnaam: [
        {
          value: definitie.systeemnaam,
          disabled: !!definitie.id,
        },
        [Validators.required, Validators.pattern("[a-z0-9_-]*")],
      ],
      beschrijving: [
        definitie.beschrijving,
        [Validators.required, Validators.maxLength(200)],
      ],
      uitleg: [definitie.uitleg],
      veldDefinities: this.formBuilder.array(
        definitie.veldDefinities.map((veld) =>
          FormulierVeldDefinitie.asFormGroup(veld),
        ),
      ),
    });
    (this.definitieFormGroup.get("veldDefinities") as FormArray).addValidators(
      Validators.required,
    ); // minimaal 1 veld definitie
    this.dataSource.data = (
      this.definitieFormGroup.get("veldDefinities") as FormArray
    ).controls;
  }

  updateSysteemnaam() {
    const isNew = !this.definitieFormGroup?.get("id")?.value;
    const naam = this.definitieFormGroup?.get("naam")?.value;
    const systeemnaam = this.toSysteemNaam(naam);
    // eslint-disable-next-line eqeqeq
    if (
      isNew &&
      this.definitieFormGroup?.get("systeemnaam")?.value ==
        this.vorigeSysteemnaam
    ) {
      this.definitieFormGroup?.get("systeemnaam")?.setValue(systeemnaam);
      this.vorigeSysteemnaam = systeemnaam;
    }
  }

  updateSysteemnaamVeld(formgroup: FormGroup) {
    const isNew = !formgroup.get("id")?.value;
    if (isNew) {
      // systeemnaam niet aanpassen bij bewerken
      const label = formgroup.get("label")?.value;
      formgroup.get("systeemnaam")?.setValue(this.toSysteemNaam(label));
    }
  }

  toSysteemNaam(naam: string) {
    return naam
      .replace(/[^a-zA-Z0-9 ]/g, "")
      .replace(/\s/g, "-")
      .toLowerCase();
  }

  addVeldDefinities() {
    const veldDefinities = this.definitieFormGroup?.get(
      "veldDefinities",
    ) as FormArray;
    const formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie"> =
      {
        volgorde: veldDefinities.length + 1,
      };
    veldDefinities.push(
      FormulierVeldDefinitie.asFormGroup(formulierVeldDefinitie),
    );
    this.dataSource.data = veldDefinities.controls;
  }

  removeVeldDefinitie(formgroup: FormGroup) {
    const veldDefinities = this.definitieFormGroup?.get(
      "veldDefinities",
    ) as FormArray;
    veldDefinities.removeAt(veldDefinities.controls.indexOf(formgroup));
    this.dataSource.data = veldDefinities.controls;
  }

  onVeldtypeChange($event: MatSelectChange, veldDefinitieFormGroup: FormGroup) {
    const veldtype: GeneratedType<"FormulierVeldtype"> = $event.value;
    if (FormulierVeldDefinitie.isMeerkeuzeVeld(veldtype)) {
      veldDefinitieFormGroup.get("meerkeuzeOpties")?.enable();
      veldDefinitieFormGroup
        .get("meerkeuzeOpties")
        ?.setValidators(Validators.required);
    } else {
      veldDefinitieFormGroup
        .get("meerkeuzeOpties")
        ?.removeValidators(Validators.required);
      veldDefinitieFormGroup.get("meerkeuzeOpties")?.disable();
    }
    veldDefinitieFormGroup.get("meerkeuzeOpties")?.updateValueAndValidity();
  }

  getVeldtypes() {
    return Object.keys(FormulierVeldtype);
  }

  opslaan() {
    this.bezigMetOpslaan = true;
    const val = this.definitieFormGroup
      ?.value as GeneratedType<"RESTFormulierDefinitie">;
    if (val.id) {
      this.service.update(val).subscribe((data) => {
        this.init(data);
        this.bezigMetOpslaan = false;
        this.utilService.openSnackbar("msg.formulierdefinitie.gewijzigd");
      });
    } else {
      this.service.create(val).subscribe((data) => {
        this.utilService.openSnackbar("msg.formulierdefinitie.toegevoegd");
        this.router.navigate(["admin/formulierdefinities", data.id]);
      });
    }
  }

  annuleren() {
    this.router.navigate(["/admin/formulierdefinities"]);
  }

  isTekstvlak(formGroup: FormGroup) {
    return formGroup.get("veldtype")?.value === FormulierVeldtype.TEKST_VLAK;
  }

  openTekstvlakEditDialog(formGroup: FormGroup) {
    this.dialog
      .open(TekstvlakEditDialogComponent, {
        width: "50%",
        data: {
          value: formGroup.get("defaultWaarde")?.value,
        },
      })
      .afterClosed()
      .subscribe((value) => {
        if (typeof value === "string") {
          formGroup.get("defaultWaarde")?.setValue(value);
        }
      });
  }
}
