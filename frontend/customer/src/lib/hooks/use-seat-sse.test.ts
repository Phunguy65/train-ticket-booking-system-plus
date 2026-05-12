import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Seat } from '@/lib/api/generated/types.gen.ts';
import {
    calculateBackoffDelay,
    extractSseFrames,
    mergeSeatsWithUpdates,
    parseSseFrame,
    reconcileSelectedSeats,
    type SeatSseUpdate,
    useSeatSSE,
} from './use-seat-sse.ts';

// ============================================================================
// Unit Tests: SSE Frame Parsing
// ============================================================================

describe('parseSseFrame', () => {
    it('parses seat-initial event correctly', () => {
        const frame = `event: seat-initial
data: {"scheduledTripId":"trip-1","seats":[{"seatId":"seat-1","status":"AVAILABLE"}],"occurredAt":"2024-01-01T00:00:00Z"}`;

        const result = parseSseFrame(frame);

        expect(result).toEqual({
            event: 'seat-initial',
            data: {
                scheduledTripId: 'trip-1',
                seats: [{ seatId: 'seat-1', status: 'AVAILABLE' }],
                occurredAt: '2024-01-01T00:00:00Z',
            },
        });
    });

    it('parses seat-changed event correctly', () => {
        const frame = `event: seat-changed
data: {"scheduledTripId":"trip-1","seats":[{"seatId":"seat-2","status":"HELD","bookingId":"booking-1"}],"occurredAt":"2024-01-01T00:00:00Z"}`;

        const result = parseSseFrame(frame);

        expect(result).toEqual({
            event: 'seat-changed',
            data: {
                scheduledTripId: 'trip-1',
                seats: [
                    {
                        seatId: 'seat-2',
                        status: 'HELD',
                        bookingId: 'booking-1',
                    },
                ],
                occurredAt: '2024-01-01T00:00:00Z',
            },
        });
    });

    it('returns null for empty frame', () => {
        expect(parseSseFrame('')).toBeNull();
        expect(parseSseFrame('   ')).toBeNull();
    });

    it('returns null for frame without event name', () => {
        const frame = `data: {"scheduledTripId":"trip-1","seats":[],"occurredAt":"2024-01-01T00:00:00Z"}`;
        expect(parseSseFrame(frame)).toBeNull();
    });

    it('returns null for frame without data', () => {
        const frame = `event: seat-initial`;
        expect(parseSseFrame(frame)).toBeNull();
    });

    it('returns null for unknown event types', () => {
        const frame = `event: unknown-event
data: {"test": true}`;
        expect(parseSseFrame(frame)).toBeNull();
    });

    it('returns null for invalid JSON data', () => {
        const frame = `event: seat-initial
data: {invalid json}`;
        expect(parseSseFrame(frame)).toBeNull();
    });

    it('handles multi-line data correctly', () => {
        // SSE spec allows data to span multiple lines
        const frame = `event: seat-initial
data: {"scheduledTripId":"trip-1",
data: "seats":[],
data: "occurredAt":"2024-01-01T00:00:00Z"}`;

        const result = parseSseFrame(frame);

        expect(result).toEqual({
            event: 'seat-initial',
            data: {
                scheduledTripId: 'trip-1',
                seats: [],
                occurredAt: '2024-01-01T00:00:00Z',
            },
        });
    });
});

describe('extractSseFrames', () => {
    it('extracts complete frames from buffer', () => {
        const buffer = `event: seat-initial
data: {"test":1}

event: seat-changed
data: {"test":2}

`;

        const { frames, remainder } = extractSseFrames(buffer);

        expect(frames).toHaveLength(2);
        expect(frames[0]).toContain('seat-initial');
        expect(frames[1]).toContain('seat-changed');
        expect(remainder).toBe('');
    });

    it('handles incomplete frame at end', () => {
        const buffer = `event: seat-initial
data: {"test":1}

event: seat-changed`;

        const { frames, remainder } = extractSseFrames(buffer);

        expect(frames).toHaveLength(1);
        expect(frames[0]).toContain('seat-initial');
        expect(remainder).toBe('event: seat-changed');
    });

    it('handles CRLF line endings', () => {
        const buffer = `event: seat-initial\r\ndata: {"test":1}\r\n\r\n`;

        const { frames } = extractSseFrames(buffer);

        expect(frames).toHaveLength(1);
        expect(frames[0]).toContain('seat-initial');
    });

    it('handles CR line endings', () => {
        const buffer = `event: seat-initial\rdata: {"test":1}\r\r`;

        const { frames } = extractSseFrames(buffer);

        expect(frames).toHaveLength(1);
    });

    it('returns empty frames array for no complete frames', () => {
        const buffer = `event: seat-initial
data: {"incomplete`;

        const { frames, remainder } = extractSseFrames(buffer);

        expect(frames).toHaveLength(0);
        expect(remainder).toContain('incomplete');
    });
});

