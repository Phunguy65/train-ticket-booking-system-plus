'use client';

import { FilterIcon, SortAscIcon, SortDescIcon, XIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Button } from '@/components/ui/button.tsx';
import { Input } from '@/components/ui/input.tsx';
import { Label } from '@/components/ui/label.tsx';
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
import { Switch } from '@/components/ui/switch.tsx';
import type { TripSearchParams } from '@/lib/search-params.ts';

type TripFiltersProps = {
    filters: TripSearchParams;
    onFiltersChange: (filters: Partial<TripSearchParams>) => void;
};

export function TripFilters({ filters, onFiltersChange }: TripFiltersProps) {
    const t = useTranslations('Trips');

    const handleSortChange = (value: string) => {
        const [sortBy, sortDirection] = value.split('-') as [
            TripSearchParams['sortBy'],
            TripSearchParams['sortDirection'],
        ];
        onFiltersChange({ sortBy, sortDirection });
    };

    const currentSort = filters.sortBy
        ? `${filters.sortBy}-${filters.sortDirection || 'ASC'}`
        : 'DEPARTURE_TIME-ASC';

    const hasActiveFilters =
        filters.minPrice !== undefined
        || filters.maxPrice !== undefined
        || filters.availableOnly;

    const clearFilters = () => {
        onFiltersChange({
            minPrice: undefined,
            maxPrice: undefined,
            availableOnly: undefined,
        });
    };

    return (
        <div className='flex flex-wrap items-center gap-2'>
            {/* Sort Control */}
            <Select value={currentSort} onValueChange={handleSortChange}>
                <SelectTrigger className='w-[200px]'>
                    <div className='flex items-center gap-2'>
                        {filters.sortDirection === 'DESC' ? (
                            <SortDescIcon className='h-4 w-4' />
                        ) : (
                            <SortAscIcon className='h-4 w-4' />
                        )}
                        <SelectValue placeholder={t('sort.label')} />
                    </div>
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value='DEPARTURE_TIME-ASC'>
                        {t('sort.departureTime')} (A-Z)
                    </SelectItem>
                    <SelectItem value='DEPARTURE_TIME-DESC'>
                        {t('sort.departureTime')} (Z-A)
                    </SelectItem>
                    <SelectItem value='PRICE-ASC'>
                        {t('sort.price')} (Low-High)
                    </SelectItem>
                    <SelectItem value='PRICE-DESC'>
                        {t('sort.price')} (High-Low)
                    </SelectItem>
                    <SelectItem value='DURATION-ASC'>
                        {t('sort.duration')} (Short-Long)
                    </SelectItem>
                    <SelectItem value='DURATION-DESC'>
                        {t('sort.duration')} (Long-Short)
                    </SelectItem>
                </SelectContent>
            </Select>

            {/* Filter Popover */}
            <Popover>
                <PopoverTrigger asChild>
                    <Button variant='outline' className='gap-2'>
                        <FilterIcon className='h-4 w-4' />
                        {t('filter.label')}
                        {hasActiveFilters && (
                            <span className='flex h-5 w-5 items-center justify-center rounded-full bg-primary text-xs text-primary-foreground'>
                                !
                            </span>
                        )}
                    </Button>
                </PopoverTrigger>
                <PopoverContent className='w-80'>
                    <div className='space-y-4'>
                        <h4 className='font-medium'>{t('filter.label')}</h4>

                        {/* Price Range */}
                        <div className='space-y-2'>
                            <Label>{t('filter.priceRange')}</Label>
                            <div className='flex items-center gap-2'>
                                <Input
                                    type='number'
                                    placeholder={t('filter.minPrice')}
                                    value={filters.minPrice ?? ''}
                                    onChange={(e) =>
                                        onFiltersChange({
                                            minPrice: e.target.value
                                                ? parseInt(e.target.value, 10)
                                                : undefined,
                                        })
                                    }
                                    className='w-full'
                                />
                                <span className='text-muted-foreground'>-</span>
                                <Input
                                    type='number'
                                    placeholder={t('filter.maxPrice')}
                                    value={filters.maxPrice ?? ''}
                                    onChange={(e) =>
                                        onFiltersChange({
                                            maxPrice: e.target.value
                                                ? parseInt(e.target.value, 10)
                                                : undefined,
                                        })
                                    }
                                    className='w-full'
                                />
                            </div>
                        </div>

                        {/* Available Only */}
                        <div className='flex items-center justify-between'>
                            <Label htmlFor='available-only'>
                                {t('filter.availableOnly')}
                            </Label>
                            <Switch
                                id='available-only'
                                checked={filters.availableOnly ?? false}
                                onCheckedChange={(checked) =>
                                    onFiltersChange({
                                        availableOnly: checked || undefined,
                                    })
                                }
                            />
                        </div>

                        {/* Clear Filters */}
                        {hasActiveFilters && (
                            <Button
                                variant='ghost'
                                size='sm'
                                className='w-full'
                                onClick={clearFilters}
                            >
                                <XIcon className='mr-2 h-4 w-4' />
                                {t('filter.clear')}
                            </Button>
                        )}
                    </div>
                </PopoverContent>
            </Popover>
        </div>
    );
}
