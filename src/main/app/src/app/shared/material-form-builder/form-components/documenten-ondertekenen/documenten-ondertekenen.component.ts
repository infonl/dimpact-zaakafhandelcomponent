/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, DoCheck, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../../utils/generated-types";
import { DocumentenLijstComponent } from "../documenten-lijst/documenten-lijst.component";
import { DocumentenOndertekenenFormField } from "./documenten-ondertekenen-form-field";

@Component({
  templateUrl: "../documenten-lijst/documenten-lijst.component.html",
  styleUrls: ["../documenten-lijst/documenten-lijst.component.less"],
})
export class DocumentenOndertekenenComponent
  extends DocumentenLijstComponent
  implements OnInit, DoCheck
{
  data: DocumentenOndertekenenFormField;

  constructor(
    public translate: TranslateService,
    public informatieObjectenService: InformatieObjectenService,
  ) {
    super(translate, informatieObjectenService);
  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  ngDoCheck(): void {
    super.ngDoCheck();
  }

  toonFilterVeld(): boolean {
    return false;
  }

  selectDisabled(
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): boolean {
    return (
      this.data.readonly ||
      (document.rechten && !document.rechten?.ondertekenen) ||
      Boolean(document.ondertekening)
    );
  }

  isSelected(
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): boolean {
    return (
      this.selection.isSelected(document) || Boolean(document.ondertekening)
    );
  }
}
