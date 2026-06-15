/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelInitiator } from "./klant-koppel-initiator.component";

@Component({ selector: "zac-persoon-zoek", template: "", standalone: true })
class PersoonZoekStubComponent {
  @Input() syncEnabled?: boolean;
  @Input() zaaktypeUUID?: string | null;
  @Output() persoon = new EventEmitter<GeneratedType<"RestPersoon">>();
}

@Component({ selector: "zac-bedrijf-zoek", template: "", standalone: true })
class BedrijfZoekStubComponent {
  @Input() syncEnabled?: boolean;
  @Output() bedrijf = new EventEmitter<GeneratedType<"RestBedrijf">>();
}

describe(KlantKoppelInitiator.name, () => {
  let fixture: ComponentFixture<KlantKoppelInitiator>;
  let component: KlantKoppelInitiator;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantKoppelInitiator,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    })
      .overrideComponent(KlantKoppelInitiator, {
        set: {
          imports: [
            TranslateModule,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(KlantKoppelInitiator);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when type is "persoon"', () => {
    beforeEach(() => {
      fixture.componentRef.setInput("type", "persoon");
      fixture.detectChanges();
    });

    it("should render zac-persoon-zoek", () => {
      expect(
        fixture.debugElement.query(By.directive(PersoonZoekStubComponent)),
      ).not.toBeNull();
    });

    it("should not render zac-bedrijf-zoek", () => {
      expect(
        fixture.debugElement.query(By.directive(BedrijfZoekStubComponent)),
      ).toBeNull();
    });

    it("should pass zaaktypeUUID to zac-persoon-zoek", () => {
      fixture.componentRef.setInput("zaaktypeUUID", "fake-zaaktype-uuid");
      fixture.detectChanges();

      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      expect(stub.zaaktypeUUID).toBe("fake-zaaktype-uuid");
    });

    it("should emit klantGegevens wrapping the persoon when a persoon is selected", () => {
      const emitted: KlantGegevens[] = [];
      component.klantGegevens.subscribe((v) => emitted.push(v));

      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      const persoon = fromPartial<GeneratedType<"RestPersoon">>({
        bsn: "999990408",
      });
      stub.persoon.emit(persoon);

      expect(emitted).toHaveLength(1);
      expect(emitted[0].klant).toBe(persoon);
    });
  });

  describe('when type is "bedrijf"', () => {
    beforeEach(() => {
      fixture.componentRef.setInput("type", "bedrijf");
      fixture.detectChanges();
    });

    it("should render zac-bedrijf-zoek", () => {
      expect(
        fixture.debugElement.query(By.directive(BedrijfZoekStubComponent)),
      ).not.toBeNull();
    });

    it("should not render zac-persoon-zoek", () => {
      expect(
        fixture.debugElement.query(By.directive(PersoonZoekStubComponent)),
      ).toBeNull();
    });

    it("should emit klantGegevens wrapping the bedrijf when a bedrijf is selected", () => {
      const emitted: KlantGegevens[] = [];
      component.klantGegevens.subscribe((v) => emitted.push(v));

      const stub = fixture.debugElement.query(
        By.directive(BedrijfZoekStubComponent),
      ).componentInstance as BedrijfZoekStubComponent;
      const bedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        kvkNummer: "12345678",
      });
      stub.bedrijf.emit(bedrijf);

      expect(emitted).toHaveLength(1);
      expect(emitted[0].klant).toBe(bedrijf);
    });
  });
});
