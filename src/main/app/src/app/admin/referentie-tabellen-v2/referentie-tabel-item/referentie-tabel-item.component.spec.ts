/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelItemComponent } from "./referentie-tabel-item.component";

const tabel: GeneratedType<"RestReferenceTable"> = {
  id: 1,
  code: "TABEL_A",
  naam: "Tabel A",
  systeem: false,
  waarden: [
    { id: 10, naam: "Waarde A1" },
    { id: 11, naam: "Waarde A2" },
  ],
};

describe(ReferentieTabelItemComponent.name, () => {
  let fixture: ComponentFixture<ReferentieTabelItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReferentieTabelItemComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReferentieTabelItemComponent);
    fixture.componentRef.setInput("tabel", tabel);
    fixture.detectChanges();
  });

  it("should render a table row per waarde", () => {
    const rows = fixture.nativeElement.querySelectorAll("tr[mat-row]");
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain("Waarde A1");
    expect(fixture.nativeElement.textContent).toContain("Waarde A2");
  });

  it("should show an empty message when there are no waarden", () => {
    fixture.componentRef.setInput("tabel", {
      ...tabel,
      waarden: [],
    } satisfies GeneratedType<"RestReferenceTable">);
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll("tr[mat-row]");
    expect(rows).toHaveLength(0);
    expect(fixture.nativeElement.textContent).toContain(
      "msg.geen.gegevens.gevonden",
    );
  });
});
