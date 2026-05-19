'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowRightLeftIcon, Loader2Icon, SearchIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button.tsx';
import { DatePickerInput } from '@/components/ui/date-picker-input.tsx';
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form.tsx';
import { useRouter } from '@/i18n/routing.ts';
import { buildTripSearchUrl } from '@/lib/search-params.ts';
import { tripSearchSchema } from '@/lib/validations/customer.ts';
import { StationCombobox } from './station-combobox.tsx';

export function TripSearchForm() {
    const t = useTranslations('Search');
    const tValidation = useTranslations('Validation');
    const router = useRouter();

    const form = useForm({
        resolver: zodResolver(tripSearchSchema),
        defaultValues: {
            originStationId: '',
            destinationStationId: '',
            departureDate: undefined as Date | undefined,
        },
    });

    const onSubmit = form.handleSubmit((values) => {
        const url = buildTripSearchUrl({
            originStationId: values.originStationId,
            destinationStationId: values.destinationStationId,
            departureDate: values.departureDate.toISOString().split('T')[0],
        });
        router.push(url);
    });

    const handleSwap = () => {
        const origin = form.getValues('originStationId');
        const destination = form.getValues('destinationStationId');
        form.setValue('originStationId', destination);
        form.setValue('destinationStationId', origin);
    };

    const translateFieldError = (message: string | undefined) => {
        if (!message) return null;
        // Only translate if the message looks like a translation key (e.g., "field.errorType")
        // Zod's internal messages like "Invalid input: expected X, received Y" should not be translated
        if (message.includes('.') && !message.includes(' ')) {
            return tValidation(message as Parameters<typeof tValidation>[0]);
        }
        // For internal Zod messages, return a generic validation error
        return tValidation('departureDate.required');
    };

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return (
        <Form {...form}>
            <form
                onSubmit={onSubmit}
                className='w-full max-w-4xl space-y-4'
                noValidate
            >
                <div className='grid gap-4 md:grid-cols-[1fr,auto,1fr,1fr,auto]'>
                    {/* Origin Station */}
                    <FormField
                        control={form.control}
                        name='originStationId'
                        render={({ field, fieldState }) => (
                            <FormItem className='flex flex-col'>
                                <FormLabel>{t('origin')}</FormLabel>
                                <FormControl>
                                    <StationCombobox
                                        value={field.value}
                                        onValueChange={field.onChange}
                                        placeholder={t('originPlaceholder')}
                                        disabled={form.formState.isSubmitting}
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

                    {/* Swap Button */}
                    <div className='flex items-end pb-2 md:pb-0 md:pt-6'>
                        <Button
                            type='button'
                            variant='ghost'
                            size='icon'
                            onClick={handleSwap}
                            disabled={form.formState.isSubmitting}
                            aria-label={t('swap')}
                        >
                            <ArrowRightLeftIcon className='h-4 w-4' />
                        </Button>
                    </div>

                    {/* Destination Station */}
                    <FormField
                        control={form.control}
                        name='destinationStationId'
                        render={({ field, fieldState }) => (
                            <FormItem className='flex flex-col'>
                                <FormLabel>{t('destination')}</FormLabel>
                                <FormControl>
                                    <StationCombobox
                                        value={field.value}
                                        onValueChange={field.onChange}
                                        placeholder={t(
                                            'destinationPlaceholder',
                                        )}
                                        disabled={form.formState.isSubmitting}
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

                    {/* Departure Date */}
                    <FormField
                        control={form.control}
                        name='departureDate'
                        render={({ field, fieldState }) => (
                            <FormItem className='flex flex-col'>
                                <FormLabel>{t('departureDate')}</FormLabel>
                                <FormControl>
                                    <DatePickerInput
                                        value={
                                            field.value instanceof Date
                                                ? field.value
                                                : undefined
                                        }
                                        onChange={field.onChange}
                                        disabled={(date) => date < today}
                                        inputDisabled={
                                            form.formState.isSubmitting
                                        }
                                        placeholder={t('datePlaceholder')}
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

                    {/* Submit Button */}
                    <div className='flex items-end'>
                        <Button
                            type='submit'
                            className='w-full md:w-auto'
                            disabled={form.formState.isSubmitting}
                        >
                            {form.formState.isSubmitting ? (
                                <>
                                    <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                                    {t('searching')}
                                </>
                            ) : (
                                <>
                                    <SearchIcon className='mr-2 h-4 w-4' />
                                    {t('submit')}
                                </>
                            )}
                        </Button>
                    </div>
                </div>
            </form>
        </Form>
    );
}
