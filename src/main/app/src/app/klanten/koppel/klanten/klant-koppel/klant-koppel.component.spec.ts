/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { SharedModule } from "src/app/shared/shared.module";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelComponent } from "./klant-koppel.component";

const fakeKlantGegevens = new KlantGegevens(
  fromPartial<GeneratedType<"RestPersoon">>({ bsn: "999990408" }),
);

@Component({
  selector: "zac-klant-koppel-initiator-persoon",
  template: `
    <p>initiator type: {{ type() }}, zaaktypeUUID: {{ zaaktypeUUID() }}</p>
    <button type="button" (click)="klantGegevens.emit(fakeKlantGegevens)">
      select fake initiator
    </button>
  `,
  standalone: true,
})
class KlantKoppelInitiatorStubComponent {
  readonly type = input<string>();
  readonly zaaktypeUUID = input<string | null>();
  readonly klantGegevens = output<KlantGegevens>();
  protected readonly fakeKlantGegevens = fakeKlantGegevens;
}

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  template: `
    <p>betrokkene type: {{ type() }}, zaaktypeUUID: {{ zaaktypeUUID() }}</p>
    <button type="button" (click)="klantGegevens.emit(fakeKlantGegevens)">
      select fake betrokkene
    </button>
  `,
  standalone: true,
})
class KlantKoppelBetrokkeneStubComponent {
  readonly type = input<string>();
  readonly zaaktypeUUID = input<string | null>();
  readonly klantGegevens = output<KlantGegevens>();
  protected readonly fakeKlantGegevens = fakeKlantGegevens;
}

