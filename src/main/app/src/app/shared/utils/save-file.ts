/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

type SaveFilePickerOptions = {
  suggestedName?: string;
  types?: {
    description?: string;
    accept: Record<string, string[]>;
  }[];
};

declare global {
  interface Window {
    // the File System Access API is not part of `lib.dom`, as only Chromium browsers implement it
    showSaveFilePicker?: (
      options?: SaveFilePickerOptions,
    ) => Promise<FileSystemFileHandle>;
  }
}

export function promptForSaveLocation(
  options: SaveFilePickerOptions,
): Promise<FileSystemFileHandle | null> {
  return window.showSaveFilePicker?.(options) ?? Promise.resolve(null);
}

export async function writeFile(
  fileHandle: FileSystemFileHandle,
  content: Blob,
) {
  const writableStream = await fileHandle.createWritable();
  await writableStream.write(content);
  await writableStream.close();
}
