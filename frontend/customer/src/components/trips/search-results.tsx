'use client';

import { useInfiniteQuery } from '@tanstack/react-query';
import { AlertCircleIcon, Loader2Icon, SearchXIcon } from 'lucide-react';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useMemo } from 'react';
import {
    TripCard,
    TripFilters,
    TripListSkeleton,
} from '@/components/trips/index.ts';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import { usePathname, useRouter } from '@/i18n/routing.ts';
import { filterScheduledTripsInfiniteOptions } from '@/lib/api/index.ts';
import {
    parseTripSearchParams,
    serializeTripSearchParams,
    type TripSearchParams,
} from '@/lib/search-params.ts';
import { getErrorMessage } from '@/lib/toast.ts';

export function SearchResults() {
    const t = useTranslations('Trips');
    const searchParams = useSearchParams();
    const router = useRouter();
    const pathname = usePathname();

    // Parse search params
    const filters = useMemo(
        () => parseTripSearchParams(searchParams),
        [searchParams],
    );

    const hasRequiredParams = !!(
        filters.originStationId
        && filters.destinationStationId
        && filters.departureDate
    );

    // Query for trips
    const {
        data,
        isLoading,
        isError,
        error,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        refetch,
    } = useInfiniteQuery({
        ...filterScheduledTripsInfiniteOptions({
            query: {
                originStationId: filters.originStationId,
                destinationStationId: filters.destinationStationId,
                departureDate: filters.departureDate,
                sortBy: filters.sortBy,
                sortDirection: filters.sortDirection,
                minPrice: filters.minPrice,
                maxPrice: filters.maxPrice,
                availableOnly: filters.availableOnly,
                size: 10,
            },
        }),
        enabled: hasRequiredParams,
        getNextPageParam: (lastPage) =>
            lastPage.hasNext ? lastPage.nextCursor : undefined,
        initialPageParam: '' as const,
    });

    // Flatten all trips from pages
    const trips = useMemo(
        () => data?.pages.flatMap((page) => page.content ?? []) ?? [],
        [data],
    );

    // Update URL when filters change
    const handleFiltersChange = useCallback(
        (newFilters: Partial<TripSearchParams>) => {
            const updated = { ...filters, ...newFilters };
            const params = serializeTripSearchParams(updated);
            router.replace(`${pathname}?${params.toString()}`);
        },
        [filters, router, pathname],
    );

    // Intersection observer for infinite scroll
    useEffect(() => {
        if (!hasNextPage || isFetchingNextPage) return;

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0]?.isIntersecting) {
                    fetchNextPage();
                }
            },
            { threshold: 0.1 },
        );

        const sentinel = document.getElementById('load-more-sentinel');
        if (sentinel) {
            observer.observe(sentinel);
        }

        return () => observer.disconnect();
    }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

    // Missing required params
    if (!hasRequiredParams) {
        return (
            <div className='flex flex-col items-center justify-center py-16 text-center animate-fade-in'>
                <div className='mb-4 rounded-full bg-muted p-5'>
                    <SearchXIcon className='h-10 w-10 text-muted-foreground/60' />
                </div>
                <h2 className='text-lg font-semibold'>{t('noResults')}</h2>
                <p className='mt-2 max-w-sm text-muted-foreground'>
                    {t('noResultsDescription')}
                </p>
            </div>
        );
    }

    // Loading state
    if (isLoading) {
        return (
            <div className='space-y-6'>
                <TripFilters
                    filters={filters}
                    onFiltersChange={handleFiltersChange}
                />
                <TripListSkeleton count={5} />
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
            <div className='space-y-6'>
                <TripFilters
                    filters={filters}
                    onFiltersChange={handleFiltersChange}
                />
                <Alert variant='destructive'>
                    <AlertCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('error')}</AlertTitle>
                    <AlertDescription className='flex items-center gap-4'>
                        <span>{getErrorMessage(error, t('error'))}</span>
                        <Button
                            variant='outline'
                            size='sm'
                            onClick={() => refetch()}
                        >
                            {t('retry')}
                        </Button>
                    </AlertDescription>
                </Alert>
            </div>
        );
    }

    // Empty state
    if (trips.length === 0) {
        return (
            <div className='space-y-6'>
                <TripFilters
                    filters={filters}
                    onFiltersChange={handleFiltersChange}
                />
                <div className='flex flex-col items-center justify-center py-16 text-center animate-fade-in'>
                    <div className='mb-4 rounded-full bg-muted p-5'>
                        <SearchXIcon className='h-10 w-10 text-muted-foreground/60' />
                    </div>
                    <h2 className='text-lg font-semibold'>{t('noResults')}</h2>
                    <p className='mt-2 max-w-sm text-muted-foreground'>
                        {t('noResultsDescription')}
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className='space-y-6'>
            <div className='flex flex-wrap items-center justify-between gap-4'>
                <p className='text-sm text-muted-foreground'>
                    {t('results', { count: trips.length })}
                </p>
                <TripFilters
                    filters={filters}
                    onFiltersChange={handleFiltersChange}
                />
            </div>

            <div className='space-y-4'>
                {trips.map((trip) => (
                    <TripCard key={trip.id} trip={trip} />
                ))}
            </div>

            {/* Load More Sentinel */}
            {hasNextPage && (
                <div
                    id='load-more-sentinel'
                    className='flex justify-center py-4'
                >
                    {isFetchingNextPage ? (
                        <div className='flex items-center gap-2 text-muted-foreground'>
                            <Loader2Icon className='h-4 w-4 motion-safe:animate-spin' />
                            <span>{t('loadingMore')}</span>
                        </div>
                    ) : (
                        <Button
                            variant='outline'
                            onClick={() => fetchNextPage()}
                        >
                            {t('loadMore')}
                        </Button>
                    )}
                </div>
            )}
        </div>
    );
}
