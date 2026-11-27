/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { writeFileSync } from "node:fs";
import path from "node:path";
import { deployProcess } from "./bpmn";
import type { ZacApi } from "./zac-api";

type Post = { url: string; body: { filename: string; content: string } };

test("given a process that is deployed already, when it is deployed again, then it goes to ZAC under a new filename, so that Flowable does not ignore it", async ({}, testInfo) => {
  const bpmnFile = testInfo.outputPath("process.bpmn");
  const formFile = testInfo.outputPath("form.json");
  writeFileSync(bpmnFile, '<process id="idInFile"/>');
  writeFileSync(formFile, "{}");
  const posts: Post[] = [];
  const api = {
    POST: (url: string, { body }: { body: Post["body"] }) => {
      posts.push({ url, body });
      return Promise.resolve({});
    },
  } as unknown as ZacApi;
  const process = {
    idInFile: "idInFile",
    key: "deployedKey",
    bpmnFile,
    formFiles: [formFile],
  };

  await deployProcess(api, process);
  await deployProcess(api, process);

  const [first, firstForm, second, secondForm] = posts.map(({ body }) => body);
  expect(second.filename).not.toBe(first.filename);
  expect(path.extname(second.filename)).toBe(".bpmn");
  expect(second.content).toBe('<process id="deployedKey"/>');
  expect(secondForm).toEqual(firstForm);
});
