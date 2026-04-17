import type * as React from 'react';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';

type AuthCardProps = {
    title: string;
    subtitle?: string;
    children: React.ReactNode;
    footer?: React.ReactNode;
};

/**
 * Auth card wrapper used by login and register pages.
 *
 * Layout:
 * - Centers the card vertically and horizontally on the viewport
 * - Full-width with padding on mobile (<640px)
 * - Max-width 448px on sm+ breakpoints
 * - Consistent title, subtitle, body, and footer slots
 */
export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
    return (
        <div className='flex flex-1 min-h-screen items-center justify-center bg-background px-4 py-8'>
            <Card className='w-full max-w-md gap-6 py-6'>
                <CardHeader className='gap-2'>
                    <CardTitle className='text-2xl font-semibold'>
                        {title}
                    </CardTitle>
                    {subtitle ? (
                        <CardDescription>{subtitle}</CardDescription>
                    ) : null}
                </CardHeader>
                <CardContent>{children}</CardContent>
                {footer ? (
                    <CardFooter className='justify-center text-sm'>
                        {footer}
                    </CardFooter>
                ) : null}
            </Card>
        </div>
    );
}
