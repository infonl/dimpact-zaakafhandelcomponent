import type {Config} from 'jest';

const config: Config = {
    "globalSetup": "<rootDir>/src/globalJest.js",
    "preset": "jest-preset-angular",
    "setupFilesAfterEnv": [
        "<rootDir>/src/setupJest.ts"
    ],
    "moduleNameMapper": {
        "^src/(.*)$": "<rootDir>/src/$1"
    },
    reporters: [
        'default',
        ['github-actions', {silent: false}],
    ],
};

export default config;
