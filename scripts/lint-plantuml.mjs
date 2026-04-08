#!/usr/bin/env node
// Lint PlantUML syntax inside Markdown fenced code blocks.
//
// Usage: node scripts/lint-plantuml.mjs 'docs/**/*.md'
//
// Extracts all ```plantuml blocks from matching Markdown files,
// validates each via `plantuml.jar -syntax`, and reports errors
// with file path + line number.
//
// Exit code: 0 = all OK, 1 = errors found or runtime failure.

import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import {
    createWriteStream,
    existsSync,
    mkdirSync,
    readFileSync,
    renameSync,
    unlinkSync,
} from 'node:fs';
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { pipeline } from 'node:stream/promises';
import { fileURLToPath } from 'node:url';

import { glob } from 'glob';
import remarkParse from 'remark-parse';
import { unified } from 'unified';
import { visit } from 'unist-util-visit';

// ── Configuration (from shared conf) ─────────────────────────────────
const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..');
const CACHE_DIR = resolve(REPO_ROOT, '.cache', 'plantuml');

// Parse shared config (plantuml-tools.conf is a KEY=VALUE shell file)
const confRaw = Object.fromEntries(
    readFileSync(resolve(__dirname, 'plantuml-tools.conf'), 'utf-8')
        .split('\n')
        .filter((l) => l.match(/^\w+=/))
        .map((l) => {
            const eq = l.indexOf('=');
            const key = l.slice(0, eq);
            const val = l.slice(eq + 1).replace(/^["']|["']$/g, '');
            return [key, val];
        }),
);

// Resolve ${VAR} interpolations in config values
function resolveConf(key) {
    let val = confRaw[key] ?? '';
    val = val.replace(/\$\{(\w+)\}/g, (_, k) => confRaw[k] ?? '');
    return val;
}

const PLANTUML_VERSION = confRaw.PLANTUML_VERSION;
const PLANTUML_JAR = resolve(CACHE_DIR, `plantuml-${PLANTUML_VERSION}.jar`);
const PLANTUML_JAR_URL = resolveConf('PLANTUML_JAR_URL');
const PLANTUML_JAR_SHA256 = confRaw.PLANTUML_JAR_SHA256;

// ── Helpers ──────────────────────────────────────────────────────────
const bold = (s) => `\x1b[1m${s}\x1b[0m`;
const red = (s) => `\x1b[31m${s}\x1b[0m`;
const green = (s) => `\x1b[32m${s}\x1b[0m`;
const dim = (s) => `\x1b[2m${s}\x1b[0m`;

/**
 * Download a file with SHA-256 verification and atomic rename.
 * @param {string} url
 * @param {string} dest
 * @param {string} expectedSha
 * @param {string} label
 */
async function downloadVerified(url, dest, expectedSha, label) {
    if (existsSync(dest)) return;

    console.log(`Downloading ${label} ...`);
    mkdirSync(dirname(dest), { recursive: true });

    const tmp = `${dest}.tmp.${process.pid}`;
    try {
        const res = await fetch(url, { redirect: 'follow' });
        if (!res.ok) {
            throw new Error(`HTTP ${res.status} ${res.statusText}`);
        }
        const ws = createWriteStream(tmp);
        await pipeline(res.body, ws);

        // Verify checksum
        const hash = createHash('sha256');
        const data = readFileSync(tmp);
        hash.update(data);
        const actual = hash.digest('hex');
        if (actual !== expectedSha) {
            throw new Error(
                `Checksum mismatch for ${label}!\n` +
                    `  expected: ${expectedSha}\n` +
                    `  got:      ${actual}`,
            );
        }

        // Atomic rename
        renameSync(tmp, dest);
        console.log(`Verified & cached → ${dest}`);
    } catch (err) {
        try {
            unlinkSync(tmp);
        } catch {
            // ignore cleanup error
        }
        throw err;
    }
}

/** @returns {{ valid: boolean, diagramType?: string, errorLine?: number, errorMsg?: string }} */
function checkSyntax(pumlCode) {
    try {
        const stdout = execFileSync('java', ['-jar', PLANTUML_JAR, '-syntax'], {
            input: pumlCode,
            encoding: 'utf-8',
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: 30_000,
        });
        const lines = stdout.trim().split('\n');
        if (lines[0] === 'ERROR') {
            return {
                valid: false,
                errorLine: parseInt(lines[1], 10) || 0,
                errorMsg: lines.slice(2).join(' ').trim() || 'Unknown error',
            };
        }
        return { valid: true, diagramType: lines[0] };
    } catch (err) {
        // execFileSync throws on non-zero exit
        const stderr = err.stderr?.toString().trim() ?? '';
        const stdout = err.stdout?.toString().trim() ?? '';
        const output = stdout || stderr;
        const lines = output.split('\n');
        if (lines[0] === 'ERROR') {
            return {
                valid: false,
                errorLine: parseInt(lines[1], 10) || 0,
                errorMsg: lines.slice(2).join(' ').trim() || 'Unknown error',
            };
        }
        return {
            valid: false,
            errorLine: 0,
            errorMsg: output || err.message,
        };
    }
}

/**
 * Extract plantuml code blocks from markdown content.
 * @returns {Array<{ code: string, startLine: number }>}
 */
function extractPlantumlBlocks(markdownContent) {
    const tree = unified().use(remarkParse).parse(markdownContent);
    const blocks = [];
    visit(tree, 'code', (node) => {
        if (node.lang === 'plantuml') {
            blocks.push({
                code: node.value,
                startLine: node.position?.start?.line ?? 0,
            });
        }
    });
    return blocks;
}

// ── Main ─────────────────────────────────────────────────────────────
async function main() {
    const pattern = process.argv[2];
    if (!pattern) {
        console.error("Usage: node scripts/lint-plantuml.mjs '<glob>'");
        process.exit(1);
    }

    // Preflight: java must be available
    try {
        execFileSync('java', ['-version'], {
            stdio: ['pipe', 'pipe', 'pipe'],
        });
    } catch {
        console.error(red('Error: java is required but not found in PATH.'));
        process.exit(1);
    }

    await downloadVerified(
        PLANTUML_JAR_URL,
        PLANTUML_JAR,
        PLANTUML_JAR_SHA256,
        `plantuml-${PLANTUML_VERSION}.jar`,
    );

    const files = await glob(pattern, {
        cwd: REPO_ROOT,
        ignore: ['**/node_modules/**'],
        nodir: true,
    });

    if (files.length === 0) {
        console.log(dim('No files matched the pattern.'));
        process.exit(0);
    }

    let totalBlocks = 0;
    let totalErrors = 0;
    const errors = [];

    for (const relPath of files.sort()) {
        const absPath = resolve(REPO_ROOT, relPath);
        const content = await readFile(absPath, 'utf-8');
        const blocks = extractPlantumlBlocks(content);

        if (blocks.length === 0) {
            console.log(dim(`  ${relPath} — no plantuml blocks`));
            continue;
        }

        let fileErrors = 0;
        for (const block of blocks) {
            totalBlocks++;
            const result = checkSyntax(block.code);
            if (!result.valid) {
                fileErrors++;
                totalErrors++;
                const errLine =
                    result.errorLine > 0
                        ? block.startLine + result.errorLine
                        : block.startLine;
                errors.push({
                    file: relPath,
                    line: errLine,
                    blockLine: block.startLine,
                    msg: result.errorMsg,
                });
            }
        }

        if (fileErrors === 0) {
            console.log(
                green('  \u2713') +
                    ` ${relPath} — ${blocks.length} block(s) OK`,
            );
        } else {
            console.log(
                red('  \u2717') +
                    ` ${relPath} — ${fileErrors} error(s) in ${blocks.length} block(s)`,
            );
        }
    }

    // Summary
    console.log('');
    if (totalErrors === 0) {
        console.log(
            bold(
                green(
                    `\u2713 All ${totalBlocks} PlantUML block(s) OK across ${files.length} file(s).`,
                ),
            ),
        );
    } else {
        console.log(bold(red(`Errors (${totalErrors}):`)));
        for (const e of errors) {
            console.log(
                `  ${e.file}:${e.line} ${dim(`(block at line ${e.blockLine})`)} — ${e.msg}`,
            );
        }
        console.log('');
        console.log(
            red(
                `\u2717 ${totalErrors} error(s) in ${totalBlocks} PlantUML block(s).`,
            ),
        );
        process.exit(1);
    }
}

main().catch((err) => {
    console.error(red(`Fatal: ${err.message}`));
    process.exit(1);
});
