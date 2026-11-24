/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  expect,
  type APIRequestContext,
  type TestInfo,
} from "@playwright/test";
import { z } from "zod";
import { ENV } from "../../config/env";

const MAIL_TIMEOUT_MILLISECONDS = 30_000;

const mailsSchema = z.array(z.object({ subject: z.string() }));

/**
 * The API of the mail server of the target. A target that delivers real mail has none: the report of the scenario then
 * tells that the mail itself was not checked.
 */
export function mailServerOrNote(testInfo: TestInfo) {
  if (!ENV.testDoubles) {
    testInfo.annotations.push({
      type: "not checked",
      description: `The target "${ENV.target}" has no mailbox to read, so nothing proves that the mail arrived.`,
    });
  }
  return ENV.testDoubles?.mail.apiUrl;
}

async function onderwerpenOfMailsTo(
  request: APIRequestContext,
  apiUrl: string,
  ontvanger: string,
) {
  const response = await request.get(
    `${apiUrl}/user/${encodeURIComponent(ontvanger)}/messages/`,
  );
  // GreenMail knows no user until the first mail to the address arrives.
  if (!response.ok()) return [];
  return mailsSchema.parse(await response.json()).map(({ subject }) => subject);
}

/** Waits until the mail server has a mail with the onderwerp, which is unique for the scenario, to the ontvanger. */
export async function untilMailArrives(
  request: APIRequestContext,
  apiUrl: string,
  { ontvanger, onderwerp }: { ontvanger: string; onderwerp: string },
) {
  await expect
    .poll(() => onderwerpenOfMailsTo(request, apiUrl, ontvanger), {
      message: `The mail server has no mail "${onderwerp}" to ${ontvanger}.`,
      timeout: MAIL_TIMEOUT_MILLISECONDS,
    })
    .toContain(onderwerp);
}
