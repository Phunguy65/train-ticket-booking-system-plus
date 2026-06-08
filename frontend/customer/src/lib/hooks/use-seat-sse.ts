'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { Seat } from '@/lib/api/generated/types.gen.ts';
import { getAccessToken } from '@/lib/auth/token-store.ts';

// ============================================================================
// Types
// ============================================================================

/**
 * Seat status values matching backend RouteSeatAvailabilityStatus enum.
 */
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED' | 'CANCELLED';

/**
 * SSE event names sent by the backend.
 */
export type SeatSseEventName = 'seat-initial' | 'seat-changed';

/**
 * Individual seat update in SSE payload.
 */
export interface SeatSseUpdate {
    seatId: string;
    status: SeatStatus;
    bookingId?: string;
}

/**
 * Payload structure for seat-initial and seat-changed SSE events.
 */
export interface SeatSsePayload {
    scheduledTripId: string;
    seats: SeatSseUpdate[];
    occurredAt: string;
}

/**
 * Parsed SSE event with typed payload.
 */
export interface SeatSseEvent {
    event: SeatSseEventName;
    data: SeatSsePayload;
}

/**
 * Connection state exposed to UI components.
 */
export type SseConnectionStatus =
    | 'connecting'
    | 'connected'
    | 'reconnecting'
    | 'disconnected';

/**
 * Options for the useSeatSSE hook.
 */
export interface UseSeatSseOptions {
    /** Scheduled trip ID to subscribe to */
    scheduledTripId: string;
    /** Whether SSE connection should be enabled */
    enabled?: boolean;
    /** Initial retry delay in milliseconds (default: 1000) */
    initialRetryDelay?: number;
    /** Maximum retry delay in milliseconds (default: 30000) */
    maxRetryDelay?: number;
    /** Callback invoked when seat updates are received */
    onSeatUpdate?: (
        seats: SeatSseUpdate[],
        eventName: SeatSseEventName,
    ) => void;
    /** Callback invoked when connection status changes */
    onConnectionStatusChange?: (status: SseConnectionStatus) => void;
}

/**
 * Return type of the useSeatSSE hook.
 */
export interface UseSeatSseResult {
    /** Current connection status */
    connectionStatus: SseConnectionStatus;
    /** Latest seat updates received */
    seatUpdates: SeatSseUpdate[];
    /** Whether the connection is active (connected or reconnecting) */
    isActive: boolean;
}

// ============================================================================
// SSE Frame Parsing
// ============================================================================

/**
 * Parses a single SSE frame (text block between double newlines) into event and data.
 *
 * SSE format:
 * event: <name>\n
 * data: <json>\n
 * \n
 */
export function parseSseFrame(frame: string): SeatSseEvent | null {
    if (!frame.trim()) {
        return null;
    }

    const lines = frame.split('\n');
    let eventName: SeatSseEventName | undefined;
    const dataLines: string[] = [];

    for (const line of lines) {
        if (line.startsWith('event:')) {
            const value = line.slice(6).trim();
            if (value === 'seat-initial' || value === 'seat-changed') {
                eventName = value;
            }
        } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim());
        }
    }

    if (!eventName || dataLines.length === 0) {
        return null;
    }

    try {
        const rawData = dataLines.join('\n');
        const data = JSON.parse(rawData) as SeatSsePayload;
        return { event: eventName, data };
    } catch {
        return null;
    }
}

/**
 * Splits a buffer into complete SSE frames and returns the remainder.
 */
export function extractSseFrames(buffer: string): {
    frames: string[];
    remainder: string;
} {
    // Normalize line endings
    const normalized = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

    // Split by double newlines (SSE frame delimiter)
    const parts = normalized.split('\n\n');

    // Last part may be incomplete
    const remainder = parts.pop() ?? '';

    return { frames: parts, remainder };
}

// ============================================================================
// Exponential Backoff
// ============================================================================

/**
 * Calculates the next retry delay using exponential backoff.
 */
export function calculateBackoffDelay(
    attempt: number,
    initialDelay: number,
    maxDelay: number,
): number {
    // Exponential backoff: initialDelay * 2^(attempt-1)
    const delay = initialDelay * 2 ** (attempt - 1);
    return Math.min(delay, maxDelay);
}

// ============================================================================
// Seat Update Merging
// ============================================================================

/**
 * Merges SSE seat updates into an existing seat array.
 * Returns a new array with updated seat statuses.
 */
export function mergeSeatsWithUpdates(
    existingSeats: Seat[],
    updates: SeatSseUpdate[],
): Seat[] {
    if (updates.length === 0) {
        return existingSeats;
    }

    // Create a map of updates by seatId for O(1) lookup
    const updateMap = new Map(updates.map((u) => [u.seatId, u]));

    return existingSeats.map((seat) => {
        const update = updateMap.get(seat.id ?? '');
        if (update) {
            return {
                ...seat,
                status: update.status,
            };
        }
        return seat;
    });
}

/**
 * Filters selected seat IDs to remove any that are no longer available.
 */
export function reconcileSelectedSeats(
    selectedSeatIds: Set<string>,
    seatUpdates: SeatSseUpdate[],
): Set<string> {
    const unavailableSeatIds = new Set(
        seatUpdates
            .filter((u) => u.status !== 'AVAILABLE')
            .map((u) => u.seatId),
    );

    const reconciledSet = new Set<string>();
    for (const seatId of selectedSeatIds) {
        if (!unavailableSeatIds.has(seatId)) {
            reconciledSet.add(seatId);
        }
    }

    return reconciledSet;
}

// ============================================================================
// Hook Implementation
// ============================================================================

const DEFAULT_INITIAL_RETRY_DELAY = 1000;
const DEFAULT_MAX_RETRY_DELAY = 30000;

