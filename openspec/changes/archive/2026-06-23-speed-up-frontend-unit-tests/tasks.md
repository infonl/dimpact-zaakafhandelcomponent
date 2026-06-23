## 1. Jest Configuration

- [x] 1.1 Add `maxWorkers: 4` to `src/main/app/jest.config.js`
- [x] 1.2 Add `coverageDirectory: "coverage"` explicitly to `src/main/app/jest.config.js` (needed so sharded coverage output path is predictable)

## 2. npm Scripts

- [x] 2.1 Fix `test:ci` in `src/main/app/package.json` — change from `npx jest -t` to `npx jest --coverage --config jest.config.js`
- [x] 2.2 Add `test:shard` script to `src/main/app/package.json`: `"test:shard": "npx jest --coverage --config jest.config.js --shard"`

## 3. Verification

- [x] 3.1 Run `npm run test:report` locally in `src/main/app/` and confirm `Workers: 4` appears in Jest output
- [x] 3.2 Run `npm run test:shard -- --shard=1/3` locally and confirm only a subset of specs executes and coverage is generated