describe(KlantKoppelComponent.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<KlantKoppelComponent>;
  let sideNav: MatDrawer;
  let onKlantGegevens: jest.Mock<void, [KlantGegevens]>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantKoppelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    })
      .overrideComponent(KlantKoppelComponent, {
        set: {
          imports: [
            SharedModule,
            TranslateModule,
            KlantKoppelInitiatorStubComponent,
            KlantKoppelBetrokkeneStubComponent,
          ],
        },
      })
      .compileComponents();

    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    fixture = TestBed.createComponent(KlantKoppelComponent);
    onKlantGegevens = jest.fn();
    fixture.componentInstance.klantGegevens.subscribe(onKlantGegevens);
  });

  function initialiseWith(inputs: {
    initiator?: boolean;
    zaaktypeUUID?: string | null;
    allowPersoon?: boolean;
    allowBedrijf?: boolean;
  }) {
    fixture.componentRef.setInput("sideNav", sideNav);
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    fixture.detectChanges();
  }

  function heading() {
    return screen.getByRole("heading");
  }

  function expectTabs(...names: string[]) {
    const tabs = screen.queryAllByRole("tab");
    expect(tabs).toHaveLength(names.length);
    names.forEach((name, index) =>
      expect(tabs[index]).toHaveAccessibleName(name),
    );
  }

  async function openTab(name: "betrokkene.persoon" | "betrokkene.bedrijf") {
    await user.click(screen.getByRole("tab", { name: new RegExp(name) }));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  describe("the heading", () => {
    it("asks to link a betrokkene when no initiator input is given", () => {
      initialiseWith({ allowPersoon: true, allowBedrijf: true });

      expect(heading()).toHaveTextContent("actie.betrokkene.koppelen");
    });

    it("asks to link the initiator when initiator is set", () => {
      initialiseWith({ initiator: true, allowPersoon: true });

      expect(heading()).toHaveTextContent("actie.initiator.koppelen");
    });

    it("switches to linking the initiator when initiator changes to true", () => {
      initialiseWith({ initiator: false, allowPersoon: true });

      fixture.componentRef.setInput("initiator", true);
      fixture.detectChanges();

      expect(heading()).toHaveTextContent("actie.initiator.koppelen");
    });
  });

  describe("closing", () => {
    beforeEach(() => initialiseWith({ allowPersoon: true }));

    it("closes the side navigation with the close button in the heading", async () => {
      await user.click(within(heading()).getByRole("button"));

      expect(sideNav.close).toHaveBeenCalled();
    });

    it("closes the side navigation with the cancel button", async () => {
      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(sideNav.close).toHaveBeenCalled();
    });
  });

  describe.each([
    { initiator: false, description: "a betrokkene" },
    { initiator: true, description: "an initiator" },
  ])("the tabs to link $description", ({ initiator }) => {
    it("shows a persoon and a bedrijf tab when both are allowed, with the persoon tab selected", () => {
      initialiseWith({ initiator, allowPersoon: true, allowBedrijf: true });

      expectTabs("betrokkene.persoon", "betrokkene.bedrijf");
      expect(
        screen.getByRole("tab", { name: /betrokkene.persoon/ }),
      ).toHaveAttribute("aria-selected", "true");
    });

    it("shows only the bedrijf tab, selected, when only a bedrijf is allowed", () => {
      initialiseWith({ initiator, allowPersoon: false, allowBedrijf: true });

      expectTabs("betrokkene.bedrijf");
      expect(
        screen.getByRole("tab", { name: /betrokkene.bedrijf/ }),
      ).toHaveAttribute("aria-selected", "true");
    });

    it("shows only the persoon tab when only a persoon is allowed", () => {
      initialiseWith({ initiator, allowPersoon: true, allowBedrijf: false });

      expectTabs("betrokkene.persoon");
    });

    it("shows no tabs when allowPersoon and allowBedrijf are not given", () => {
      initialiseWith({ initiator });

      expectTabs();
    });

    it("adds the bedrijf tab when allowBedrijf changes to true", () => {
      initialiseWith({ initiator, allowPersoon: true, allowBedrijf: false });

      fixture.componentRef.setInput("allowBedrijf", true);
      fixture.detectChanges();

      expectTabs("betrokkene.persoon", "betrokkene.bedrijf");
    });
  });

  describe("linking an initiator", () => {
    beforeEach(() =>
      initialiseWith({
        initiator: true,
        zaaktypeUUID: "fakeZaaktypeUuid",
        allowPersoon: true,
        allowBedrijf: true,
      }),
    );

    it("searches for a persoon initiator of the zaaktype in the persoon tab", () => {
      expect(
        screen.getByText(
          "initiator type: persoon, zaaktypeUUID: fakeZaaktypeUuid",
        ),
      ).toBeInTheDocument();
      expect(screen.queryByText(/^betrokkene type/)).not.toBeInTheDocument();
    });

    it("searches for a bedrijf initiator without a zaaktype in the bedrijf tab", async () => {
      await openTab("betrokkene.bedrijf");

      expect(
        screen.getByText("initiator type: bedrijf, zaaktypeUUID:"),
      ).toBeInTheDocument();
    });

    it("passes the new zaaktypeUUID to the persoon initiator search when the zaaktypeUUID changes", () => {
      fixture.componentRef.setInput("zaaktypeUUID", "fakeOtherZaaktypeUuid");
      fixture.detectChanges();

      expect(
        screen.getByText(
          "initiator type: persoon, zaaktypeUUID: fakeOtherZaaktypeUuid",
        ),
      ).toBeInTheDocument();
    });

    it("emits the klantGegevens of the selected initiator", async () => {
      await user.click(
        screen.getByRole("button", { name: "select fake initiator" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledWith(fakeKlantGegevens);
    });
  });

  describe("linking a betrokkene", () => {
    beforeEach(() =>
      initialiseWith({
        initiator: false,
        zaaktypeUUID: "fakeZaaktypeUuid",
        allowPersoon: true,
        allowBedrijf: true,
      }),
    );

    it("searches for a persoon betrokkene of the zaaktype in the persoon tab", () => {
      expect(
        screen.getByText(
          "betrokkene type: persoon, zaaktypeUUID: fakeZaaktypeUuid",
        ),
      ).toBeInTheDocument();
      expect(screen.queryByText(/^initiator type/)).not.toBeInTheDocument();
    });

    it("searches for a bedrijf betrokkene of the zaaktype in the bedrijf tab", async () => {
      await openTab("betrokkene.bedrijf");

      expect(
        screen.getByText(
          "betrokkene type: bedrijf, zaaktypeUUID: fakeZaaktypeUuid",
        ),
      ).toBeInTheDocument();
    });

    it("passes the new zaaktypeUUID to the persoon betrokkene search when the zaaktypeUUID changes", () => {
      fixture.componentRef.setInput("zaaktypeUUID", "fakeOtherZaaktypeUuid");
      fixture.detectChanges();

      expect(
        screen.getByText(
          "betrokkene type: persoon, zaaktypeUUID: fakeOtherZaaktypeUuid",
        ),
      ).toBeInTheDocument();
    });

    it("emits the klantGegevens of the selected betrokkene", async () => {
      await user.click(
        screen.getByRole("button", { name: "select fake betrokkene" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledWith(fakeKlantGegevens);
    });
  });
});
