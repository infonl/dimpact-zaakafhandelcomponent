/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { Subject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZoekVeld } from "../../../model/zoek-veld";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";
import { ZaakBetrokkeneFilterComponent } from "./zaak-betrokkene-filter.component";

type ZoekParameters = GeneratedType<"RestZoekParameters">;
type Klant = GeneratedType<"RestBedrijf" | "RestPersoon">;

const makeZoekParameters = (
  zoeken: ZoekParameters["zoeken"] = {},
): ZoekParameters => fromPartial<ZoekParameters>({ page: 0, rows: 25, zoeken });

describe(ZaakBetrokkeneFilterComponent.name, () => {
  let fixture: ComponentFixture<ZaakBetrokkeneFilterComponent>;
  let changed: jest.Mock<void, [void]>;
  let dialogOpen: jest.SpyInstance;
  let dialogClosed: Subject<Klant>;

  const user = userEvent.setup({ delay: null });

  async function setup(zoekparameters: ZoekParameters) {
    changed = jest.fn();
    const rendered = await render(ZaakBetrokkeneFilterComponent, {
      inputs: { zoekparameters },
      on: { changed },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
    });
    fixture = rendered.fixture;
    await fixture.whenStable();
    fixture.detectChanges();

    dialogClosed = new Subject<Klant>();
    dialogOpen = jest
      .spyOn(fixture.debugElement.injector.get(MatDialog), "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<KlantZoekDialog>>({
          afterClosed: () => dialogClosed.asObservable(),
        }),
      );
  }

  function idField() {
    return screen.getByPlaceholderText("bsn.vestigingsnummer.rsin");
  }

  function roltypeSelect() {
    return screen.getByRole("combobox");
  }

  async function selectRoltype(name: string) {
    await user.click(roltypeSelect());
    await user.click(screen.getByRole("option", { name }));
  }

  async function enterId(id: string) {
    await user.clear(idField());
    await user.type(idField(), id);
    await user.tab();
  }

  async function closeDialogWith(klant: Klant) {
    dialogClosed.next(klant);
    dialogClosed.complete();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  describe("showing the zoekparameters", () => {
    it("offers every betrokkene roltype", async () => {
      await setup(makeZoekParameters());

      await user.click(roltypeSelect());

      expect(
        screen
          .getAllByRole("option")
          .map((option) => option.textContent?.trim()),
      ).toEqual([
        "betrokkeneRoltype.-alle-",
        "betrokkeneRoltype.INITIATOR",
        "betrokkeneRoltype.MEDE_INITIATOR",
        "betrokkeneRoltype.BELANGHEBBENDE",
        "betrokkeneRoltype.BESLISSER",
        "betrokkeneRoltype.ADVISEUR",
        "betrokkeneRoltype.KLANTCONTACTER",
        "betrokkeneRoltype.ZAAKCOORDINATOR",
      ]);
    });

    it("starts with the initiator roltype selected, even when the zoekparameters hold another roltype", async () => {
      await setup(
        makeZoekParameters({ [ZoekVeld.ZAAK_BETROKKENE_ADVISEUR]: "fakeId" }),
      );

      expect(roltypeSelect()).toHaveTextContent("betrokkeneRoltype.INITIATOR");
    });

    it.each([
      ZoekVeld.ZAAK_BETROKKENEN,
      ZoekVeld.ZAAK_INITIATOR,
      ZoekVeld.ZAAK_BETROKKENE_BELANGHEBBENDE,
      ZoekVeld.ZAAK_BETROKKENE_ADVISEUR,
      ZoekVeld.ZAAK_BETROKKENE_BESLISSER,
      ZoekVeld.ZAAK_BETROKKENE_ZAAKCOORDINATOR,
      ZoekVeld.ZAAK_BETROKKENE_MEDE_INITIATOR,
    ])("shows the id searched for as %s", async (zoekVeld) => {
      await setup(makeZoekParameters({ [zoekVeld]: "fakeId" }));

      expect(idField()).toHaveValue("fakeId");
    });

    it("does not show an id searched for as klantcontacter", async () => {
      await setup(
        makeZoekParameters({
          [ZoekVeld.ZAAK_BETROKKENE_KLANTCONTACTER]: "fakeId",
        }),
      );

      expect(idField()).toHaveValue("");
    });

    it("shows the id of all betrokkenen before that of the initiator", async () => {
      await setup(
        makeZoekParameters({
          [ZoekVeld.ZAAK_INITIATOR]: "fakeInitiatorId",
          [ZoekVeld.ZAAK_BETROKKENEN]: "fakeBetrokkeneId",
        }),
      );

      expect(idField()).toHaveValue("fakeBetrokkeneId");
    });

    it("shows an empty id when nothing is searched for", async () => {
      await setup(makeZoekParameters());

      expect(idField()).toHaveValue("");
    });
  });

  describe("entering an id", () => {
    it("writes the id into the zoekparameters of the parent and emits changed when the field loses focus", async () => {
      const zoekparameters = makeZoekParameters({
        [ZoekVeld.ZAAK_INITIATOR]: "fakeOldId",
      });
      await setup(zoekparameters);

      await enterId("fakeNewId");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_INITIATOR]: "fakeNewId",
      });
      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith(undefined);
    });

    it("writes the id into the zoekparameters of the parent and emits changed on enter", async () => {
      const zoekparameters = makeZoekParameters();
      await setup(zoekparameters);

      await user.type(idField(), "fakeNewId{Enter}");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_INITIATOR]: "fakeNewId",
      });
      expect(changed).toHaveBeenCalledTimes(1);
    });

    it("writes the id under the roltype that the zoekparameters hold", async () => {
      const zoekparameters = makeZoekParameters({
        [ZoekVeld.ZAAK_BETROKKENE_BESLISSER]: "fakeOldId",
      });
      await setup(zoekparameters);

      await enterId("fakeNewId");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_BETROKKENE_BESLISSER]: "fakeNewId",
      });
    });

    it("does not emit changed when the id is unchanged", async () => {
      await setup(makeZoekParameters({ [ZoekVeld.ZAAK_INITIATOR]: "fakeId" }));

      await user.click(idField());
      await user.tab();

      expect(changed).not.toHaveBeenCalled();
    });

    it("ignores the id when the zoekparameters have no zoeken", async () => {
      const zoekparameters = makeZoekParameters(null);
      await setup(zoekparameters);

      await enterId("fakeNewId");

      expect(zoekparameters.zoeken).toBeNull();
      expect(changed).not.toHaveBeenCalled();
    });
  });

  describe("changing the roltype", () => {
    it("moves the id to the selected roltype and emits changed", async () => {
      const zoekparameters = makeZoekParameters({
        [ZoekVeld.ZAAK_INITIATOR]: "fakeId",
      });
      await setup(zoekparameters);

      await selectRoltype("betrokkeneRoltype.ADVISEUR");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_BETROKKENE_ADVISEUR]: "fakeId",
      });
      expect(changed).toHaveBeenCalledTimes(1);
    });

    it("moves an empty id to the selected roltype without emitting changed", async () => {
      const zoekparameters = makeZoekParameters();
      await setup(zoekparameters);

      await selectRoltype("betrokkeneRoltype.BESLISSER");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_BETROKKENE_BESLISSER]: "",
      });
      expect(changed).not.toHaveBeenCalled();
    });

    it("writes an id entered afterwards under the selected roltype", async () => {
      const zoekparameters = makeZoekParameters();
      await setup(zoekparameters);

      await selectRoltype("betrokkeneRoltype.ZAAKCOORDINATOR");
      await enterId("fakeId");

      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_BETROKKENE_ZAAKCOORDINATOR]: "fakeId",
      });
    });

    it("ignores the roltype when the zoekparameters have no zoeken", async () => {
      const zoekparameters = makeZoekParameters(null);
      await setup(zoekparameters);

      await selectRoltype("betrokkeneRoltype.ADVISEUR");

      expect(zoekparameters.zoeken).toBeNull();
      expect(changed).not.toHaveBeenCalled();
    });
  });

  describe("searching for a klant", () => {
    it("opens the klant search dialog", async () => {
      await setup(makeZoekParameters());

      await user.click(screen.getByText("person"));

      expect(dialogOpen).toHaveBeenCalledWith(KlantZoekDialog, {
        minWidth: "750px",
        backdropClass: "noColor",
      });
    });

    it("marks the search icon active while the dialog is open", async () => {
      await setup(makeZoekParameters());

      await user.click(screen.getByText("person"));

      expect(screen.getByText("person")).toHaveClass("active");
    });

    it("no longer marks the search icon active after the dialog closes", async () => {
      await setup(makeZoekParameters());
      await user.click(screen.getByText("person"));

      await closeDialogWith(fromPartial<Klant>({ bsn: "fakeBsn" }));

      expect(screen.getByText("person")).not.toHaveClass("active");
    });

    it("writes the id of the chosen klant into the zoekparameters of the parent and emits changed", async () => {
      const zoekparameters = makeZoekParameters({
        [ZoekVeld.ZAAK_BETROKKENE_ADVISEUR]: "fakeOldId",
      });
      await setup(zoekparameters);
      await user.click(screen.getByText("person"));

      await closeDialogWith(fromPartial<Klant>({ bsn: "fakeBsn" }));

      expect(idField()).toHaveValue("fakeBsn");
      expect(zoekparameters.zoeken).toEqual({
        [ZoekVeld.ZAAK_BETROKKENE_ADVISEUR]: "fakeBsn",
      });
      expect(changed).toHaveBeenCalledTimes(1);
    });

    it.each([
      [
        "the vestigingsnummer",
        { vestigingsnummer: "fakeVestigingsnummer", kvkNummer: "fakeKvk" },
        "fakeVestigingsnummer",
      ],
      [
        "the kvk-nummer without a vestigingsnummer",
        { vestigingsnummer: null, kvkNummer: "fakeKvk" },
        "fakeKvk",
      ],
      ["the bsn of a persoon", { bsn: "fakeBsn" }, "fakeBsn"],
      [
        "an empty id without any identification",
        { vestigingsnummer: null, kvkNummer: null },
        "",
      ],
    ])(
      "uses %s as the id of the chosen klant",
      async (_description, klant, id) => {
        await setup(
          makeZoekParameters({ [ZoekVeld.ZAAK_INITIATOR]: "fakeOldId" }),
        );
        await user.click(screen.getByText("person"));

        await closeDialogWith(fromPartial<Klant>(klant));

        expect(idField()).toHaveValue(id);
      },
    );

    it("emits changed after the dialog closes even when the zoekparameters have no zoeken", async () => {
      const zoekparameters = makeZoekParameters(null);
      await setup(zoekparameters);
      await user.click(screen.getByText("person"));

      await closeDialogWith(fromPartial<Klant>({ bsn: "fakeBsn" }));

      expect(zoekparameters.zoeken).toBeNull();
      expect(idField()).toHaveValue("");
      expect(changed).toHaveBeenCalledTimes(1);
    });
  });
});
