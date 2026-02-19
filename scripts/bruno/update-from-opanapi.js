#!/usr/bin/env node

/*
 * SPDX-FileCopyrightText: Brian Armstrong (https://github.com/brian-arms), 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * Bruno Collection OpenAPI Updater
 *
 * Updates a Bruno collection from an OpenAPI JSON URL while preserving
 * environments, variables, and other collection settings.
 *
 * Source: https://gist.github.com/brian-arms/36117243233f7d65105d5a19abe9928c
 *
 * Somewhat tweaked by INFO.nl for our needs.
 *
 * Usage:
 *   node update-from-openapi.js <collection-path> <openapi-url>
 *   node update-from-openapi.js ./my-collection https://api.example.com/openapi.json
 */

const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

// ANSI color codes for console output
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    red: '\x1b[31m',
    cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

function error(message) {
    console.error(`${colors.red}Error: ${message}${colors.reset}`);
}

/**
 * Fetch content from a URL
 */
async function fetchFromUrl(url) {
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https') ? https : http;

        protocol.get(url, (res) => {
            let data = '';

            // Handle redirects
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                return fetchFromUrl(res.headers.location).then(resolve).catch(reject);
            }

            if (res.statusCode !== 200) {
                reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
                return;
            }

            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

/**
 * Convert HTTP method to Bruno format
 */
function normalizeMethod(method) {
    return method.toLowerCase();
}

/**
 * Convert OpenAPI parameter to Bruno format
 */
function convertParameter(param) {
    const brunoParam = {
        name: param.name,
        value: param.example || param.default || '',
        enabled: param.required || false
    };

    if (param.description) {
        brunoParam.description = param.description;
    }

    return brunoParam;
}

/**
 * Generate a default value from an OpenAPI schema
 */
function generateDefaultFromSchema(schema, openApiSpec, visited = new Set()) {
    // Handle $ref references
    if (schema.$ref) {
        const refPath = schema.$ref.replace('#/', '').split('/');
        let refSchema = openApiSpec;
        for (const part of refPath) {
            refSchema = refSchema[part];
            if (!refSchema) return {};
        }

        // Avoid circular references
        if (visited.has(schema.$ref)) {
            return {};
        }
        visited.add(schema.$ref);

        return generateDefaultFromSchema(refSchema, openApiSpec, visited);
    }

    // Handle allOf, oneOf, anyOf
    if (schema.allOf) {
        let result = {};
        for (const subSchema of schema.allOf) {
            Object.assign(result, generateDefaultFromSchema(subSchema, openApiSpec, visited));
        }
        return result;
    }

    if (schema.oneOf || schema.anyOf) {
        const subSchemas = schema.oneOf || schema.anyOf;
        if (subSchemas.length > 0) {
            return generateDefaultFromSchema(subSchemas[0], openApiSpec, visited);
        }
    }

    // Return example or default if provided
    if (schema.example !== undefined) {
        return schema.example;
    }
    if (schema.default !== undefined) {
        return schema.default;
    }

    // Generate based on type
    switch (schema.type) {
        case 'object':
            const obj = {};
            if (schema.properties) {
                for (const [propName, propSchema] of Object.entries(schema.properties)) {
                    obj[propName] = generateDefaultFromSchema(propSchema, openApiSpec, visited);
                }
            }
            return obj;

        case 'array':
            return [];

        case 'string':
            return '';

        case 'number':
        case 'integer':
            return 0;

        case 'boolean':
            return false;

        case 'null':
            return null;

        default:
            return '';
    }
}

/**
 * Determine auth mode from OpenAPI operation
 */
function determineAuthMode(operation, openApiSpec) {
    // Check if operation has security requirements
    const security = operation.security || openApiSpec.security || [];

    if (security.length === 0) {
        return { mode: 'none' };
    }

    // Get the first security requirement
    const securityScheme = security[0];
    const schemeName = Object.keys(securityScheme)[0];

    if (!schemeName) {
        return { mode: 'none' };
    }

    // Look up the security scheme definition
    const securitySchemes = openApiSpec.components?.securitySchemes || {};
    const schemeDefinition = securitySchemes[schemeName];

    if (!schemeDefinition) {
        return { mode: 'inherit' };
    }

    // Map OpenAPI security types to Bruno auth modes
    switch (schemeDefinition.type) {
        case 'http':
            if (schemeDefinition.scheme === 'bearer') {
                return { mode: 'bearer', tokenVar: 'token' };
            } else if (schemeDefinition.scheme === 'basic') {
                return { mode: 'basic' };
            }
            return { mode: 'inherit' };

        case 'apiKey':
            return { mode: 'inherit' }; // Handle via headers

        case 'oauth2':
            return { mode: 'oauth2' };

        default:
            return { mode: 'inherit' };
    }
}

/**
 * Generate Bruno request file content
 */
function generateBrunoRequest(operationId, method, urlPath, operation, servers = [], openApiSpec = {}) {
    const name = operation.summary || operationId || urlPath;
    const description = operation.description || '';

    // Determine auth mode
    // INFO.nl: we always use inherit mode for auth, and define the authorisation configuration globally in the collection.bru file.
    const authInfo = { mode: 'inherit' }
    // determineAuthMode(operation, openApiSpec);

    // Build query parameters
    const queryParams = (operation.parameters || [])
        .filter(p => p.in === 'query')
        .map(p => `  ${p.name}: ${p.example || p.default || ''}`);

    // Build headers
    const headerParams = (operation.parameters || [])
        .filter(p => p.in === 'header')
        .map(p => `  ${p.name}: ${p.example || p.default || ''}`);

    // Build path parameters (as variables in URL)
    const pathParams = (operation.parameters || [])
        .filter(p => p.in === 'path');

    // Build path variables with example values
    const pathVariables = pathParams.map(p => {
        const value = p.example || p.default || `{${p.name}}`;
        return `  ${p.name}: ${value}`;
    });

    // Replace path parameters with Bruno variables
    let brunoUrl = urlPath;
    pathParams.forEach(p => {
        brunoUrl = brunoUrl.replace(`{${p.name}}`, `:${p.name}`);
    });

    // Determine body type
    let bodyType = 'none';
    let bodyContent = '';
    const requestBody = operation.requestBody;

    if (requestBody && requestBody.content) {
        const contentType = Object.keys(requestBody.content)[0];
        if (contentType.includes('json')) {
            bodyType = 'json';
            const schema = requestBody.content[contentType].schema;
            const example = requestBody.content[contentType].example;

            if (example) {
                bodyContent = JSON.stringify(example, null, 2);
            } else if (schema) {
                // Generate a default JSON object from schema
                bodyContent = JSON.stringify(generateDefaultFromSchema(schema, openApiSpec), null, 2);
            } else {
                bodyContent = '{}';
            }
        } else if (contentType.includes('form')) {
            bodyType = 'form-urlencoded';
        } else if (contentType.includes('multipart')) {
            bodyType = 'multipart-form';
        }
    }

    // Build the .bru file content
    let bruContent = `meta {
  name: ${name}
  type: http
  seq: 1
}

${method} {
  url: {{baseUrl}}${brunoUrl}
  body: ${bodyType}
  auth: ${authInfo.mode}
}
`;

    // Add path params section
    if (pathVariables.length > 0) {
        bruContent += `\nparams:path {
${pathVariables.join('\n')}
}
`;
    }

    // Add query params section
    if (queryParams.length > 0) {
        bruContent += `\nquery {
${queryParams.join('\n')}
}
`;
    }

    // Add headers section
    if (headerParams.length > 0) {
        bruContent += `\nheaders {
${headerParams.join('\n')}
}
`;
    }

    // Add body section
    if (bodyType === 'json' && bodyContent) {
        bruContent += `\nbody:json {
  ${bodyContent.split('\n').join('\n  ')}
}
`;
    }

    // Add auth section for bearer tokens
    if (authInfo.mode === 'bearer' && authInfo.tokenVar) {
        bruContent += `\nauth:bearer {
  token: {{${authInfo.tokenVar}}}
}
`;
    }

    // Add docs section
    if (description) {
        bruContent += `\ndocs {
  ${description}
}
`;
    }

    return bruContent;
}

/**
 * Sanitize filename to be filesystem-safe
 */
function sanitizeFilename(name) {
    return name
        .replace(/[^a-zA-Z0-9-_ ]/g, '')
        .replace(/\s+/g, ' ')
        .trim();
}

/**
 * Convert string to snake_case
 */
function toSnakeCase(str) {
    return str
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')  // Replace non-alphanumeric with underscore
        .replace(/^_+|_+$/g, '')  // Trim underscores from start/end
        .replace(/_+/g, '_');  // Collapse multiple underscores
}

/**
 * Get or create directory path
 */
function ensureDirectory(dirPath) {
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
    }
}

/**
 * Find the most recent collection in parent folder
 */
function findMostRecentCollection(parentFolder) {
    if (!fs.existsSync(parentFolder)) {
        return null;
    }

    const entries = fs.readdirSync(parentFolder);
    const collections = [];

    for (const entry of entries) {
        const entryPath = path.join(parentFolder, entry);
        const stat = fs.statSync(entryPath);

        if (stat.isDirectory()) {
            const brunoJsonPath = path.join(entryPath, 'bruno.json');
            if (fs.existsSync(brunoJsonPath)) {
                collections.push({
                    path: entryPath,
                    name: entry,
                    mtime: stat.mtime
                });
            }
        }
    }

    if (collections.length === 0) {
        return null;
    }

    // Sort by modification time, most recent first
    collections.sort((a, b) => b.mtime - a.mtime);
    return collections[0].path;
}

/**
 * Copy environments from source collection to target
 */
function copyEnvironments(sourceCollection, targetCollection) {
    const sourceEnvPath = path.join(sourceCollection, 'environments');
    const targetEnvPath = path.join(targetCollection, 'environments');

    if (fs.existsSync(sourceEnvPath)) {
        log(`Copying environments from previous collection`, 'cyan');
        ensureDirectory(targetEnvPath);
        fs.cpSync(sourceEnvPath, targetEnvPath, { recursive: true });
        return true;
    }

    return false;
}

/**
 * Copy collection.bru file from source collection to target
 */
function copyCollectionBruFile(sourceCollection, targetCollection) {
    const sourceEnvPath = path.join(sourceCollection, 'collection.bru');
    const targetEnvPath = path.join(targetCollection, 'collection.bru');

    if (fs.existsSync(sourceEnvPath)) {
        log(`Copying 'collection.bru' file from previous collection`, 'cyan');
        fs.cpSync(sourceEnvPath, targetEnvPath);
        return true;
    }

    return false;
}

/**
 * Open collection in Bruno
 */
function openInBruno(collectionPath) {
    const brunoJsonPath = path.join(collectionPath, 'bruno.json');

    if (!fs.existsSync(brunoJsonPath)) {
        error('Cannot open in Bruno: bruno.json not found');
        return;
    }

    log('Opening collection in Bruno...', 'cyan');

    // Try to open with Bruno CLI or via open command
    const { spawn } = require('child_process');

    // On macOS, try to open with the default application
    if (process.platform === 'darwin') {
        spawn('open', ['-a', 'Bruno', collectionPath], { detached: true, stdio: 'ignore' });
    } else if (process.platform === 'win32') {
        spawn('cmd', ['/c', 'start', 'bruno', collectionPath], { detached: true, stdio: 'ignore' });
    } else {
        // Linux
        spawn('bruno', [collectionPath], { detached: true, stdio: 'ignore' });
    }
}

/**
 * Parse OpenAPI spec and generate Bruno collection structure
 */
function convertOpenApiToBruno(openApiSpec, options = {}) {
    const { organizeByTags = false } = options;
    const paths = openApiSpec.paths || {};
    const servers = openApiSpec.servers || [];
    const baseUrl = servers.length > 0 ? servers[0].url : '';

    const requests = [];

    // Process each path
    for (const [urlPath, pathItem] of Object.entries(paths)) {
        const methods = ['get', 'post', 'put', 'patch', 'delete', 'options', 'head'];

        for (const method of methods) {
            if (pathItem[method]) {
                const operation = pathItem[method];
                const operationId = operation.operationId || `${method}_${urlPath}`;
                const tags = operation.tags || [];

                // Determine folder structure based on organization preference
                let folderPath;
                if (organizeByTags && tags.length > 0) {
                    folderPath = tags[0];
                } else {
                    // Organize by path segments
                    const pathSegments = urlPath.split('/').filter(s => s && !s.startsWith('{'));
                    folderPath = pathSegments.length > 0 ? pathSegments.join('/') : 'api';
                }

                requests.push({
                    method,
                    urlPath,
                    operation,
                    operationId,
                    folderPath,
                    name: operation.summary || operationId
                });
            }
        }
    }

    return { requests, baseUrl, servers, info: openApiSpec.info };
}

/**
 * Create new Bruno collection with OpenAPI data
 */
function updateFromOpanapi(parentFolder, openApiSpec, options = {}) {
    const { dryRun = false, organizeByTags = false } = options;

    // Ensure parent folder exists
    if (!dryRun) {
        ensureDirectory(parentFolder);
    }

    // Find most recent collection to copy environments from
    const lastCollection = findMostRecentCollection(parentFolder);

    // Create timestamped collection name using OpenAPI title
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').replace('T', '_').split('.')[0];
    const apiTitle = openApiSpec.info?.title || 'api';
    const baseName = toSnakeCase(apiTitle);
    const collectionName = `${baseName}-${timestamp}`;
    const collectionPath = path.join(parentFolder, collectionName);

    log(`Creating new collection: ${collectionName}`, 'bright');

    if (!dryRun) {
        ensureDirectory(collectionPath);
    }

    // Copy environments from last collection if it exists
    if (lastCollection && !dryRun) {
        const environmentsCopied = copyEnvironments(lastCollection, collectionPath);
        if (environmentsCopied) {
            log(`✓ Copied environments from: ${path.basename(lastCollection)}`, 'green');
        }
        const collectionBruFileCopied = copyCollectionBruFile(lastCollection, collectionPath);
        if (collectionBruFileCopied) {
            log(`✓ Copied 'collection.bru' from: ${path.basename(lastCollection)}`, 'green');
        }
    } else if (lastCollection) {
        log(`[DRY RUN] Would copy environments and 'collection.bru' file from: ${path.basename(lastCollection)}`, 'cyan');
    } else {
        log(`No previous collection found - creating fresh environment`, 'yellow');
    }

    // Create bruno.json with timestamped name
    const brunoJson = {
        version: '1',
        name: collectionName,
        type: 'collection',
        ignore: ['node_modules', '.git']
    };

    if (!dryRun) {
        fs.writeFileSync(
            path.join(collectionPath, 'bruno.json'),
            JSON.stringify(brunoJson, null, 2),
            'utf8'
        );
    }

    log(`Collection: ${brunoJson.name}`, 'bright');

    // Convert OpenAPI to Bruno structure
    const { requests, baseUrl, servers, info } = convertOpenApiToBruno(openApiSpec, { organizeByTags });
    log(`Found ${requests.length} endpoints in OpenAPI spec`, 'blue');
    log(`Organization: ${organizeByTags ? 'by tags' : 'by path'}`, 'blue');

    // Group requests by folder
    const folderMap = new Map();
    for (const request of requests) {
        const folder = request.folderPath;
        if (!folderMap.has(folder)) {
            folderMap.set(folder, []);
        }
        folderMap.get(folder).push(request);
    }

    // Create new request files
    let createdCount = 0;
    for (const [folder, folderRequests] of folderMap.entries()) {
        const folderPath = path.join(collectionPath, folder);

        if (dryRun) {
            log(`[DRY RUN] Would create folder: ${folder}`, 'cyan');
        } else {
            ensureDirectory(folderPath);
        }

        for (const request of folderRequests) {
            const filename = `${sanitizeFilename(request.name)}.bru`;
            const filePath = path.join(folderPath, filename);

            const bruContent = generateBrunoRequest(
                request.operationId,
                request.method,
                request.urlPath,
                request.operation,
                servers,
                openApiSpec
            );

            if (dryRun) {
                log(`[DRY RUN] Would create: ${folder}/${filename}`, 'cyan');
            } else {
                fs.writeFileSync(filePath, bruContent, 'utf8');
                createdCount++;
            }
        }
    }

    log(`\n${dryRun ? '[DRY RUN] Would create' : 'Created'} ${createdCount} request files`, 'green');

    // Open in Bruno
    if (!dryRun) {
        openInBruno(collectionPath);
    }

    return { createdCount, folderCount: folderMap.size, collectionPath, collectionName };
}

/**
 * Main function
 */
async function main() {
    const args = process.argv.slice(2);

    if (args.length < 2 || args.includes('--help') || args.includes('-h')) {
        console.log(`
${colors.bright}Bruno Collection OpenAPI Updater${colors.reset}

${colors.bright}Usage:${colors.reset}
  node update-from-openapi.js [options] <parent-folder> <openapi-url>

${colors.bright}Arguments:${colors.reset}
  parent-folder     Parent directory where timestamped collections will be created
  openapi-url       URL to the OpenAPI JSON specification

${colors.bright}Options:${colors.reset}
  --dry-run            Preview changes without modifying files
  --organize-by-tags   Organize requests by OpenAPI tags (default: by path)
  --help, -h           Show this help message

${colors.bright}Examples:${colors.reset}
  node update-from-openapi.js ./collections https://api.example.com/openapi.json
  node update-from-openapi.js --dry-run ./collections https://petstore.swagger.io/v2/swagger.json
  node update-from-openapi.js --organize-by-tags ./collections https://api.example.com/openapi.json
  node update-from-openapi.js ./collections ./local-openapi.json

${colors.bright}Note:${colors.reset}
  - Creates a new timestamped collection for each run
  - Copies environments from the most recent collection in parent folder
  - Opens the new collection in Bruno automatically
`);
        process.exit(0);
    }

    // Parse options
    const options = {
        dryRun: args.includes('--dry-run'),
        organizeByTags: args.includes('--organize-by-tags')
    };

    // Get parent folder and OpenAPI URL
    const positionalArgs = args.filter(arg => !arg.startsWith('--'));
    const [parentFolder, openApiSource] = positionalArgs;

    if (!parentFolder || !openApiSource) {
        error('Missing required arguments');
        console.log('Run with --help for usage information');
        process.exit(1);
    }

    try {
        log(`\n${colors.bright}Starting Bruno Collection Generation${colors.reset}`, 'bright');
        log(`Parent Folder: ${parentFolder}`);
        log(`OpenAPI Source: ${openApiSource}\n`);

        // Fetch or read OpenAPI spec
        let openApiContent;
        if (openApiSource.startsWith('http://') || openApiSource.startsWith('https://')) {
            log('Fetching OpenAPI specification from URL...', 'blue');
            openApiContent = await fetchFromUrl(openApiSource);
        } else {
            log('Reading OpenAPI specification from file...', 'blue');
            openApiContent = fs.readFileSync(openApiSource, 'utf8');
        }

        const openApiSpec = JSON.parse(openApiContent);
        log(`✓ Loaded OpenAPI ${openApiSpec.openapi || openApiSpec.swagger} specification`, 'green');
        log(`  Title: ${openApiSpec.info?.title || 'Untitled'}`);
        log(`  Version: ${openApiSpec.info?.version || 'Unknown'}\n`);

        // Create new collection
        const result = updateFromOpanapi(parentFolder, openApiSpec, options);

        log(`\n${colors.bright}${colors.green}✓ Collection Created${colors.reset}`, 'bright');
        log(`  Name: ${result.collectionName}`);
        log(`  Path: ${result.collectionPath}`);
        log(`  Folders: ${result.folderCount}`);
        log(`  Requests: ${result.createdCount}`);

        if (options.dryRun) {
            log(`\n${colors.yellow}This was a dry run. No files were modified.${colors.reset}`);
            log('Run without --dry-run to create the collection.');
        } else {
            log(`\n${colors.green}✓ Collection opened in Bruno${colors.reset}`);
        }

    } catch (err) {
        error(err.message);
        if (process.env.DEBUG) {
            console.error(err.stack);
        }
        process.exit(1);
    }
}

// Run if executed directly
if (require.main === module) {
    main();
}

module.exports = {
    updateBrunoCollection: updateFromOpanapi,
    convertOpenApiToBruno,
    fetchFromUrl
};
