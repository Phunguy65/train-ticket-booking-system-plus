'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircleIcon, CalendarIcon, Loader2Icon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
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
import {
    getAuthenticatedUserOptions,
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
            <Card>
                <CardHeader>
                    <Skeleton className='h-6 w-32' />
                </CardHeader>
                <CardContent className='space-y-4'>
                    {[1, 2, 3, 4, 5].map((id) => (
                        <div
                            key={`profile-skeleton-${id}`}
                            className='space-y-2'
                        >
                            <Skeleton className='h-4 w-24' />
                            <Skeleton className='h-10 w-full' />
                        </div>
                    ))}
                </CardContent>
            </Card>
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
        <Card>
            <CardHeader>
                <CardTitle>{t('title')}</CardTitle>
            </CardHeader>
            <CardContent>
                <Form {...form}>
                    <form onSubmit={onSubmit} className='space-y-4' noValidate>
                        <FormField
                            control={form.control}
                            name='fullName'
                            render={({ field, fieldState }) => (
                                <FormItem>
                                    <FormLabel>{t('fullName')}</FormLabel>
                                    <FormControl>
                                        <Input
                                            {...field}
                                            disabled={updateProfile.isPending}
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
                                            disabled={updateProfile.isPending}
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
                                            placeholder={t('phonePlaceholder')}
                                            {...field}
                                            value={field.value ?? ''}
                                            disabled={updateProfile.isPending}
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
                            name='dateOfBirth'
                            render={({ field }) => (
                                <FormItem className='flex flex-col'>
                                    <FormLabel>{t('dateOfBirth')}</FormLabel>
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
                                                    {field.value instanceof Date
                                                        ? formatShortDate(
                                                              field.value,
                                                          )
                                                        : t('selectDate')}
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
                                                    field.value instanceof Date
                                                        ? field.value
                                                        : undefined
                                                }
                                                onSelect={field.onChange}
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
                                        disabled={updateProfile.isPending}
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
                                                {t('genderOptions.male')}
                                            </SelectItem>
                                            <SelectItem value='female'>
                                                {t('genderOptions.female')}
                                            </SelectItem>
                                            <SelectItem value='other'>
                                                {t('genderOptions.other')}
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>
                                </FormItem>
                            )}
                        />

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
                                            disabled={updateProfile.isPending}
                                        />
                                    </FormControl>
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
                                        <Input
                                            placeholder={t(
                                                'addressPlaceholder',
                                            )}
                                            {...field}
                                            value={field.value ?? ''}
                                            disabled={updateProfile.isPending}
                                        />
                                    </FormControl>
                                </FormItem>
                            )}
                        />

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
            </CardContent>
        </Card>
    );
}
