/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick,
} from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZoekVeld } from "../../../model/zoek-veld";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";
import { ZaakBetrokkeneFilterComponent } from "./zaak-betrokkene-filter.component";

const makeZoekParams = (
  zoeken: { [key: string]: string } = {},
): GeneratedType<"RestZoekParameters"> =>
  ({
    page: 0,
    rows: 25,
    zoeken,
  }) as Partial<
    GeneratedType<"RestZoekParameters">
  > as unknown as GeneratedType<"RestZoekParameters">;

describe(ZaakBetrokkeneFilterComponent.name, () => {
  let fixture: ComponentFixture<ZaakBetrokkeneFilterComponent>;
  let loader: HarnessLoader;
  let component: ZaakBetrokkeneFilterComponent;
  let dialog: MatDialog;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [KlantZoekDialog],
      imports: [
        ZaakBetrokkeneFilterComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakBetrokkeneFilterComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    dialog = fixture.debugElement.injector.get(MatDialog);
  });

  describe("ngOnInit — roltype initialisation", () => {
    it("defaults huidigeRoltype to ZAAK_INITIATOR when zoeken is empty", () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();

      expect(component["huidigeRoltype"]).toBe(ZoekVeld.ZAAK_INITIATOR);
    });

    it("sets huidigeRoltype to ZAAK_BETROKKENEN when that key is present", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_BETROKKENEN: "123",
      });
      fixture.detectChanges();

      expect(component["huidigeRoltype"]).toBe(ZoekVeld.ZAAK_BETROKKENEN);
    });

    it("sets huidigeRoltype to ZAAK_BETROKKENE_BELANGHEBBENDE when that key is present", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_BETROKKENE_BELANGHEBBENDE: "456",
      });
      fixture.detectChanges();

      expect(component["huidigeRoltype"]).toBe(
        ZoekVeld.ZAAK_BETROKKENE_BELANGHEBBENDE,
      );
    });

    it("populates klantIdControl from the active roltype key", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_INITIATOR: "987654321",
      });
      fixture.detectChanges();

      expect(component["klantIdControl"].value).toBe("987654321");
    });

    it("leaves klantIdControl empty when zoekparameters has no matching value", () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();

      expect(component["klantIdControl"].value).toBe("");
    });
  });

  describe("idChanged", () => {
    it("emits changed and updates zoekparameters when the ID value changes", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_INITIATOR: "old",
      });
      fixture.detectChanges();
      const changedSpy = jest.fn();
      component.changed.subscribe(changedSpy);

      component["klantIdControl"].setValue("new");
      component["idChanged"]();

      expect(changedSpy).toHaveBeenCalledTimes(1);
      expect(component.zoekparameters.zoeken!["ZAAK_INITIATOR"]).toBe("new");
    });

    it("does not emit changed when the ID value is unchanged", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_INITIATOR: "same",
      });
      fixture.detectChanges();
      component["klantIdControl"].setValue("same");
      const changedSpy = jest.fn();
      component.changed.subscribe(changedSpy);

      component["idChanged"]();

      expect(changedSpy).not.toHaveBeenCalled();
    });

    it("does not throw when zoekparameters.zoeken is absent", () => {
      component.zoekparameters = makeZoekParams();
      component.zoekparameters.zoeken = null;
      fixture.detectChanges();

      expect(() => component["idChanged"]()).not.toThrow();
    });
  });

  describe("roltypeChanged", () => {
    it("transfers the old value to the new roltype key and emits changed", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_INITIATOR: "bsn123",
      });
      fixture.detectChanges();
      const changedSpy = jest.fn();
      component.changed.subscribe(changedSpy);

      component["betrokkeneSelectControl"].setValue(
        ZoekVeld.ZAAK_BETROKKENE_ADVISEUR,
      );
      component["roltypeChanged"]();

      expect(
        component.zoekparameters.zoeken!["ZAAK_INITIATOR"],
      ).toBeUndefined();
      expect(component.zoekparameters.zoeken!["ZAAK_BETROKKENE_ADVISEUR"]).toBe(
        "bsn123",
      );
      expect(changedSpy).toHaveBeenCalledTimes(1);
    });

    it("does not emit changed when there was no previous ID value", () => {
      component.zoekparameters = makeZoekParams({
        ZAAK_INITIATOR: "",
      });
      fixture.detectChanges();
      const changedSpy = jest.fn();
      component.changed.subscribe(changedSpy);

      component["betrokkeneSelectControl"].setValue(
        ZoekVeld.ZAAK_BETROKKENE_BESLISSER,
      );
      component["roltypeChanged"]();

      expect(changedSpy).not.toHaveBeenCalled();
    });
  });

  describe("openDialog", () => {
    it("sets dialogOpen to true while the dialog is open", () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();

      const afterClosed$ = new Subject<{ identificatie: string }>();
      jest.spyOn(dialog, "open").mockReturnValue({
        afterClosed: () => afterClosed$.asObservable(),
      } as unknown as MatDialogRef<KlantZoekDialog>);

      component["openDialog"]();

      expect(component["dialogOpen"]).toBe(true);
    });

    it("sets dialogOpen to false and updates klantIdControl after dialog closes", fakeAsync(() => {
      component.zoekparameters = makeZoekParams({ ZAAK_INITIATOR: "" });
      fixture.detectChanges();

      const afterClosed$ = new Subject<{ identificatie: string }>();
      jest.spyOn(dialog, "open").mockReturnValue({
        afterClosed: () => afterClosed$.asObservable(),
      } as unknown as MatDialogRef<KlantZoekDialog>);

      component["openDialog"]();
      afterClosed$.next({ identificatie: "NEW_ID" });
      afterClosed$.complete();
      tick();
      fixture.detectChanges();

      expect(component["dialogOpen"]).toBe(false);
      expect(component["klantIdControl"].value).toBe("NEW_ID");
      expect(component.zoekparameters.zoeken!["ZAAK_INITIATOR"]).toBe("NEW_ID");
    }));

    it("emits changed after dialog closes", fakeAsync(() => {
      component.zoekparameters = makeZoekParams({ ZAAK_INITIATOR: "" });
      fixture.detectChanges();
      const changedSpy = jest.fn();
      component.changed.subscribe(changedSpy);

      const afterClosed$ = new Subject<{ identificatie: string }>();
      jest.spyOn(dialog, "open").mockReturnValue({
        afterClosed: () => afterClosed$.asObservable(),
      } as unknown as MatDialogRef<KlantZoekDialog>);

      component["openDialog"]();
      afterClosed$.next({ identificatie: "X" });
      afterClosed$.complete();
      tick();

      expect(changedSpy).toHaveBeenCalledTimes(1);
    }));
  });

  describe("template rendering", () => {
    it("renders a mat-select with all 8 betrokkene roltype options", async () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();

      const select = await loader.getHarness(MatSelectHarness);
      await select.open();
      const options = await select.getOptions();

      expect(options).toHaveLength(8);
    });

    it("renders a text input bound to klantIdControl", async () => {
      component.zoekparameters = makeZoekParams({ ZAAK_INITIATOR: "test123" });
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getValue()).toBe("test123");
    });

    it("adds active class to the icon when dialogOpen is true", () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();
      component["dialogOpen"] = true;
      fixture.detectChanges();

      const icon: HTMLElement = fixture.nativeElement.querySelector("mat-icon");
      expect(icon.classList).toContain("active");
    });

    it("does not have active class on the icon when dialogOpen is false", () => {
      component.zoekparameters = makeZoekParams({});
      fixture.detectChanges();

      const icon: HTMLElement = fixture.nativeElement.querySelector("mat-icon");
      expect(icon.classList).not.toContain("active");
    });

    it("calls idChanged when blur event fires on the input", async () => {
      component.zoekparameters = makeZoekParams({ ZAAK_INITIATOR: "old" });
      fixture.detectChanges();
      const idChangedSpy = jest.spyOn(component as never, "idChanged");

      const input = await loader.getHarness(MatInputHarness);
      await input.blur();

      expect(idChangedSpy).toHaveBeenCalledTimes(1);
    });
  });
});
