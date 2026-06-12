/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTabGroupHarness } from "@angular/material/tabs/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelComponent } from "./klant-koppel.component";

@Component({
  selector: "zac-klant-koppel-initiator-persoon",
  template: "",
  standalone: true,
})
class KlantKoppelInitiatorStubComponent {
  @Input() type!: string;
  @Input() zaaktypeUUID?: string | null;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
}

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  template: "",
  standalone: true,
})
class KlantKoppelBetrokkeneStubComponent {
  @Input() type!: string;
  @Input() zaaktypeUUID?: string | null;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
}

const mockSideNav = { close: jest.fn() } as unknown as MatDrawer;

describe(KlantKoppelComponent.name, () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        KlantKoppelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    });
  });

  describe("tab groups", () => {
    let fixture: ComponentFixture<KlantKoppelComponent>;
    let loader: HarnessLoader;

    function createFixture(
      initiator = false,
      allowPersoon = true,
      allowBedrijf = true,
    ) {
      fixture = TestBed.createComponent(KlantKoppelComponent);
      fixture.componentRef.setInput("sideNav", mockSideNav);
      fixture.componentRef.setInput("initiator", initiator);
      fixture.componentRef.setInput("allowPersoon", allowPersoon);
      fixture.componentRef.setInput("allowBedrijf", allowBedrijf);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    }

    beforeEach(async () => {
      await TestBed.overrideComponent(KlantKoppelComponent, {
        set: {
          imports: [
            SharedModule,
            TranslateModule,
            KlantKoppelInitiatorStubComponent,
            KlantKoppelBetrokkeneStubComponent,
          ],
        },
      }).compileComponents();

      createFixture();
    });

    it("should render two tabs when allowPersoon and allowBedrijf are true", async () => {
      const tabGroup = await loader.getHarness(MatTabGroupHarness);
      expect((await tabGroup.getTabs()).length).toBe(2);
    });

    describe("betrokkene tab group (initiator=false)", () => {
      describe("when allowPersoon is true", () => {
        it("should show the persoon tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const tabs = await tabGroup.getTabs();
          expect(tabs.length).toBe(2);
          expect(await tabs[0].getLabel()).toContain("betrokkene.persoon");
        });

        it("should select the persoon tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isSelected()).toBe(true);
        });
      });

      describe("when allowPersoon is false", () => {
        beforeEach(() => createFixture(false, false, true));

        it("should hide the persoon tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const tabs = await tabGroup.getTabs();
          expect(tabs.length).toBe(1);
          expect(await tabs[0].getLabel()).toContain("betrokkene.bedrijf");
        });

        it("should show the bedrijf tab as the only tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [bedrijfTab] = await tabGroup.getTabs();
          expect(await bedrijfTab.isSelected()).toBe(true);
        });
      });
    });

    describe("initiator tab group (initiator=true)", () => {
      beforeEach(() => createFixture(true));

      describe("when allowPersoon is true", () => {
        it("should show the persoon tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const tabs = await tabGroup.getTabs();
          expect(tabs.length).toBe(2);
          expect(await tabs[0].getLabel()).toContain("betrokkene.persoon");
        });

        it("should select the persoon tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isSelected()).toBe(true);
        });
      });

      describe("when allowPersoon is false", () => {
        beforeEach(() => createFixture(true, false, true));

        it("should hide the persoon tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const tabs = await tabGroup.getTabs();
          expect(tabs.length).toBe(1);
          expect(await tabs[0].getLabel()).toContain("betrokkene.bedrijf");
        });

        it("should show the bedrijf tab as the only tab", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [bedrijfTab] = await tabGroup.getTabs();
          expect(await bedrijfTab.isSelected()).toBe(true);
        });
      });
    });
  });
});
