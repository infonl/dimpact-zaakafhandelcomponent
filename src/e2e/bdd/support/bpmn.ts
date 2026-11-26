/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import path from "node:path";
import type { ZacApi } from "./zac-api";

export type BpmnProcess = {
  /** The id of the process in its BPMN file. */
  idInFile: string;
  /** The id that the target deploys the process with. */
  key: string;
  bpmnFile: string;
  formFiles: string[];
};

async function contentOf(file: string) {
  return {
    filename: path.basename(file),
    content: await readFile(file, "utf8"),
  };
}

/**
 * Flowable ignores a deployment that equals the last one with the same filename, also when another file has deployed a
 * newer version of the process since. A new filename makes it deploy again.
 */
function filenameOfNewDeployment(filename: string) {
  const { name, ext } = path.parse(filename);
  return `${name}-${randomUUID()}${ext}`;
}

/** Deploys the process and its forms as a new version, also when ZAC has the same version already. */
export async function deployProcess(
  api: ZacApi,
  { idInFile, key, bpmnFile, formFiles }: BpmnProcess,
) {
  const { filename, content } = await contentOf(bpmnFile);
  await api.POST("/rest/bpmn-process-definitions", {
    body: {
      filename: filenameOfNewDeployment(filename),
      content: content.replaceAll(idInFile, key),
    },
  });
  for (const formFile of formFiles) {
    await api.POST("/rest/bpmn-process-definitions/{key}/forms", {
      params: { path: { key } },
      body: await contentOf(formFile),
    });
  }
}
