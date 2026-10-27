/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { signal } from "@angular/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { sleep } from "../../../../setupJest";
import {
  RefetchableQuery,
  RefetchErrorHintComponent,
} from "./refetch-error-hint.component";

const HINT_TEXT = "msg.verversen.mislukt";
// the Material tooltip is aria-hidden and describes the icon, which is aria-hidden too
const TOOLTIP_SELECTOR = ".mat-mdc-tooltip-surface";

function fakeQuery(isRefetchError: boolean, error: unknown = null) {
  return {
    isRefetchError: signal(isRefetchError),
    error: signal(error),
  };
}

describe(RefetchErrorHintComponent.name, () => {
  async function setup(
    query: RefetchableQuery | RefetchableQuery[],
    compact = false,
  ) {
    return render(RefetchErrorHintComponent, {
      inputs: { query, compact },
      imports: [NoopAnimationsModule],
      providers: [provideTranslateService()],
    });
  }

  it("shows the hint in a status region when a refetch has failed", async () => {
    await setup(
      fakeQuery(true, new HttpErrorResponse({ status: 500, url: "/rest/x" })),
    );

    expect(screen.getByRole("status")).toHaveTextContent(HINT_TEXT);
  });

  it("keeps the status region empty while the query is fine", async () => {
    await setup(fakeQuery(false));

    expect(screen.getByRole("status")).toBeEmptyDOMElement();
  });

  it("does not show the hint when the session has expired, because the page reloads to the login page", async () => {
    await setup(
      fakeQuery(
        true,
        new HttpErrorResponse({ status: 0, url: "https://zac.example/rest/x" }),
      ),
    );

    expect(screen.queryByText(HINT_TEXT)).not.toBeInTheDocument();
  });

  it("shows the hint for a dropped connection outside the REST API", async () => {
    await setup(
      fakeQuery(
        true,
        new HttpErrorResponse({ status: 0, url: "https://zac.example/x" }),
      ),
    );

    expect(screen.getByText(HINT_TEXT)).toBeVisible();
  });

  it("shows the hint for an error that is not an HTTP response", async () => {
    await setup(fakeQuery(true, "De server heeft code 500 geretourneerd."));

    expect(screen.getByText(HINT_TEXT)).toBeVisible();
  });

  it("shows the hint once when one of several queries failed to refetch", async () => {
    await setup([
      fakeQuery(false),
      fakeQuery(true, new HttpErrorResponse({ status: 404 })),
    ]);

    expect(screen.getAllByText(HINT_TEXT)).toHaveLength(1);
  });

  it("does not show the hint when none of several queries failed to refetch", async () => {
    await setup([fakeQuery(false), fakeQuery(false)]);

    expect(screen.queryByText(HINT_TEXT)).not.toBeInTheDocument();
  });

  it("follows the query: the hint goes away after a successful refetch", async () => {
    const query = fakeQuery(true, new HttpErrorResponse({ status: 404 }));
    const { fixture } = await setup(query);
    expect(screen.getByText(HINT_TEXT)).toBeVisible();

    query.isRefetchError.set(false);
    query.error.set(null);
    fixture.detectChanges();

    expect(screen.queryByText(HINT_TEXT)).not.toBeInTheDocument();
  });

  describe("in its compact form, for a narrow place such as a collapsed side menu", () => {
    it("still announces the message in the status region, for screen readers", async () => {
      await setup(
        fakeQuery(true, new HttpErrorResponse({ status: 404 })),
        true,
      );

      expect(screen.getByRole("status")).toHaveTextContent(HINT_TEXT);
    });

    it("shows the message in a tooltip on the icon", async () => {
      await setup(
        fakeQuery(true, new HttpErrorResponse({ status: 404 })),
        true,
      );

      await userEvent.setup().hover(screen.getByText("sync_problem"));

      expect(
        await screen.findByText(HINT_TEXT, { selector: TOOLTIP_SELECTOR }),
      ).toBeInTheDocument();
    });
  });

  it("has no tooltip in its full form, because the message is already on screen", async () => {
    await setup(fakeQuery(true, new HttpErrorResponse({ status: 404 })));

    await userEvent.setup().hover(screen.getByText("sync_problem"));
    await sleep();

    expect(
      screen.queryByText(HINT_TEXT, { selector: TOOLTIP_SELECTOR }),
    ).not.toBeInTheDocument();
  });
});
