'use client';

import type * as React from 'react';

import { cn } from '@/lib/utils.ts';

/**
 * Props for the StickyFooter component.
 */
export type StickyFooterProps = {
    children: React.ReactNode;
    className?: string;
    /**
     * Show the footer only on mobile viewports.
     * @default true
     */
    mobileOnly?: boolean;
};

/**
 * Mobile-only sticky footer container for CTA actions.
 * Stays fixed at the bottom of the viewport and provides
 * consistent padding/styling for action buttons and summaries.
 *
 * Uses safe-area-inset-bottom for proper iOS support.
 */
function StickyFooter({
    children,
    className,
    mobileOnly = true,
}: StickyFooterProps) {
    return (
        <div
            data-slot='sticky-footer'
            className={cn(
                'fixed inset-x-0 bottom-0 z-40 border-t bg-background/95 backdrop-blur-sm supports-backdrop-filter:bg-background/80',
                'pb-[env(safe-area-inset-bottom)]',
                mobileOnly && 'lg:hidden',
                className,
            )}
        >
            <div className='container mx-auto px-4 py-3'>{children}</div>
        </div>
    );
}

/**
 * Spacer component to prevent content from being hidden behind the sticky footer.
 * Place this at the bottom of your page content.
 */
function StickyFooterSpacer({
    className,
    mobileOnly = true,
}: {
    className?: string;
    mobileOnly?: boolean;
}) {
    return (
        <div
            data-slot='sticky-footer-spacer'
            className={cn(
                'h-20 pb-[env(safe-area-inset-bottom)]',
                mobileOnly && 'lg:hidden',
                className,
            )}
            aria-hidden='true'
        />
    );
}

export { StickyFooter, StickyFooterSpacer };
