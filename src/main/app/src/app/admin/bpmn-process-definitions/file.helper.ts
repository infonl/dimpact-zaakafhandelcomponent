/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export function readFileContent(file: File): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = (error) => reject(error);
    reader.readAsText(file);
  });
}

export function extractBpmnProcessKey(content: string): string | null {
  const doc = new DOMParser().parseFromString(content, "application/xml");
  return doc.querySelector("process")?.getAttribute("id") ?? null;
}
