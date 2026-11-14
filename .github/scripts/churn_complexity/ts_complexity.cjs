/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Usage: node ts_complexity.cjs <node_modules> <file list> <out.json> <base dir>
// Per function: ESLint `complexity` (the metric) and the function span; per file: the functions the TypeScript
// compiler API sees, to check that ESLint did not lose any.
const path = require('path'); const fs = require('fs');
const [nm, listFile, outFile, baseDir] = process.argv.slice(2);
const req = (m) => require(require.resolve(m, { paths: [nm] }));
const { ESLint } = req('eslint'); const tsParser = req('@typescript-eslint/parser'); const ts = req('typescript');
const astUtils = require(path.join(path.dirname(require.resolve('eslint/package.json', { paths: [nm] })), 'lib/rules/utils/ast-utils.js'));
const spanRule = { meta: { type: 'suggestion', schema: [] }, create(context) {
  const sourceCode = context.sourceCode;
  return { onCodePathEnd(codePath, node) {
    if (codePath.origin !== 'function' && codePath.origin !== 'class-field-initializer' && codePath.origin !== 'class-static-block') return;
    let loc = node.loc;
    if (codePath.origin === 'class-static-block') loc = sourceCode.getFirstToken(node).loc;
    else if (codePath.origin === 'function') loc = astUtils.getFunctionHeadLoc(node, sourceCode);
    const name = codePath.origin === 'function' ? astUtils.getFunctionNameWithKind(node) : codePath.origin;
    context.report({ node, loc, message: JSON.stringify({ origin: codePath.origin, name, start: node.loc.start.line, end: node.loc.end.line }) });
  } };
} };
(async () => {
  const files = fs.readFileSync(listFile, 'utf8').split('\n').filter(Boolean);
  const eslint = new ESLint({ cwd: baseDir, overrideConfigFile: true, ignore: false,
    overrideConfig: [{ files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.mjs', '**/*.cjs', '**/*.jsx'],
      plugins: { cc: { rules: { span: spanRule } } },
      languageOptions: { parser: tsParser, ecmaVersion: 'latest', sourceType: 'module' },
      linterOptions: { reportUnusedDisableDirectives: 'off', noInlineConfig: true },
      rules: { complexity: ['warn', 0], 'cc/span': 'warn' } }] });
  const out = {};
  for (let i = 0; i < files.length; i += 400) {
    for (const r of await eslint.lintFiles(files.slice(i, i + 400))) {
      const text = fs.readFileSync(r.filePath, 'utf8');
      const sf = ts.createSourceFile(r.filePath, text, ts.ScriptTarget.Latest, true);
      let declared = 0;
      const visit = (n) => { if (ts.isFunctionLike(n) && n.body) declared++; ts.forEachChild(n, visit); };
      visit(sf);
      const key = (m) => m.line + ':' + m.column;
      const spans = new Map(r.messages.filter(m => m.ruleId === 'cc/span').map(m => [key(m), JSON.parse(m.message)]));
      const functions = r.messages.filter(m => m.ruleId === 'complexity').map(m => {
        const s = spans.get(key(m)) || {};
        return { name: s.name || m.message.split(' has a')[0], origin: s.origin, line: m.line, start: s.start, end: s.end,
          complexity: +(/complexity of (\d+)/.exec(m.message) || [0, 0])[1] };
      });
      out[r.filePath] = { declared, fatal: r.messages.filter(m => m.fatal).map(m => m.message + ' @' + m.line), functions };
    }
  }
  fs.writeFileSync(outFile, JSON.stringify(out));
})().catch(e => { console.error(e); process.exit(1); });
