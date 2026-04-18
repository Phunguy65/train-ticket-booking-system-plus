'use client';

import { useMemo } from 'react';
import { Card, CardContent } from '@/components/ui/card.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';

export function TripCardSkeleton() {
    return (
        <Card>
            <CardContent className='p-4 sm:p-6'>
                <div className='flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between'>
                    {/* Train Info */}
                    <div className='flex items-center gap-3'>
                        <Skeleton className='h-10 w-10 rounded-full' />
                        <div className='space-y-2'>
                            <Skeleton className='h-5 w-24' />
                            <Skeleton className='h-4 w-16' />
                        </div>
                    </div>

                    {/* Route & Time */}
                    <div className='flex flex-1 items-center justify-center gap-8'>
                        <div className='space-y-2 text-center'>
                            <Skeleton className='mx-auto h-6 w-16' />
                            <Skeleton className='mx-auto h-4 w-20' />
                        </div>
                        <div className='flex flex-col items-center gap-1'>
                            <Skeleton className='h-4 w-16' />
                            <Skeleton className='h-px w-24' />
                        </div>
                        <div className='space-y-2 text-center'>
                            <Skeleton className='mx-auto h-6 w-16' />
                            <Skeleton className='mx-auto h-4 w-20' />
                        </div>
                    </div>

                    {/* Price & Availability */}
                    <div className='flex flex-col items-end gap-2'>
                        <Skeleton className='h-7 w-24' />
                        <Skeleton className='h-5 w-20' />
                    </div>

                    {/* Action */}
                    <Skeleton className='h-10 w-28' />
                </div>
            </CardContent>
        </Card>
    );
}

export function TripListSkeleton({ count = 5 }: { count?: number }) {
    const skeletonIds = useMemo(
        () => Array.from({ length: count }, (_, i) => i + 1),
        [count],
    );

    return (
        <div className='space-y-4'>
            {skeletonIds.map((id) => (
                <TripCardSkeleton key={`trip-skeleton-${id}`} />
            ))}
        </div>
    );
}
