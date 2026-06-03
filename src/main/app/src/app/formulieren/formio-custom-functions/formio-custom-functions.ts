/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * All custom function names that can appear in form.io ${...} expressions.
 * Add a new entry here when you add a new function below.
 */
export enum KNOWN_FORMIO_FUNCTIONS {
  GET_DOCUMENT_NAME = "getDocumentName",
}

type EvalContext = Record<string, (...args: unknown[]) => unknown>;

/**
 * Scans a form definition for ${functionName(...)} expressions and builds an
 * evalContext object containing only the functions that are actually used.
 * Form.io merges this object into every component's eval context at render time.
 *
 * To add a new function:
 *   1. Add its name to KNOWN_FORMIO_FUNCTIONS.
 *   2. Add a case for it in the switch block inside buildEvalContext.
 *   3. Implement the function as a private method below.
 */
export class FormioCustomFunctions {
  buildEvalContext(form: unknown): EvalContext {
    const context: EvalContext = {};

    for (const [name, parameter] of this.extractFunctionCalls(form)) {
      console.log("NAAM: ", name, " | PARAMETER: ", parameter);
      switch (name) {
        // Add a case here for each entry in KNOWN_FORMIO_FUNCTIONS:
        //
        // case KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_NAME:
        //   context[name] = this.getDocumentName;
        //   break;
      }
    }

    return context;
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  // Returns a Map of functionName → parameter so both are available in the switch.
  private extractFunctionCalls(value: unknown): Map<string, string> {
    const calls = new Map<string, string>();
    this.collectFunctionCalls(value, calls);
    return calls;
  }

  private collectFunctionCalls(
    value: unknown,
    calls: Map<string, string>,
  ): void {
    if (typeof value === "string") {
      const pattern = /\$\{(\w+)\((\w+)\)/g;
      let match: RegExpExecArray | null;
      while ((match = pattern.exec(value)) !== null) {
        calls.set(match[1], match[2]);
      }
    } else if (Array.isArray(value)) {
      for (const item of value) this.collectFunctionCalls(item, calls);
    } else if (value !== null && typeof value === "object") {
      for (const v of Object.values(value)) this.collectFunctionCalls(v, calls);
    }
  }
}
