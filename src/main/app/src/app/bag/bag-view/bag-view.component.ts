/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, inject, signal } from "@angular/core";
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

  protected readonly bagIdentificatie = signal("");
  protected readonly adres = signal<GeneratedType<"RestBagAdres"> | undefined>(
    undefined,
  );
  protected readonly adresRegels = signal<string | undefined>(undefined);
  protected readonly openbareRuimte = signal<
    GeneratedType<"RestOpenbareRuimte"> | undefined
  >(undefined);
  protected readonly woonplaats = signal<
    GeneratedType<"RestWoonplaats"> | undefined
  >(undefined);
  protected readonly pand = signal<GeneratedType<"RestPand"> | undefined>(
    undefined,
  );
  protected readonly nummeraanduiding = signal<
    GeneratedType<"RestNummeraanduiding"> | undefined
  >(undefined);
  protected readonly geometrie = signal<
    GeneratedType<"RestGeometry"> | undefined
  >(undefined);

  ngOnInit() {
    this.activatedRoute.data.subscribe((data) => {
      const bagObject: GeneratedType<"RestBagObject"> = data.bagObject;
      this.bagIdentificatie.set(bagObject.identificatie!);
      switch (bagObject.bagObjectType) {
        case "ADRES": {
          const adres: GeneratedType<"RestBagAdres"> = bagObject;
          this.adres.set(adres);
          this.adresRegels.set(
            joinNonEmpty(
              "\n",
              joinNonEmpty(
                " ",
                adres.openbareRuimteNaam,
                adres.huisnummerWeergave,
              ),
              joinNonEmpty(" ", adres.postcode, adres.woonplaatsNaam),
            ),
          );
          this.geometrie.set(adres.geometry ?? undefined);
          break;
        }
        case "ADRESSEERBAAR_OBJECT":
          break; // (Nog) geen zelfstandige entiteit
        case "WOONPLAATS":
          this.woonplaats.set(bagObject);
          break;
        case "PAND": {
          const pand: GeneratedType<"RestPand"> = bagObject;
          this.pand.set(pand);
          this.geometrie.set(pand.geometry ?? undefined);
          break;
        }
        case "OPENBARE_RUIMTE":
          this.openbareRuimte.set(bagObject);
          break;
        case "NUMMERAANDUIDING":
          this.nummeraanduiding.set(bagObject);
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
