export const DOCUMENT_STRING_SPLITTER = ";";

export function mapDocumentenToString(
  documentUUIDs?: (string | null | undefined)[],
): string {
  if (!documentUUIDs || documentUUIDs.length === 0) return "";
  return documentUUIDs.filter(Boolean).join(DOCUMENT_STRING_SPLITTER);
}

export function mapStringToDocumentenStrings(
  documentString?: unknown,
): string[] {
  if (typeof documentString !== "string") return [];
  if (!documentString) return [];
  return documentString.split(DOCUMENT_STRING_SPLITTER);
}
