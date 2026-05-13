'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircleIcon, CalendarIcon, Loader2Icon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import {
    Avatar,
    AvatarFallback,
    AvatarImage,
} from '@/components/ui/avatar.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Calendar } from '@/components/ui/calendar.tsx';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form.tsx';
import { Input } from '@/components/ui/input.tsx';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover.tsx';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Textarea } from '@/components/ui/textarea.tsx';
import {
    getAuthenticatedUserOptions,
    type UserResponse,
    updateAuthenticatedUserMutation,
} from '@/lib/api/index.ts';
import { formatShortDate } from '@/lib/customer-utils.ts';
import {
    getErrorMessage,
    showApiErrorToast,
    showSuccessToast,
} from '@/lib/toast.ts';
import { cn } from '@/lib/utils.ts';
import { profileSchema } from '@/lib/validations/customer.ts';

/**
 * Get initials from a user's full name for avatar fallback
 */
function getInitials(fullName: string | undefined): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(/\s+/);
    if (names.length === 1) {
        return names[0].charAt(0).toUpperCase();
    }
    return (
        names[0].charAt(0) + names[names.length - 1].charAt(0)
    ).toUpperCase();
}

/**
 * Profile header with avatar, name, email, and member since date
 */
function ProfileHeader({
    user,
    t,
}: {
    user: UserResponse;
    t: ReturnType<typeof useTranslations<'Profile'>>;
}) {
    const memberSinceDate = user.createdAt ? new Date(user.createdAt) : null;

    return (
        <div className='relative overflow-hidden rounded-2xl border bg-gradient-to-br from-primary/5 via-card to-accent/5 p-6'>
            <div className='absolute -top-12 -right-12 h-32 w-32 rounded-full bg-primary/10 blur-2xl' />
            <div className='absolute -bottom-8 -left-8 h-24 w-24 rounded-full bg-accent/10 blur-2xl' />
            <div className='relative flex items-center gap-4'>
                <Avatar className='h-16 w-16 ring-2 ring-primary/20'>
                    <AvatarImage alt={user.fullName ?? ''} />
                    <AvatarFallback className='text-lg bg-primary/10'>
                        {getInitials(user.fullName)}
                    </AvatarFallback>
                </Avatar>
                <div className='flex flex-col'>
                    <h2 className='text-xl font-semibold'>{user.fullName}</h2>
                    <p className='text-sm text-muted-foreground'>
                        {user.email}
                    </p>
                    {memberSinceDate && (
                        <p className='text-xs text-muted-foreground'>
                            {t('memberSince', {
                                date: formatShortDate(memberSinceDate),
                            })}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}

export function ProfileForm() {
    const t = useTranslations('Profile');
    const tValidation = useTranslations('Validation');
    const tErrors = useTranslations('Errors');
    const queryClient = useQueryClient();

    const {
        data: user,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getAuthenticatedUserOptions(),
    });

    const form = useForm({
        resolver: zodResolver(profileSchema),
        defaultValues: {
            fullName: '',
            email: '',
            phone: '',
            dateOfBirth: null as Date | null,
            gender: null as string | null,
            idDocumentNumber: null as string | null,
            addressLine: null as string | null,
        },
    });

    // Populate form when user data loads
    useEffect(() => {
        if (user) {
            form.reset({
                fullName: user.fullName ?? '',
                email: user.email ?? '',
                phone: user.phone ?? '',
                dateOfBirth: user.dateOfBirth
                    ? new Date(user.dateOfBirth)
                    : null,
                gender: user.gender ?? null,
                idDocumentNumber: user.idDocumentNumber ?? null,
                addressLine: user.addressLine ?? null,
            });
        }
    }, [user, form]);

    // Dirty state warning - prevent accidental navigation with unsaved changes
    const isDirty = form.formState.isDirty;
    const handleBeforeUnload = useCallback(
        (event: BeforeUnloadEvent) => {
            if (isDirty) {
                event.preventDefault();
            }
        },
        [isDirty],
    );

    useEffect(() => {
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [handleBeforeUnload]);

    const updateProfile = useMutation({
        ...updateAuthenticatedUserMutation(),
        onSuccess: () => {
            showSuccessToast(t('saveSuccess'));
            queryClient.invalidateQueries({
                queryKey: ['getAuthenticatedUser'],
            });
        },
        onError: (error) => {
            showApiErrorToast(error, {
                network: tErrors('networkError'),
                unknown: tErrors('unknownError'),
                fail: t('saveError'),
            });
        },
    });

    const onSubmit = form.handleSubmit((values) => {
        updateProfile.mutate({
            body: {
                fullName: values.fullName,
                email: values.email,
                phone: values.phone || null,
                dateOfBirth: values.dateOfBirth
                    ? values.dateOfBirth.toISOString().split('T')[0]
                    : null,
                gender: values.gender || null,
                idDocumentNumber: values.idDocumentNumber || null,
                addressLine: values.addressLine || null,
            },
        });
    });

    const translateFieldError = (message: string | undefined) => {
        if (!message) return null;
        return tValidation(message as Parameters<typeof tValidation>[0]);
    };

    // Loading state
    if (isLoading) {
        return (
            <div className='space-y-6'>
                {/* Profile header skeleton */}
                <div className='flex items-center gap-4 mb-6'>
                    <Skeleton className='h-16 w-16 rounded-full' />
                    <div className='flex flex-col gap-2'>
                        <Skeleton className='h-6 w-40' />
                        <Skeleton className='h-4 w-48' />
                        <Skeleton className='h-3 w-32' />
                    </div>
                </div>
                {/* Personal Information card skeleton */}
                <Card>
                    <CardHeader>
                        <Skeleton className='h-6 w-40' />
                    </CardHeader>
                    <CardContent className='space-y-4'>
                        {[1, 2, 3, 4, 5].map((id) => (
                            <div
                                key={`profile-skeleton-personal-${id}`}
                                className='space-y-2'
                            >
                                <Skeleton className='h-4 w-24' />
                                <Skeleton className='h-10 w-full' />
                            </div>
                        ))}
                    </CardContent>
                </Card>
                {/* Travel Documents card skeleton */}
                <Card>
                    <CardHeader>
                        <Skeleton className='h-6 w-36' />
                    </CardHeader>
                    <CardContent className='space-y-4'>
                        {[1, 2].map((id) => (
                            <div
                                key={`profile-skeleton-travel-${id}`}
                                className='space-y-2'
                            >
                                <Skeleton className='h-4 w-24' />
                                <Skeleton className='h-10 w-full' />
                            </div>
                        ))}
                    </CardContent>
                </Card>
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

    return (
        <div className='space-y-6'>
            <h1 className='text-2xl font-bold tracking-tight'>{t('title')}</h1>
            {user && <ProfileHeader user={user} t={t} />}

            <Form {...form}>
                <form onSubmit={onSubmit} className='space-y-6' noValidate>
                    {/* Card 1: Personal Information */}
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('personalInfo')}</CardTitle>
                        </CardHeader>
                        <CardContent className='space-y-4'>
                            <FormField
                                control={form.control}
                                name='fullName'
                                render={({ field, fieldState }) => (
                                    <FormItem>
                                        <FormLabel>{t('fullName')}</FormLabel>
                                        <FormControl>
                                            <Input
                                                {...field}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            />
                                        </FormControl>
                                        <FormMessage>
                                            {translateFieldError(
                                                fieldState.error?.message,
                                            )}
                                        </FormMessage>
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name='email'
                                render={({ field, fieldState }) => (
                                    <FormItem>
                                        <FormLabel>{t('email')}</FormLabel>
                                        <FormControl>
                                            <Input
                                                type='email'
                                                {...field}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            />
                                        </FormControl>
                                        <FormMessage>
                                            {translateFieldError(
                                                fieldState.error?.message,
                                            )}
                                        </FormMessage>
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name='phone'
                                render={({ field, fieldState }) => (
                                    <FormItem>
                                        <FormLabel>{t('phone')}</FormLabel>
                                        <FormControl>
                                            <Input
                                                type='tel'
                                                placeholder={t(
                                                    'phonePlaceholder',
                                                )}
                                                {...field}
                                                value={field.value ?? ''}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            />
                                        </FormControl>
                                        <FormDescription>
                                            {t('phoneDescription')}
                                        </FormDescription>
                                        <FormMessage>
                                            {translateFieldError(
                                                fieldState.error?.message,
                                            )}
                                        </FormMessage>
                                    </FormItem>
                                )}
                            />

                            {/* Grouped DOB and Gender on md+ screens */}
                            <div className='grid gap-4 md:grid-cols-2'>
                                <FormField
                                    control={form.control}
                                    name='dateOfBirth'
                                    render={({ field }) => (
                                        <FormItem className='flex flex-col'>
                                            <FormLabel>
                                                {t('dateOfBirth')}
                                            </FormLabel>
                                            <Popover>
                                                <PopoverTrigger asChild>
                                                    <FormControl>
                                                        <Button
                                                            variant='outline'
                                                            className={cn(
                                                                'w-full justify-start text-left font-normal',
                                                                !field.value
                                                                    && 'text-muted-foreground',
                                                            )}
                                                            disabled={
                                                                updateProfile.isPending
                                                            }
                                                        >
                                                            <CalendarIcon className='mr-2 h-4 w-4' />
                                                            {field.value
                                                            instanceof Date
                                                                ? formatShortDate(
                                                                      field.value,
                                                                  )
                                                                : t(
                                                                      'selectDate',
                                                                  )}
                                                        </Button>
                                                    </FormControl>
                                                </PopoverTrigger>
                                                <PopoverContent
                                                    className='w-auto p-0'
                                                    align='start'
                                                >
                                                    <Calendar
                                                        mode='single'
                                                        selected={
                                                            field.value
                                                            instanceof Date
                                                                ? field.value
                                                                : undefined
                                                        }
                                                        onSelect={
                                                            field.onChange
                                                        }
                                                        disabled={(date) =>
                                                            date > new Date()
                                                        }
                                                        initialFocus
                                                    />
                                                </PopoverContent>
                                            </Popover>
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name='gender'
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{t('gender')}</FormLabel>
                                            <Select
                                                onValueChange={field.onChange}
                                                value={field.value ?? ''}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            >
                                                <FormControl>
                                                    <SelectTrigger>
                                                        <SelectValue
                                                            placeholder={t(
                                                                'selectGender',
                                                            )}
                                                        />
                                                    </SelectTrigger>
                                                </FormControl>
                                                <SelectContent>
                                                    <SelectItem value='male'>
                                                        {t(
                                                            'genderOptions.male',
                                                        )}
                                                    </SelectItem>
                                                    <SelectItem value='female'>
                                                        {t(
                                                            'genderOptions.female',
                                                        )}
                                                    </SelectItem>
                                                    <SelectItem value='other'>
                                                        {t(
                                                            'genderOptions.other',
                                                        )}
                                                    </SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </FormItem>
                                    )}
                                />
                            </div>
                        </CardContent>
                    </Card>

                    {/* Card 2: Travel Documents */}
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('travelDocuments')}</CardTitle>
                        </CardHeader>
                        <CardContent className='space-y-4'>
                            <FormField
                                control={form.control}
                                name='idDocumentNumber'
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('idDocument')}</FormLabel>
                                        <FormControl>
                                            <Input
                                                placeholder={t(
                                                    'idDocumentPlaceholder',
                                                )}
                                                {...field}
                                                value={field.value ?? ''}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            />
                                        </FormControl>
                                        <FormDescription>
                                            {t('idDocumentDescription')}
                                        </FormDescription>
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name='addressLine'
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('address')}</FormLabel>
                                        <FormControl>
                                            <Textarea
                                                placeholder={t(
                                                    'addressPlaceholder',
                                                )}
                                                rows={3}
                                                {...field}
                                                value={field.value ?? ''}
                                                disabled={
                                                    updateProfile.isPending
                                                }
                                            />
                                        </FormControl>
                                        <FormDescription>
                                            {t('addressDescription')}
                                        </FormDescription>
                                    </FormItem>
                                )}
                            />
                        </CardContent>
                    </Card>

                    <Button
                        type='submit'
                        className='w-full'
                        disabled={updateProfile.isPending}
                    >
                        {updateProfile.isPending ? (
                            <>
                                <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                                {t('saving')}
                            </>
                        ) : (
                            t('save')
                        )}
                    </Button>
                </form>
            </Form>
        </div>
    );
}
