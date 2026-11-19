/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { addDays, PickedDate, toApiDate, today } from "../../date-time";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakBrondatumZettenDialogComponent } from "./zaak-brondatum-zetten-dialog.component";

const zaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "fakeZaakUuid",
  zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
    uuid: "fakeZaaktypeUuid",
    omschrijving: "fakeZaaktypeOmschrijving",
  }),
  resultaat: null,
});

const planItem = fromPartial<GeneratedType<"RestPlanItem">>({
  id: "fakePlanItemId",
  userEventListenerActie: "BRONDATUM_ZETTEN",
});

describe(ZaakBrondatumZettenDialogComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<ZaakBrondatumZettenDialogComponent>;

  const user = userEvent.setup();

  async function setup({
    zaakToHandle = zaak,
    planItemToHandle,
  }: {
    zaakToHandle?: GeneratedType<"RestZaak">;
    planItemToHandle?: GeneratedType<"RestPlanItem"> | null;
  } = {}) {
    dialogRef = fromPartial<MatDialogRef<ZaakBrondatumZettenDialogComponent>>({
      close: jest.fn(),
      disableClose: false,
    });

    await render(ZaakBrondatumZettenDialogComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideZonelessChangeDetection(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { zaak: zaakToHandle, planItem: planItemToHandle },
        },
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.zaak.brondatum-zetten" });
  }

  async function fillInBrondatum(date: PickedDate) {
    await user.type(screen.getByLabelText("zaak.brondatum"), toApiDate(date)!);
  }

  it("keeps the submit button disabled while no brondatum has been filled in", async () => {
    await setup({ planItemToHandle: planItem });

    expect(submitButton()).toBeDisabled();
  });

  it("allows submitting a brondatum of today", async () => {
    await setup({ planItemToHandle: planItem });

    await fillInBrondatum(today());

    expect(submitButton()).toBeEnabled();
  });

  it("refuses a brondatum before today when the zaak has no einddatum", async () => {
    await setup({ planItemToHandle: planItem });

    await fillInBrondatum(addDays(today(), -1));

    expect(submitButton()).toBeDisabled();
  });

  describe("given a zaak that was closed in the past", () => {
    const einddatum = addDays(today(), -10);
    const closedZaak = fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      einddatum: toApiDate(einddatum),
    });

    it("allows a brondatum in the past on the einddatum of the zaak", async () => {
      await setup({ zaakToHandle: closedZaak, planItemToHandle: planItem });

      await fillInBrondatum(einddatum);

      expect(submitButton()).toBeEnabled();
    });

    it("allows a brondatum in the past after the einddatum of the zaak", async () => {
      await setup({ zaakToHandle: closedZaak, planItemToHandle: planItem });

      await fillInBrondatum(addDays(einddatum, 1));

      expect(submitButton()).toBeEnabled();
    });

    it("refuses a brondatum before the einddatum of the zaak", async () => {
      await setup({ zaakToHandle: closedZaak, planItemToHandle: planItem });

      await fillInBrondatum(addDays(einddatum, -1));
      await user.tab();

      expect(submitButton()).toBeDisabled();
      expect(
        await screen.findByText(
          "msg.error.date.invalid.datum.brondatum-voor-einddatum",
        ),
      ).toBeVisible();
    });
  });

  it("closes the dialog when the cancel button is clicked", async () => {
    await setup({ planItemToHandle: planItem });

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it("afhandelt the plan item with the brondatum, as a date without a time, when the dialog has one", async () => {
    await setup({ planItemToHandle: planItem });
    const brondatum = addDays(today(), 1);

    await fillInBrondatum(brondatum);
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(
      "/rest/planitems/doUserEventListenerPlanItem",
    );
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual(
      expect.objectContaining({
        actie: "BRONDATUM_ZETTEN",
        planItemInstanceId: "fakePlanItemId",
        zaakUuid: "fakeZaakUuid",
        brondatum: toApiDate(brondatum),
      }),
    );
    request.flush({});
  });

  it("sets the brondatum on the zaak, as a date without a time, when the dialog has no plan item", async () => {
    await setup({ planItemToHandle: null });
    const brondatum = addDays(today(), 1);

    await fillInBrondatum(brondatum);
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(
      "/rest/zaken/zaak/fakeZaakUuid/brondatum",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual(
      expect.objectContaining({
        brondatum: toApiDate(brondatum),
      }),
    );
    request.flush({});
  });

  it("closes the dialog with true once the brondatum has been set", async () => {
    await setup({ planItemToHandle: null });

    await fillInBrondatum(addDays(today(), 1));
    await user.click(submitButton());
    await sleep();

    httpTestingController
      .expectOne("/rest/zaken/zaak/fakeZaakUuid/brondatum")
      .flush({});
    await sleep();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it("labels the date field with the datumkenmerk of the resultaattype when it is required", async () => {
    await setup({
      zaakToHandle: fromPartial<GeneratedType<"RestZaak">>({
        ...zaak,
        resultaat: fromPartial({
          resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
            isDatumKenmerkVerplicht: true,
            datumKenmerkOmschrijving: "fakeDatumKenmerkOmschrijving",
          }),
        }),
      }),
      planItemToHandle: planItem,
    });

    expect(screen.getByLabelText("fakeDatumKenmerkOmschrijving")).toBeVisible();
  });
});
