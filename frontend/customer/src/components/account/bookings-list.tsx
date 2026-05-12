'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircleIcon, Loader2Icon, SearchIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Link } from '@/i18n/routing.ts';
import {
    cancelBookingMutation,
    getAuthenticatedUserOptions,
    getUserBookingsOptions,
} from '@/lib/api/index.ts';
import type { BookingStatus } from '@/lib/customer-utils.ts';
import {
    canCancelBooking,
    formatDateTime,
    formatPrice,
} from '@/lib/customer-utils.ts';
import {
    getErrorMessage,
    showApiErrorToast,
    showSuccessToast,
} from '@/lib/toast.ts';

export function BookingsList() {
    const t = useTranslations('Account');

    // Get authenticated user for userId
    const { data: user } = useQuery({
        ...getAuthenticatedUserOptions(),
    });

    const {
        data: bookings,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getUserBookingsOptions({
            path: { userId: user?.id ?? '' },
            query: { page: 0, size: 20 },
        }),
        enabled: !!user?.id,
    });

    // Loading state
    if (isLoading) {
        return (
            <div className='space-y-4'>
                {[1, 2, 3].map((id) => (
                    <Skeleton key={`booking-skeleton-${id}`} className='h-32' />
                ))}
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
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
        );
    }

    // Empty state
    if (!bookings?.content || bookings.content.length === 0) {
        return (
            <div className='flex flex-col items-center justify-center py-12 text-center'>
                <SearchIcon className='h-12 w-12 text-muted-foreground' />
                <h2 className='mt-4 text-lg font-semibold'>{t('empty')}</h2>
                <p className='mt-2 text-muted-foreground'>
                    {t('emptyDescription')}
                </p>
                <Button className='mt-4' asChild>
                    <Link href='/'>{t('searchTrips')}</Link>
                </Button>
            </div>
        );
    }

    return (
        <div className='space-y-4'>
            {bookings.content.map((booking) => (
                <BookingCard key={booking.id} booking={booking} />
            ))}
        </div>
    );
}

type BookingCardProps = {
    booking: {
        id?: string;
        status?: BookingStatus;
        totalPrice?: number;
        currency?: string;
        createdAt?: string;
        paymentDeadline?: string;
    };
};

function BookingCard({ booking }: BookingCardProps) {
    const t = useTranslations('Account');
    const tStatus = useTranslations('Status');
    const tErrors = useTranslations('Errors');
    const queryClient = useQueryClient();
    const [cancelDialogOpen, setCancelDialogOpen] = useState(false);

    const cancelBooking = useMutation({
        ...cancelBookingMutation(),
        onSuccess: () => {
            showSuccessToast(t('cancelSuccess'));
            queryClient.invalidateQueries({ queryKey: ['getUserBookings'] });
            setCancelDialogOpen(false);
        },
        onError: (error) => {
            showApiErrorToast(error, {
                network: tErrors('networkError'),
                unknown: tErrors('unknownError'),
                fail: t('cancelError'),
            });
        },
    });

    const handleCancel = () => {
        if (!booking.id) return;
        cancelBooking.mutate({
            path: { id: booking.id },
        });
    };

    const statusVariant = getStatusVariant(booking.status as BookingStatus);
    const canCancel = canCancelBooking(booking.status);

    return (
        <Card>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
                <CardTitle className='text-sm font-medium'>
                    {t('bookingId')}: {booking.id?.slice(0, 8)}...
                </CardTitle>
                <Badge variant={statusVariant}>
                    {tStatus(booking.status as BookingStatus)}
                </Badge>
            </CardHeader>
            <CardContent>
                <div className='flex flex-wrap items-center justify-between gap-4'>
                    <div className='space-y-1'>
                        <p className='text-2xl font-bold'>
                            {booking.totalPrice
                                ? formatPrice(booking.totalPrice)
                                : '-'}
                        </p>
                        <p className='text-xs text-muted-foreground'>
                            {booking.createdAt
                                ? formatDateTime(booking.createdAt)
                                : '-'}
                        </p>
                    </div>

                    <div className='flex gap-2'>
                        <Button variant='outline' size='sm' asChild>
                            <Link href={`/booking/${booking.id}`}>
                                {t('viewDetails')}
                            </Link>
                        </Button>

                        {canCancel && (
                            <Dialog
                                open={cancelDialogOpen}
                                onOpenChange={setCancelDialogOpen}
                            >
                                <DialogTrigger asChild>
                                    <Button variant='destructive' size='sm'>
                                        {t('cancel')}
                                    </Button>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>
                                            {t('cancelConfirm.title')}
                                        </DialogTitle>
                                        <DialogDescription>
                                            {t('cancelConfirm.description')}
                                        </DialogDescription>
                                    </DialogHeader>
                                    <DialogFooter>
                                        <DialogClose asChild>
                                            <Button variant='outline'>
                                                {t('cancelConfirm.cancel')}
                                            </Button>
                                        </DialogClose>
                                        <Button
                                            variant='destructive'
                                            onClick={handleCancel}
                                            disabled={cancelBooking.isPending}
                                        >
                                            {cancelBooking.isPending ? (
                                                <>
                                                    <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                                                    {t(
                                                        'cancelConfirm.cancelling',
                                                    )}
                                                </>
                                            ) : (
                                                t('cancelConfirm.confirm')
                                            )}
                                        </Button>
                                    </DialogFooter>
                                </DialogContent>
                            </Dialog>
                        )}
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}

function getStatusVariant(
    status: BookingStatus,
): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
        case 'CONFIRMED':
            return 'default';
        case 'HELD':
            return 'secondary';
        case 'CANCELLED':
            return 'destructive';
        default:
            return 'outline';
    }
}
