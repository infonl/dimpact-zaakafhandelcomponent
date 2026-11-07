/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakHistorieComponent } from "./zaak-historie.component";

const zaak = fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid" });

const historieRegel = (fields: Partial<GeneratedType<"HistoryLine">> = {}) =>
  fromPartial<GeneratedType<"HistoryLine">>({
    attribuutLabel: "fakeAttribuutLabel",
    oudeWaarde: "fakeOudeWaarde",
    nieuweWaarde: "fakeNieuweWaarde",
    toelichting: "fakeToelichting",
    datumTijd: "2026-01-01T00:00:00Z",
    door: "fakeGebruiker",
    ...fields,
  });

describe(ZaakHistorieComponent.name, () => {
  let fixture: ComponentFixture<ZaakHistorieComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  async function setup(zaakToShow: GeneratedType<"RestZaak"> = zaak) {
    const rendered = await render(ZaakHistorieComponent, {
      inputs: { zaak: zaakToShow },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    return rendered;
  }

  async function respondWith(
    historie: GeneratedType<"HistoryLine">[],
    uuid = zaak.uuid,
  ) {
    httpTestingController
      .expectOne(`/rest/zaken/zaak/${uuid}/historie`)
      .flush(historie);
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  async function respondWithError(uuid = zaak.uuid) {
    httpTestingController
      .expectOne(`/rest/zaken/zaak/${uuid}/historie`)
      .flush(null, { status: 404, statusText: "Not Found" });
    await sleep();
    fixture.detectChanges();
  }

  async function refetch() {
    await testQueryClient.invalidateQueries();
  }

  function gebruikersInRowOrder() {
    return screen
      .getAllByRole("row")
      .slice(1)
      .map((row) => within(row).getAllByRole("cell")[1]?.textContent?.trim());
  }

  it("shows a loading message until the historie arrives", async () => {
    await setup();

    expect(screen.getByText("msg.loading")).toBeVisible();

    await respondWith([]);
  });

  it("shows the no-data message when the zaak has no historie", async () => {
    await setup();

    await respondWith([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("renders a row per historie regel", async () => {
    await setup();

    await respondWith([
      historieRegel({ toelichting: "eersteToelichting" }),
      historieRegel({ toelichting: "tweedeToelichting" }),
    ]);

    expect(
      screen.getByRole("row", { name: /eersteToelichting/ }),
    ).toBeVisible();
    expect(
      screen.getByRole("row", { name: /tweedeToelichting/ }),
    ).toBeVisible();
  });

  it("asks for the historie of the zaak it is given", async () => {
    await setup(
      fromPartial<GeneratedType<"RestZaak">>({ uuid: "andereZaakUuid" }),
    );

    await respondWith(
      [historieRegel({ toelichting: "andereToelichting" })],
      "andereZaakUuid",
    );

    expect(
      screen.getByRole("row", { name: /andereToelichting/ }),
    ).toBeVisible();
  });

  describe("when the historie is read again in the background", () => {
    it("shows the refetch hint and keeps the historie when the read fails", async () => {
      await setup();
      await respondWith([historieRegel({ toelichting: "oudeToelichting" })]);

      void refetch();
      await respondWithError();

      expect(screen.getByRole("status")).toHaveTextContent(
        "msg.verversen.mislukt",
      );
      expect(
        screen.getByRole("row", { name: /oudeToelichting/ }),
      ).toBeVisible();
    });

    it("hides the refetch hint again after a read that succeeds", async () => {
      await setup();
      await respondWith([historieRegel({ toelichting: "oudeToelichting" })]);
      void refetch();
      await respondWithError();

      void refetch();
      await respondWith([historieRegel({ toelichting: "nieuweToelichting" })]);

      expect(
        screen.queryByText("msg.verversen.mislukt"),
      ).not.toBeInTheDocument();
      expect(
        screen.getByRole("row", { name: /nieuweToelichting/ }),
      ).toBeVisible();
    });
  });

  it("does not show the refetch hint when the first read fails, because the error dialog reports that", async () => {
    await setup();

    await respondWithError();

    expect(screen.queryByText("msg.verversen.mislukt")).not.toBeInTheDocument();
  });

  it("sorts the rows on the moment of the change", async () => {
    await setup();
    await respondWith([
      historieRegel({ door: "Aap", datumTijd: "2026-03-01T00:00:00Z" }),
      historieRegel({ door: "Noot", datumTijd: "2026-01-01T00:00:00Z" }),
    ]);

    await user.click(screen.getByRole("columnheader", { name: "datum" }));
    fixture.detectChanges();

    expect(gebruikersInRowOrder()).toEqual(["Noot", "Aap"]);
  });

  it("sorts the rows on the user that made the change", async () => {
    await setup();
    await respondWith([
      historieRegel({ door: "Aap", datumTijd: "2026-03-01T00:00:00Z" }),
      historieRegel({ door: "Noot", datumTijd: "2026-01-01T00:00:00Z" }),
    ]);

    await user.click(screen.getByRole("columnheader", { name: "door" }));
    fixture.detectChanges();
    expect(gebruikersInRowOrder()).toEqual(["Aap", "Noot"]);

    await user.click(screen.getByRole("columnheader", { name: "door" }));
    fixture.detectChanges();
    expect(gebruikersInRowOrder()).toEqual(["Noot", "Aap"]);
  });

  it("sorts columns without a dedicated accessor on their own value", async () => {
    await setup();
    await respondWith([historieRegel({ toelichting: "fakeToelichting" })]);

    expect(
      fixture.componentInstance["historie"].sortingDataAccessor(
        historieRegel({ toelichting: "fakeToelichting" }),
        "toelichting",
      ),
    ).toBe("fakeToelichting");
  });
});
