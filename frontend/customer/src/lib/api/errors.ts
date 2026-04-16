import type { ErrorCode, Violation } from './generated/index.ts';

export class ApiFailError extends Error {
    readonly code?: ErrorCode;
    readonly violations: Array<Violation>;
    readonly statusCode?: number;

    constructor(options: {
        message?: string;
        code?: ErrorCode;
        violations?: Array<Violation>;
        statusCode?: number;
    }) {
        super(options.message ?? 'Request failed');
        this.name = 'ApiFailError';
        this.code = options.code;
        this.violations = options.violations ?? [];
        this.statusCode = options.statusCode;
    }
}

export class ApiTechnicalError extends Error {
    readonly statusCode?: number;

    constructor(options?: { message?: string; statusCode?: number }) {
        super(options?.message ?? 'Unexpected technical failure');
        this.name = 'ApiTechnicalError';
        this.statusCode = options?.statusCode;
    }
}
