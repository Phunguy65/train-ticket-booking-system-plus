'use client';

import { useTranslations } from 'next-intl';
import { useCallback, useMemo } from 'react';
import { PassengerForm, type PassengerFormData } from './passenger-form.tsx';

export type SeatInfo = {
    id: string;
    seatNumber: string;
};

type PassengerListProps = {
    /** List of selected seats with their labels */
    seats: SeatInfo[];
    /** Current passenger data for all seats */
    passengers: PassengerFormData[];
    /** Disabled state */
    disabled?: boolean;
    /** Called when passenger data changes */
    onChange: (passengers: PassengerFormData[]) => void;
};

/**
 * Renders one PassengerForm per selected seat and validates duplicate
 * ID document numbers across the full list.
 */
export function PassengerList({
    seats,
    passengers,
    disabled,
    onChange,
}: PassengerListProps) {
    const t = useTranslations('Passenger');

    // Find duplicate ID document numbers
    const duplicateIdDocuments = useMemo(() => {
        const ids = passengers
            .map((p) => p.idDocumentNumber)
            .filter((id) => id && id.trim() !== '');

        const seen = new Set<string>();
        const duplicates = new Set<string>();

        for (const id of ids) {
            if (seen.has(id)) {
                duplicates.add(id);
            }
            seen.add(id);
        }

        return duplicates;
    }, [passengers]);

    const handlePassengerChange = useCallback(
        (index: number, data: PassengerFormData) => {
            const newPassengers = [...passengers];
            newPassengers[index] = data;
            onChange(newPassengers);
        },
        [passengers, onChange],
    );

    return (
        <div className='space-y-4'>
            <h2 className='text-lg font-semibold'>{t('sectionTitle')}</h2>
            <div className='space-y-4'>
                {seats.map((seat, index) => {
                    const passengerData = passengers[index] || {
                        seatId: seat.id,
                        fullName: '',
                        idDocumentNumber: '',
                        dateOfBirth: null,
                        gender: '',
                    };

                    const hasDuplicateId =
                        passengerData.idDocumentNumber
                        && passengerData.idDocumentNumber.trim() !== ''
                        && duplicateIdDocuments.has(
                            passengerData.idDocumentNumber,
                        );

                    return (
                        <PassengerForm
                            key={seat.id}
                            seatId={seat.id}
                            seatLabel={seat.seatNumber}
                            data={passengerData}
                            index={index}
                            duplicateIdError={hasDuplicateId}
                            disabled={disabled}
                            onChange={(data) =>
                                handlePassengerChange(index, data)
                            }
                        />
                    );
                })}
            </div>
            {duplicateIdDocuments.size > 0 && (
                <p className='text-sm text-destructive'>
                    {t('duplicateIdWarning')}
                </p>
            )}
        </div>
    );
}

/**
 * Check if all passenger forms are valid (all required fields filled).
 */
export function isPassengerListValid(passengers: PassengerFormData[]): boolean {
    if (passengers.length === 0) return false;

    // Check all required fields are filled
    for (const p of passengers) {
        if (!p.fullName || p.fullName.trim() === '') return false;
        if (!p.idDocumentNumber || p.idDocumentNumber.trim() === '')
            return false;
        if (!p.dateOfBirth) return false;
        if (!p.gender || p.gender.trim() === '') return false;
    }

    // Check for duplicate ID documents
    const ids = passengers.map((p) => p.idDocumentNumber.trim());
    const uniqueIds = new Set(ids);
    if (uniqueIds.size !== ids.length) return false;

    return true;
}

/**
 * Initialize passenger data for selected seats.
 */
export function initializePassengers(seats: SeatInfo[]): PassengerFormData[] {
    return seats.map((seat) => ({
        seatId: seat.id,
        fullName: '',
        idDocumentNumber: '',
        dateOfBirth: null,
        gender: '',
    }));
}
