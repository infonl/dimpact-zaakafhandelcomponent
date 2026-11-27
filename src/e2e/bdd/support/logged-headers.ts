/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const LOGGED_SECRET_HEADER =
  /(- (?:authorization|cookie|set-cookie):)[^\r\n]*/gi;

function withoutSecretHeaders(text: string) {
  return text.replace(LOGGED_SECRET_HEADER, "$1 REDACTED");
}

/**
 * The call, without the values of the secret headers that Playwright adds to the error of a call that gets no answer.
 * The log of GitHub Actions and the nightly report are public, and they show that error.
 */
export async function withoutLoggedHeaders<Result>(call: Promise<Result>) {
  try {
    return await call;
  } catch (error) {
    if (error instanceof Error) {
      const { stack } = error;
      error.message = withoutSecretHeaders(error.message);
      error.stack = stack && withoutSecretHeaders(stack);
    }
    throw error;
  }
}
