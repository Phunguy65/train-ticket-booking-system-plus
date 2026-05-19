'use client';

import { useTranslations } from 'next-intl';
import type { ChangeEvent } from 'react';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { DatePickerInput } from '@/components/ui/date-picker-input.tsx';
import { Input } from '@/components/ui/input.tsx';
import { Label } from '@/components/ui/label.tsx';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select.tsx';

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
                        <DatePickerInput
                            value={data.dateOfBirth ?? undefined}
                            onChange={(date) =>
                                handleFieldChange('dateOfBirth', date)
                            }
                            disabled={(date) =>
                                date > new Date()
                                || date < new Date('1900-01-01')
                            }
                            inputDisabled={disabled}
                            fromYear={1900}
                            toYear={new Date().getFullYear()}
                            placeholder={tProfile('selectDate')}
                        />
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
