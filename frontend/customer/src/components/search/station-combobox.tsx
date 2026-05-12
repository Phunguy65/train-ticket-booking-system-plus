'use client';

import { useQuery } from '@tanstack/react-query';
import { CheckIcon, ChevronsUpDownIcon, Loader2Icon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button.tsx';
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from '@/components/ui/command.tsx';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover.tsx';
import { searchStationsOptions } from '@/lib/api/index.ts';
import { showApiErrorToast } from '@/lib/toast.ts';
import { cn } from '@/lib/utils.ts';

type StationComboboxProps = {
    value: string;
    onValueChange: (value: string) => void;
    placeholder: string;
    disabled?: boolean;
};

export function StationCombobox({
    value,
    onValueChange,
    placeholder,
    disabled,
}: StationComboboxProps) {
    const t = useTranslations('Search');
    const tCommon = useTranslations('Common');
    const tErrors = useTranslations('Errors');
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');

    const {
        data: stations,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...searchStationsOptions({
            query: {
                q: search || undefined,
                limit: 10,
            },
        }),
        enabled: open,
        staleTime: 30 * 1000,
    });

    // Show toast on error (in effect to avoid render-time side effects)
    useEffect(() => {
        if (isError && error) {
            showApiErrorToast(error, {
                fail: tErrors('validationError'),
                network: tErrors('networkError'),
                unknown: tErrors('unknownError'),
            });
        }
    }, [isError, error, tErrors]);

    const selectedStation = stations?.find((s) => s.id === value);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant='outline'
                    role='combobox'
                    aria-expanded={open}
                    className={cn(
                        'w-full justify-between',
                        !value && 'text-muted-foreground',
                    )}
                    disabled={disabled}
                >
                    {selectedStation
                        ? `${selectedStation.name} (${selectedStation.code})`
                        : value
                          ? value
                          : placeholder}
                    <ChevronsUpDownIcon className='ml-2 h-4 w-4 shrink-0 opacity-50' />
                </Button>
            </PopoverTrigger>
            <PopoverContent className='w-[300px] p-0' align='start'>
                <Command shouldFilter={false}>
                    <CommandInput
                        placeholder={placeholder}
                        value={search}
                        onValueChange={setSearch}
                    />
                    <CommandList>
                        {isLoading ? (
                            <div className='flex items-center justify-center py-6'>
                                <Loader2Icon className='h-4 w-4 motion-safe:animate-spin' />
                                <span className='ml-2 text-sm text-muted-foreground'>
                                    {t('loadingStations')}
                                </span>
                            </div>
                        ) : isError ? (
                            <div className='flex flex-col items-center gap-2 py-6'>
                                <span className='text-sm text-muted-foreground'>
                                    {t('stationSearchError')}
                                </span>
                                <Button
                                    variant='ghost'
                                    size='sm'
                                    onClick={() => refetch()}
                                >
                                    {tCommon('retry')}
                                </Button>
                            </div>
                        ) : (
                            <>
                                <CommandEmpty>
                                    {t('noStationsFound')}
                                </CommandEmpty>
                                <CommandGroup>
                                    {stations?.map((station) => (
                                        <CommandItem
                                            key={station.id}
                                            value={station.id}
                                            onSelect={() => {
                                                onValueChange(
                                                    station.id === value
                                                        ? ''
                                                        : (station.id ?? ''),
                                                );
                                                setOpen(false);
                                            }}
                                        >
                                            <CheckIcon
                                                className={cn(
                                                    'mr-2 h-4 w-4',
                                                    value === station.id
                                                        ? 'opacity-100'
                                                        : 'opacity-0',
                                                )}
                                            />
                                            <div className='flex flex-col'>
                                                <span>{station.name}</span>
                                                <span className='text-xs text-muted-foreground'>
                                                    {station.code} -{' '}
                                                    {station.city}
                                                </span>
                                            </div>
                                        </CommandItem>
                                    ))}
                                </CommandGroup>
                            </>
                        )}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}