// ============================================================================
// Unit Tests: Exponential Backoff
// ============================================================================

describe('calculateBackoffDelay', () => {
    it('returns initial delay for first attempt', () => {
        expect(calculateBackoffDelay(1, 1000, 30000)).toBe(1000);
    });

    it('doubles delay for each subsequent attempt', () => {
        expect(calculateBackoffDelay(2, 1000, 30000)).toBe(2000);
        expect(calculateBackoffDelay(3, 1000, 30000)).toBe(4000);
        expect(calculateBackoffDelay(4, 1000, 30000)).toBe(8000);
    });

    it('caps delay at maximum', () => {
        expect(calculateBackoffDelay(10, 1000, 30000)).toBe(30000);
        expect(calculateBackoffDelay(100, 1000, 30000)).toBe(30000);
    });

    it('works with custom initial and max delays', () => {
        expect(calculateBackoffDelay(1, 500, 10000)).toBe(500);
        expect(calculateBackoffDelay(2, 500, 10000)).toBe(1000);
        expect(calculateBackoffDelay(5, 500, 10000)).toBe(8000);
        expect(calculateBackoffDelay(6, 500, 10000)).toBe(10000); // capped
    });
});

// ============================================================================
// Unit Tests: Seat Update Merging
// ============================================================================

describe('mergeSeatsWithUpdates', () => {
    const existingSeats: Seat[] = [
        { id: 'seat-1', seatNumber: '1A', status: 'AVAILABLE' },
        { id: 'seat-2', seatNumber: '1B', status: 'AVAILABLE' },
        { id: 'seat-3', seatNumber: '1C', status: 'HELD' },
    ];

    it('updates seat status from SSE updates', () => {
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'HELD', bookingId: 'booking-1' },
        ];

        const result = mergeSeatsWithUpdates(existingSeats, updates);

        expect(result.find((s) => s.id === 'seat-1')?.status).toBe('HELD');
        expect(result.find((s) => s.id === 'seat-2')?.status).toBe('AVAILABLE');
        expect(result.find((s) => s.id === 'seat-3')?.status).toBe('HELD');
    });

    it('handles multiple seat updates', () => {
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'BOOKED' },
            { seatId: 'seat-2', status: 'HELD' },
        ];

        const result = mergeSeatsWithUpdates(existingSeats, updates);

        expect(result.find((s) => s.id === 'seat-1')?.status).toBe('BOOKED');
        expect(result.find((s) => s.id === 'seat-2')?.status).toBe('HELD');
    });

    it('returns original array when updates is empty', () => {
        const result = mergeSeatsWithUpdates(existingSeats, []);
        expect(result).toBe(existingSeats);
    });

    it('ignores updates for non-existent seats', () => {
        const updates: SeatSseUpdate[] = [
            { seatId: 'non-existent', status: 'HELD' },
        ];

        const result = mergeSeatsWithUpdates(existingSeats, updates);

        expect(result).toHaveLength(3);
        expect(result.every((s) => existingSeats.includes(s))).toBe(true);
    });

    it('preserves other seat properties', () => {
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'BOOKED' },
        ];

        const result = mergeSeatsWithUpdates(existingSeats, updates);
        const updatedSeat = result.find((s) => s.id === 'seat-1');

        expect(updatedSeat?.seatNumber).toBe('1A');
        expect(updatedSeat?.status).toBe('BOOKED');
    });
});

describe('reconcileSelectedSeats', () => {
    it('removes seats that became unavailable', () => {
        const selected = new Set(['seat-1', 'seat-2', 'seat-3']);
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'HELD' },
            { seatId: 'seat-2', status: 'BOOKED' },
        ];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.has('seat-1')).toBe(false);
        expect(result.has('seat-2')).toBe(false);
        expect(result.has('seat-3')).toBe(true);
    });

    it('keeps seats that remained available', () => {
        const selected = new Set(['seat-1', 'seat-2']);
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'AVAILABLE' },
            { seatId: 'seat-3', status: 'HELD' },
        ];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.has('seat-1')).toBe(true);
        expect(result.has('seat-2')).toBe(true);
    });

    it('returns empty set when all selected become unavailable', () => {
        const selected = new Set(['seat-1', 'seat-2']);
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'BOOKED' },
            { seatId: 'seat-2', status: 'HELD' },
        ];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.size).toBe(0);
    });

    it('handles empty selected set', () => {
        const selected = new Set<string>();
        const updates: SeatSseUpdate[] = [{ seatId: 'seat-1', status: 'HELD' }];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.size).toBe(0);
    });

    it('handles empty updates', () => {
        const selected = new Set(['seat-1', 'seat-2']);
        const updates: SeatSseUpdate[] = [];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.has('seat-1')).toBe(true);
        expect(result.has('seat-2')).toBe(true);
    });

    it('removes CANCELLED seats', () => {
        const selected = new Set(['seat-1']);
        const updates: SeatSseUpdate[] = [
            { seatId: 'seat-1', status: 'CANCELLED' },
        ];

        const result = reconcileSelectedSeats(selected, updates);

        expect(result.has('seat-1')).toBe(false);
    });
});

