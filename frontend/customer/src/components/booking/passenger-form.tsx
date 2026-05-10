'use client';

import { format } from 'date-fns';
import { CalendarIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import type { ChangeEvent } from 'react';
import { Button } from '@/components/ui/button.tsx';
import { Calendar } from '@/components/ui/calendar.tsx';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
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
import { cn } from '@/lib/utils.ts';

export type PassengerFormData = {
    seatId: string;
    fullName: string;
    idDocumentNumber: string;
    dateOfBirth: Date | null;
    gender: string;
};

type PassengerFormProps = {
    /** Seat ID this passenger is assigned to */
    seatId: string;
    /** Seat label for display (e.g., "12A") */
    seatLabel: string;
    /** Current form data */
    data: PassengerFormData;
    /** Passenger index (0-based) for form IDs */
    index: number;
    /** Whether there's a duplicate ID document error */
    duplicateIdError?: boolean;
    /** Disabled state */
    disabled?: boolean;
    /** Called when any field changes */
    onChange: (data: PassengerFormData) => void;
};

export function PassengerForm({
    seatLabel,
    data,
    index,
    duplicateIdError,
    disabled,
    onChange,
}: PassengerFormProps) {
    const t = useTranslations('Passenger');
    const tProfile = useTranslations('Profile');

    const handleFieldChange = (
        field: keyof PassengerFormData,
        value: string | Date | null,
    ) => {
        onChange({ ...data, [field]: value });
    };

    const idPrefix = `passenger-${index}`;

    return (
        <Card>
            <CardHeader className='pb-3'>
                <CardTitle className='text-base'>
                    {t('seatLabel', { seat: seatLabel })}
                </CardTitle>
            </CardHeader>
            <CardContent className='space-y-4'>
                {/* Full Name */}
                <div className='space-y-2'>
                    <Label htmlFor={`${idPrefix}-fullName`}>
                        {t('fullName')}
                    </Label>
                    <Input
                        id={`${idPrefix}-fullName`}
                        placeholder={t('fullNamePlaceholder')}
                        value={data.fullName}
                        onChange={(e: ChangeEvent<HTMLInputElement>) =>
                            handleFieldChange('fullName', e.target.value)
                        }
                        disabled={disabled}
                        aria-invalid={!data.fullName}
                    />
                </div>

                {/* ID Document Number */}
                <div className='space-y-2'>
                    <Label htmlFor={`${idPrefix}-idDocument`}>
                        {t('idDocumentNumber')}
                    </Label>
                    <Input
                        id={`${idPrefix}-idDocument`}
                        placeholder={t('idDocumentPlaceholder')}
                        value={data.idDocumentNumber}
                        onChange={(e: ChangeEvent<HTMLInputElement>) =>
                            handleFieldChange(
                                'idDocumentNumber',
                                e.target.value,
                            )
                        }
                        disabled={disabled}
                        aria-invalid={
                            !data.idDocumentNumber || duplicateIdError
                        }
                    />
                    {duplicateIdError && (
                        <p className='text-sm text-destructive'>
                            {t('duplicateIdDocument')}
                        </p>
                    )}
                </div>

                <div className='grid gap-4 sm:grid-cols-2'>
                    {/* Date of Birth */}
                    <div className='space-y-2'>
                        <Label>{t('dateOfBirth')}</Label>
                        <Popover>
                            <PopoverTrigger asChild>
                                <Button
                                    variant='outline'
                                    disabled={disabled}
                                    className={cn(
                                        'w-full justify-start text-left font-normal',
                                        !data.dateOfBirth
                                            && 'text-muted-foreground',
                                    )}
                                >
                                    <CalendarIcon className='mr-2 h-4 w-4' />
                                    {data.dateOfBirth ? (
                                        format(data.dateOfBirth, 'PPP')
                                    ) : (
                                        <span>{tProfile('selectDate')}</span>
                                    )}
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent
                                className='w-auto p-0'
                                align='start'
                            >
                                <Calendar
                                    mode='single'
                                    selected={data.dateOfBirth ?? undefined}
                                    onSelect={(date) =>
                                        handleFieldChange(
                                            'dateOfBirth',
                                            date ?? null,
                                        )
                                    }
                                    disabled={(date) =>
                                        date > new Date()
                                        || date < new Date('1900-01-01')
                                    }
                                    defaultMonth={
                                        data.dateOfBirth ?? new Date(1990, 0, 1)
                                    }
                                    fromYear={1900}
                                    toYear={new Date().getFullYear()}
                                    captionLayout='dropdown-months'
                                />
                            </PopoverContent>
                        </Popover>
                    </div>

                    {/* Gender */}
                    <div className='space-y-2'>
                        <Label>{t('gender')}</Label>
                        <Select
                            value={data.gender}
                            onValueChange={(value) =>
                                handleFieldChange('gender', value)
                            }
                            disabled={disabled}
                        >
                            <SelectTrigger className='w-full'>
                                <SelectValue
                                    placeholder={tProfile('selectGender')}
                                />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value='male'>
                                    {tProfile('genderOptions.male')}
                                </SelectItem>
                                <SelectItem value='female'>
                                    {tProfile('genderOptions.female')}
                                </SelectItem>
                                <SelectItem value='other'>
                                    {tProfile('genderOptions.other')}
                                </SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