/**
 * React hook for subscribing to real-time seat updates via SSE.
 *
 * Features:
 * - Authenticated SSE connection using fetch with credentials
 * - Automatic reconnection with exponential backoff
 * - Proper cleanup on unmount
 * - Connection status tracking
 */
export function useSeatSSE(options: UseSeatSseOptions): UseSeatSseResult {
    const {
        scheduledTripId,
        enabled = true,
        initialRetryDelay = DEFAULT_INITIAL_RETRY_DELAY,
        maxRetryDelay = DEFAULT_MAX_RETRY_DELAY,
        onSeatUpdate,
        onConnectionStatusChange,
    } = options;

    const [connectionStatus, setConnectionStatus] =
        useState<SseConnectionStatus>('disconnected');
    const [seatUpdates, setSeatUpdates] = useState<SeatSseUpdate[]>([]);

    // Refs to track reconnection state
    const abortControllerRef = useRef<AbortController | null>(null);
    const retryAttemptRef = useRef(0);
    const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const isMountedRef = useRef(true);

    // Ref to break circular dependency between connect and scheduleReconnect
    const connectRef = useRef<() => Promise<void>>();

    // Stable callback refs
    const onSeatUpdateRef = useRef(onSeatUpdate);
    const onConnectionStatusChangeRef = useRef(onConnectionStatusChange);

    useEffect(() => {
        onSeatUpdateRef.current = onSeatUpdate;
    }, [onSeatUpdate]);

    useEffect(() => {
        onConnectionStatusChangeRef.current = onConnectionStatusChange;
    }, [onConnectionStatusChange]);

    const updateConnectionStatus = useCallback(
        (status: SseConnectionStatus) => {
            if (!isMountedRef.current) return;
            setConnectionStatus(status);
            onConnectionStatusChangeRef.current?.(status);
        },
        [],
    );

    const scheduleReconnect = useCallback(() => {
        if (!isMountedRef.current) return;

        retryAttemptRef.current += 1;
        const delay = calculateBackoffDelay(
            retryAttemptRef.current,
            initialRetryDelay,
            maxRetryDelay,
        );

        updateConnectionStatus('reconnecting');

        retryTimeoutRef.current = setTimeout(() => {
            if (isMountedRef.current) {
                connectRef.current?.();
            }
        }, delay);
    }, [initialRetryDelay, maxRetryDelay, updateConnectionStatus]);

    const connect = useCallback(async () => {
        if (!isMountedRef.current || !scheduledTripId) return;

        // Abort any existing connection
        abortControllerRef.current?.abort();
        abortControllerRef.current = new AbortController();
        const { signal } = abortControllerRef.current;

        const isReconnect = retryAttemptRef.current > 0;
        updateConnectionStatus(isReconnect ? 'reconnecting' : 'connecting');

        try {
            const token = getAccessToken();
            const headers: Record<string, string> = {
                Accept: 'text/event-stream',
                'Cache-Control': 'no-cache',
            };
            if (token) {
                headers.Authorization = `Bearer ${token}`;
            }

            const sseBaseUrl = process.env.NEXT_PUBLIC_SSE_BASE_URL ?? '';
            const response = await fetch(
                `${sseBaseUrl}/api/v1/sse/trips/${scheduledTripId}/seats`,
                {
                    method: 'GET',
                    headers,
                    signal,
                },
            );

            if (!response.ok) {
                throw new Error(`SSE connection failed: ${response.status}`);
            }

            if (!response.body) {
                throw new Error('No response body for SSE');
            }

            // Reset retry counter on successful connection
            retryAttemptRef.current = 0;
            updateConnectionStatus('connected');

            const reader = response.body
                .pipeThrough(new TextDecoderStream())
                .getReader();
            let buffer = '';

            try {
                while (true) {
                    const { done, value } = await reader.read();

                    if (done) {
                        break;
                    }

                    buffer += value;
                    const { frames, remainder } = extractSseFrames(buffer);
                    buffer = remainder;

                    for (const frame of frames) {
                        const parsedEvent = parseSseFrame(frame);
                        if (parsedEvent && isMountedRef.current) {
                            setSeatUpdates(parsedEvent.data.seats);
                            onSeatUpdateRef.current?.(
                                parsedEvent.data.seats,
                                parsedEvent.event,
                            );
                        }
                    }
                }
            } finally {
                reader.releaseLock();
            }

            // Stream ended normally - try to reconnect
            if (isMountedRef.current && !signal.aborted) {
                scheduleReconnect();
            }
        } catch (error) {
            // Ignore abort errors (intentional disconnection)
            if (error instanceof Error && error.name === 'AbortError') {
                return;
            }

            // Connection failed - schedule reconnect
            if (isMountedRef.current) {
                scheduleReconnect();
            }
        }
    }, [scheduledTripId, updateConnectionStatus, scheduleReconnect]);

    // Assign connect to ref for scheduleReconnect to use
    connectRef.current = connect;

    const disconnect = useCallback(() => {
        // Clear retry timeout
        if (retryTimeoutRef.current) {
            clearTimeout(retryTimeoutRef.current);
            retryTimeoutRef.current = null;
        }

        // Abort active connection
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            abortControllerRef.current = null;
        }

        retryAttemptRef.current = 0;
        updateConnectionStatus('disconnected');
    }, [updateConnectionStatus]);

    // Main effect to manage connection lifecycle
    useEffect(() => {
        isMountedRef.current = true;

        if (enabled && scheduledTripId) {
            connect();
        }

        return () => {
            isMountedRef.current = false;
            disconnect();
        };
    }, [enabled, scheduledTripId, connect, disconnect]);

    return {
        connectionStatus,
        seatUpdates,
        isActive:
            connectionStatus === 'connected'
            || connectionStatus === 'reconnecting',
    };
}