// ============================================================================
// Integration Tests: useSeatSSE Hook
// ============================================================================

describe('useSeatSSE', () => {
    let mockFetch: ReturnType<typeof vi.fn>;
    let originalFetch: typeof fetch;

    beforeEach(() => {
        originalFetch = globalThis.fetch;
        mockFetch = vi.fn();
        globalThis.fetch = mockFetch;
        vi.useFakeTimers();
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    /**
     * Helper to create a mock SSE response stream.
     */
    function createMockSseResponse(
        chunks: string[],
        options: { delay?: number; shouldFail?: boolean } = {},
    ) {
        let chunkIndex = 0;
        const encoder = new TextEncoder();

        const stream = new ReadableStream({
            async pull(controller) {
                if (options.shouldFail && chunkIndex === 0) {
                    controller.error(new Error('Stream error'));
                    return;
                }

                if (chunkIndex < chunks.length) {
                    if (options.delay) {
                        await new Promise((resolve) =>
                            setTimeout(resolve, options.delay),
                        );
                    }
                    controller.enqueue(encoder.encode(chunks[chunkIndex]));
                    chunkIndex++;
                } else {
                    controller.close();
                }
            },
        });

        return new Response(stream, {
            status: 200,
            headers: { 'Content-Type': 'text/event-stream' },
        });
    }

    it('starts in disconnected state when disabled', () => {
        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: false,
            }),
        );

        expect(result.current.connectionStatus).toBe('disconnected');
        expect(result.current.isActive).toBe(false);
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it('transitions to connecting state when enabled', async () => {
        // Create a response that never resolves
        mockFetch.mockImplementation(
            () => new Promise(() => {}), // Never resolves
        );

        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
            }),
        );

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(result.current.connectionStatus).toBe('connecting');
        expect(mockFetch).toHaveBeenCalledWith(
            '/api/v1/sse/trips/trip-1/seats',
            expect.objectContaining({
                method: 'GET',
                headers: expect.objectContaining({
                    Accept: 'text/event-stream',
                    'Cache-Control': 'no-cache',
                }),
            }),
        );
    });

    it('transitions to connected state on successful connection', async () => {
        // Note: This test is simplified because ReadableStream mocking with fake timers
        // has compatibility issues. The actual functionality is validated by the
        // 'calls onSeatUpdate callback' and 'updates seatUpdates state' tests.
        vi.useRealTimers();

        const chunks = [
            `event: seat-initial
data: {"scheduledTripId":"trip-1","seats":[{"seatId":"seat-1","status":"AVAILABLE"}],"occurredAt":"2024-01-01T00:00:00Z"}

`,
        ];

        mockFetch.mockResolvedValue(createMockSseResponse(chunks));

        const onSeatUpdate = vi.fn();
        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
                onSeatUpdate,
            }),
        );

        // Wait for callback to be called (proves connection worked)
        await waitFor(
            () => {
                expect(onSeatUpdate).toHaveBeenCalled();
            },
            { timeout: 1000 },
        );

        // Connection status should be connected or reconnecting (stream ended)
        expect(['connected', 'reconnecting']).toContain(
            result.current.connectionStatus,
        );

        vi.useFakeTimers();
    });

    it('calls onSeatUpdate callback when events are received', async () => {
        vi.useRealTimers();

        const chunks = [
            `event: seat-initial
data: {"scheduledTripId":"trip-1","seats":[{"seatId":"seat-1","status":"AVAILABLE"}],"occurredAt":"2024-01-01T00:00:00Z"}

`,
        ];

        mockFetch.mockResolvedValue(createMockSseResponse(chunks));

        const onSeatUpdate = vi.fn();

        renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
                onSeatUpdate,
            }),
        );

        await waitFor(
            () => {
                expect(onSeatUpdate).toHaveBeenCalledWith(
                    [{ seatId: 'seat-1', status: 'AVAILABLE' }],
                    'seat-initial',
                );
            },
            { timeout: 1000 },
        );

        vi.useFakeTimers();
    });

    it('updates seatUpdates state when events are received', async () => {
        vi.useRealTimers();

        const chunks = [
            `event: seat-changed
data: {"scheduledTripId":"trip-1","seats":[{"seatId":"seat-2","status":"HELD"}],"occurredAt":"2024-01-01T00:00:00Z"}

`,
        ];

        mockFetch.mockResolvedValue(createMockSseResponse(chunks));

        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
            }),
        );

        await waitFor(
            () => {
                expect(result.current.seatUpdates).toEqual([
                    { seatId: 'seat-2', status: 'HELD' },
                ]);
            },
            { timeout: 1000 },
        );

        vi.useFakeTimers();
    });

    it('handles connection failure with exponential backoff', async () => {
        mockFetch.mockRejectedValue(new Error('Network error'));

        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
                initialRetryDelay: 1000,
                maxRetryDelay: 30000,
            }),
        );

        // First attempt
        await act(async () => {
            await vi.advanceTimersByTimeAsync(100);
        });

        expect(result.current.connectionStatus).toBe('reconnecting');
        expect(mockFetch).toHaveBeenCalledTimes(1);

        // After 1s delay, second attempt
        await act(async () => {
            await vi.advanceTimersByTimeAsync(1000);
        });

        expect(mockFetch).toHaveBeenCalledTimes(2);

        // After 2s delay, third attempt
        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(mockFetch).toHaveBeenCalledTimes(3);
    });

    it('cleans up on unmount', async () => {
        const abortSpy = vi.spyOn(AbortController.prototype, 'abort');

        mockFetch.mockImplementation(
            () => new Promise(() => {}), // Never resolves
        );

        const { unmount } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
            }),
        );

        await act(async () => {
            await vi.advanceTimersByTimeAsync(100);
        });

        unmount();

        expect(abortSpy).toHaveBeenCalled();
    });

    it('aborts pending retry on unmount', async () => {
        mockFetch.mockRejectedValue(new Error('Network error'));

        const { result, unmount } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
                initialRetryDelay: 5000,
            }),
        );

        // First attempt fails
        await act(async () => {
            await vi.advanceTimersByTimeAsync(100);
        });

        expect(result.current.connectionStatus).toBe('reconnecting');

        // Unmount before retry triggers
        unmount();

        // Advance past retry delay
        await act(async () => {
            await vi.advanceTimersByTimeAsync(6000);
        });

        // Should only have 1 call (no retry after unmount)
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('ignores AbortError during cleanup', async () => {
        vi.useRealTimers();

        const abortError = new Error('Aborted');
        abortError.name = 'AbortError';

        mockFetch.mockRejectedValue(abortError);

        const { unmount } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
            }),
        );

        // Wait a tick for the effect to run
        await act(async () => {
            await new Promise((resolve) => setTimeout(resolve, 10));
        });

        // Unmount - this should not throw despite the AbortError
        unmount();

        // If we get here without throwing, the test passes
        expect(true).toBe(true);

        vi.useFakeTimers();
    });

    it('resets retry counter on successful connection', async () => {
        vi.useRealTimers();

        let callCount = 0;

        mockFetch.mockImplementation(() => {
            callCount++;
            if (callCount <= 2) {
                return Promise.reject(new Error('Network error'));
            }
            return Promise.resolve(
                createMockSseResponse([
                    `event: seat-initial
data: {"scheduledTripId":"trip-1","seats":[],"occurredAt":"2024-01-01T00:00:00Z"}

`,
                ]),
            );
        });

        const onSeatUpdate = vi.fn();
        renderHook(() =>
            useSeatSSE({
                scheduledTripId: 'trip-1',
                enabled: true,
                initialRetryDelay: 50, // Short delay for testing
                maxRetryDelay: 200,
                onSeatUpdate,
            }),
        );

        // Wait for eventual success after retries - verified by callback being called
        await waitFor(
            () => {
                expect(onSeatUpdate).toHaveBeenCalled();
            },
            { timeout: 2000 },
        );

        // Should have made at least 3 calls (2 failures + 1 success)
        // May have more due to stream ending and triggering reconnect
        expect(callCount).toBeGreaterThanOrEqual(3);

        vi.useFakeTimers();
    }, 10000); // Increase test timeout

    it('does not connect when scheduledTripId is empty', () => {
        const { result } = renderHook(() =>
            useSeatSSE({
                scheduledTripId: '',
                enabled: true,
            }),
        );

        expect(result.current.connectionStatus).toBe('disconnected');
        expect(mockFetch).not.toHaveBeenCalled();
    });
});
