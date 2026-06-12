/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { KlantenService } from "src/app/klanten/klanten.service";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelBetrokkeneComponent } from "./klant-koppel-betrokkene.component";

@Component({ selector: "zac-persoon-zoek", template: "", standalone: true })
class PersoonZoekStubComponent {
  @Input() syncEnabled?: boolean;
  @Input() blockSearch?: boolean;
  @Input() zaaktypeUUID?: string | null;
  @Output() persoon = new EventEmitter<GeneratedType<"RestPersoon">>();
}

@Component({ selector: "zac-bedrijf-zoek", template: "", standalone: true })
class BedrijfZoekStubComponent {
  @Input() syncEnabled?: boolean;
  @Input() blockSearch?: boolean;
  @Output() bedrijf = new EventEmitter<GeneratedType<"RestBedrijf">>();
}

@Component({ selector: "zac-select", template: "", standalone: true })
class SelectStubComponent {
  @Input() form: unknown;
  @Input() key?: string;
  @Input() options?: unknown[];
  @Input() optionDisplayValue?: string;
}

@Component({ selector: "zac-input", template: "", standalone: true })
class InputStubComponent {
  @Input() form: unknown;
  @Input() key?: string;
}

const fakeRoltype = fromPartial<GeneratedType<"RestRoltype">>({
  uuid: "fake-roltype-uuid",
  naam: "fakeRoltype",
});

describe(KlantKoppelBetrokkeneComponent.name, () => {
  let fixture: ComponentFixture<KlantKoppelBetrokkeneComponent>;
  let component: KlantKoppelBetrokkeneComponent;
  let klantenService: KlantenService;

  function createFixture(
    type: "persoon" | "bedrijf" = "persoon",
    zaaktypeUUID?: string,
  ) {
    fixture = TestBed.createComponent(KlantKoppelBetrokkeneComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("type", type);
    if (zaaktypeUUID) fixture.componentRef.setInput("zaaktypeUUID", zaaktypeUUID);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantKoppelBetrokkeneComponent,
        NoopAnimationsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: KlantenService,
          useValue: {
            listBetrokkeneRoltypen: jest.fn().mockReturnValue(of([])),
          },
        },
      ],
    })
      .overrideComponent(KlantKoppelBetrokkeneComponent, {
        set: {
          imports: [
            NgIf,
            ReactiveFormsModule,
            TranslateModule,
            SelectStubComponent,
            InputStubComponent,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

    klantenService = TestBed.inject(KlantenService);
  });

  describe('when type is "persoon"', () => {
    beforeEach(() => createFixture("persoon"));

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
  });

  describe('when type is "bedrijf"', () => {
    beforeEach(() => createFixture("bedrijf"));

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
  });

  describe("ngOnInit", () => {
    it("should load betrokkeneRoltypen when zaaktypeUUID is provided", () => {
      const roltypen = [fakeRoltype];
      jest
        .spyOn(klantenService, "listBetrokkeneRoltypen")
        .mockReturnValue(of(roltypen));

      createFixture("persoon", "fake-zaaktype-uuid");

      expect(klantenService.listBetrokkeneRoltypen).toHaveBeenCalledWith(
        "fake-zaaktype-uuid",
      );
      expect(component["betrokkeneRoltypen"]).toEqual(roltypen);
    });

    it("should not call the service when zaaktypeUUID is not provided", () => {
      createFixture("persoon");
      expect(klantenService.listBetrokkeneRoltypen).not.toHaveBeenCalled();
    });
  });

  describe("blockSearch", () => {
    beforeEach(() => createFixture("persoon"));

    it("should block search when betrokkeneRoltype is not set", () => {
      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      expect(stub.blockSearch).toBe(true);
    });

    it("should not block search when betrokkeneRoltype is set", () => {
      component["form"].patchValue({ betrokkeneRoltype: fakeRoltype });
      fixture.detectChanges();

      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      expect(stub.blockSearch).toBe(false);
    });
  });

  describe("klantGeselecteerd", () => {
    beforeEach(() => createFixture("persoon"));

    it("should emit klantGegevens with klant, betrokkeneRoltype and toelichting", () => {
      const emitted: KlantGegevens[] = [];
      component.klantGegevens.subscribe((v) => emitted.push(v));

      component["form"].patchValue({
        betrokkeneRoltype: fakeRoltype,
        toelichting: "fakeNote",
      });
      fixture.detectChanges();

      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      const persoon = fromPartial<GeneratedType<"RestPersoon">>({
        bsn: "999990408",
      });
      stub.persoon.emit(persoon);

      expect(emitted).toHaveLength(1);
      expect(emitted[0].klant).toBe(persoon);
      expect(emitted[0].betrokkeneRoltype).toBe(fakeRoltype);
      expect(emitted[0].betrokkeneToelichting).toBe("fakeNote");
    });

    it("should emit empty string for betrokkeneToelichting when not filled in", () => {
      const emitted: KlantGegevens[] = [];
      component.klantGegevens.subscribe((v) => emitted.push(v));

      component["form"].patchValue({ betrokkeneRoltype: fakeRoltype });
      fixture.detectChanges();

      const stub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      stub.persoon.emit(
        fromPartial<GeneratedType<"RestPersoon">>({ bsn: "999990408" }),
      );

      expect(emitted[0].betrokkeneToelichting).toBe("");
    });
  });
});
