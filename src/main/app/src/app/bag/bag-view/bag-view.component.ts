/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BagLocatieComponent } from "../bag-locatie/bag-locatie.component";
import { BagZakenTabelComponent } from "../bag-zaken-tabel/bag-zaken-tabel.component";

@Component({
  templateUrl: "./bag-view.component.html",
  styleUrls: ["./bag-view.component.less"],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    I18nKeyPipe,
    MatCardModule,
    MatSidenavModule,
    TranslatePipe,
    StaticTextComponent,
    BagZakenTabelComponent,
    BagLocatieComponent,
  ],
})
export class BAGViewComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);

  protected bagIdentificatie!: string;
  protected adres?: GeneratedType<"RestBagAdres">;
  protected adresRegels?: string;
  protected openbareRuimte?: GeneratedType<"RestOpenbareRuimte">;
  protected woonplaats?: GeneratedType<"RestWoonplaats">;
  protected pand?: GeneratedType<"RestPand">;
  protected nummeraanduiding?: GeneratedType<"RestNummeraanduiding">;
  protected geometrie?: GeneratedType<"RestGeometry">;

  ngOnInit() {
    this.activatedRoute.data.subscribe((data) => {
      const bagObject: GeneratedType<"RestBagObject"> = data.bagObject;
      this.bagIdentificatie = bagObject.identificatie!;
      switch (bagObject.bagObjectType) {
        case "ADRES":
          this.adres = bagObject;
          this.adresRegels = joinNonEmpty(
            "\n",
            joinNonEmpty(
              " ",
              this.adres.openbareRuimteNaam,
              this.adres.huisnummerWeergave,
            ),
            joinNonEmpty(" ", this.adres.postcode, this.adres.woonplaatsNaam),
          );
          this.geometrie = this.adres.geometry ?? undefined;
          break;
        case "ADRESSEERBAAR_OBJECT":
          break; // (Nog) geen zelfstandige entiteit
        case "WOONPLAATS":
          this.woonplaats = bagObject;
          break;
        case "PAND":
          this.pand = bagObject;
          this.geometrie = this.pand.geometry ?? undefined;
          break;
        case "OPENBARE_RUIMTE":
          this.openbareRuimte = bagObject;
          break;
        case "NUMMERAANDUIDING":
          this.nummeraanduiding = bagObject;
          break;
      }
    });
  }
}

function joinNonEmpty(
  separator: string,
  ...parts: (string | null | undefined)[]
) {
  return parts.filter(Boolean).join(separator);
}
