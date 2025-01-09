/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { FormControlStatus, FormGroup } from "@angular/forms";
import moment from "moment";
import { Moment } from "moment/moment";
import { UtilService } from "../../../core/service/util.service";
import { DateFormField } from "../../material-form-builder/form-components/date/date-form-field";
import { InputFormField } from "../../material-form-builder/form-components/input/input-form-field";
import { MaterialFormBuilderService } from "../../material-form-builder/material-form-builder.service";
import { EditComponent } from "../edit.component";
import { TextIcon } from "../text-icon";

@Component({
  selector: "zac-edit-datum-groep",
  templateUrl: "./edit-datum-groep.component.html",
  styleUrls: [
    "../../static-text/static-text.component.less",
    "../edit.component.less",
    "./edit-datum-groep.component.less",
  ],
})
export class EditDatumGroepComponent
  extends EditComponent
  implements OnChanges, OnInit
{
  @Input() formField: DateFormField;
  @Input() startDatumField: DateFormField;
  @Input() einddatumGeplandField: DateFormField;
  @Input() uiterlijkeEinddatumAfdoeningField: DateFormField;
  @Input() einddatumGeplandIcon: TextIcon;
  @Input() uiterlijkeEinddatumAfdoeningIcon: TextIcon;
  @Input() reasonField: InputFormField;
  @Input() opgeschort: boolean;
  @Input() opschortDuur: number;
  @Input() verlengDuur: string;

  showEinddatumGeplandIcon: boolean;
  showUiterlijkeEinddatumAfdoeningIcon: boolean;
  showEinddatumGeplandError: boolean;
  showUiterlijkeEinddatumAfdoeningError: boolean;

  duurField: InputFormField;
  werkelijkeOpschortDuur: number;

  editFormFieldIcons: Map<string, TextIcon> = new Map<string, TextIcon>();

  constructor(
    mfbService: MaterialFormBuilderService,
    utilService: UtilService,
  ) {
    super(mfbService, utilService);
  }

  ngOnInit(): void {
    this.updateGroep();
  }

  ngOnChanges(changes: SimpleChanges): void {
    super.ngOnChanges(changes);
    this.updateGroep();
  }

  updateGroep(): void {
    this.showEinddatumGeplandIcon = this.einddatumGeplandIcon?.showIcon(
      this.einddatumGeplandField.formControl,
    );
    this.showUiterlijkeEinddatumAfdoeningIcon =
      this.uiterlijkeEinddatumAfdoeningIcon?.showIcon(
        this.uiterlijkeEinddatumAfdoeningField.formControl,
      );
  }

  save(): void {
    if (this.formFields.valid) {
      this.validate();
      if (
        !this.showEinddatumGeplandError &&
        !this.showUiterlijkeEinddatumAfdoeningError
      ) {
        this.onSave.emit({
          startdatum: this.startDatumField.formControl.value,
          einddatumGepland: this.einddatumGeplandField.formControl.value,
          uiterlijkeEinddatumAfdoening:
            this.uiterlijkeEinddatumAfdoeningField.formControl.value,
          reden: this.reasonField?.formControl.value,
        });
        this.editing = false;
      }
    }
  }

  validate(): void {
    const start: Moment = moment(this.startDatumField.formControl.value);
    const uiterlijkeEinddatumAfdoening: Moment = moment(
      this.uiterlijkeEinddatumAfdoeningField.formControl.value,
    );
    if (this.einddatumGeplandField.formControl.value) {
      const einddatumGepland: Moment = moment(
        this.einddatumGeplandField.formControl.value,
      );
      this.showEinddatumGeplandError =
        einddatumGepland.isBefore(start) ||
        uiterlijkeEinddatumAfdoening.isBefore(einddatumGepland);
      this.showUiterlijkeEinddatumAfdoeningError =
        uiterlijkeEinddatumAfdoening.isBefore(start) ||
        uiterlijkeEinddatumAfdoening.isBefore(einddatumGepland);
    } else {
      this.showUiterlijkeEinddatumAfdoeningError =
        uiterlijkeEinddatumAfdoening.isBefore(start);
    }
  }

  hasError(): boolean {
    return (
      this.showEinddatumGeplandError ||
      this.showUiterlijkeEinddatumAfdoeningError
    );
  }

  edit(): void {
    if (!this.readonly && !this.utilService.hasEditOverlay()) {
      this.editing = true;

      this.formFields = new FormGroup({
        startdatum: this.startDatumField.formControl,
        einddatumGepland: this.einddatumGeplandField.formControl,
        uiterlijkeEinddatumAfdoening:
          this.uiterlijkeEinddatumAfdoeningField.formControl,
        reden: this.reasonField.formControl,
      });

      this.formFields.statusChanges.subscribe((status: FormControlStatus) => {
        this.isInValid = this.formFields.dirty && status !== "VALID";
      });
    }
  }
}
