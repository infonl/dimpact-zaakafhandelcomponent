#!/usr/bin/env python3

#
#  SPDX-FileCopyrightText: 2026 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+
#

"""
Formats mixed structured/unstructured log output for human-readable terminal display.

Usage:
    tail -f <logfile> | python3 structured-json-logformat.py
    kubectl logs -f <pod> | python3 structured-json-logformat.py

Handles three types of lines:
  - JSON log entries (WildFly/JBoss structured logging format):
      Rendered as a compact header line (timestamp, level, logger, thread),
      followed by the message. Embedded newlines in message strings (e.g. Java
      object toString() dumps) are expanded into real newlines.
      If an 'exception' field is present, it is rendered as a traditional Java
      stack trace (ExceptionType: message / at Class.method(File.java:N)) with
      the full causedBy chain, instead of as a raw JSON object.
  - JVM GC log lines (e.g. from -Xlog:gc):
      Passed through unchanged.
  - Any other non-JSON lines:
      Passed through unchanged.
"""

import sys
import json

# ANSI colors
RESET  = '\033[0m'
BOLD   = '\033[1m'
DIM    = '\033[2m'
RED    = '\033[31m'
YELLOW = '\033[33m'
GREEN  = '\033[32m'
CYAN   = '\033[36m'

LEVEL_COLORS = {
    'SEVERE':  RED + BOLD,
    'WARNING': YELLOW,
    'INFO':    GREEN,
    'DEBUG':   CYAN,
    'FINE':    DIM,
}

def format_exception(exc, depth=0):
    """Recursively render exception + causedBy chain as a traditional stack trace."""
    prefix = '' if depth == 0 else 'Caused by: '
    ex_type = exc.get('exceptionType', 'Exception')
    message  = exc.get('message', '')
    out = [f'{RED}{prefix}{ex_type}{RESET}: {message}']
    for f in exc.get('frames', []):
        cls     = f.get('class', '?')
        method  = f.get('method', '?')
        line_no = f.get('line')
        loc = f'{cls.split(".")[-1]}.java:{line_no}' if line_no else 'Unknown Source'
        out.append(f'{DIM}    at {cls}.{method}({loc}){RESET}')
    caused_by_exc = (exc.get('causedBy') or {}).get('exception')
    if caused_by_exc:
        out.append('')
        out.extend(format_exception(caused_by_exc, depth + 1))
    return out

def render_json(obj):
    ts     = obj.get('timestamp', '')
    level  = obj.get('level', 'INFO')
    logger = obj.get('loggerName', '')
    msg    = obj.get('message', '')
    thread = obj.get('threadName', '')

    level_color = LEVEL_COLORS.get(level, '')

    # Header line: timestamp  LEVEL  logger  [thread]
    print(f'{DIM}{ts}{RESET}  {level_color}{level:<8}{RESET}  {BOLD}{logger}{RESET}  {DIM}[{thread}]{RESET}')

    # Message — expand embedded \n (e.g. class Zaak { ... } toString dumps)
    if '\n' in msg:
        lines = msg.split('\n')
        print(f'  {lines[0]}')
        for l in lines[1:]:
            print(f'  {l}')
    else:
        print(f'  {msg}')

    # Exception — rendered as a traditional Java stack trace, not JSON
    exc = obj.get('exception')
    if exc:
        print()
        for line in format_exception(exc):
            print(f'  {line}')

    print()
    sys.stdout.flush()

for raw in sys.stdin:
    raw = raw.rstrip('\n')
    try:
        render_json(json.loads(raw))
    except (json.JSONDecodeError, ValueError):
        print(raw)
        sys.stdout.flush()
