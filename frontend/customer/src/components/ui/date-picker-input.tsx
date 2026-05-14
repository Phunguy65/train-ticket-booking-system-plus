'use client';

import { format, isValid, parse } from 'date-fns';
import { CalendarIcon } from 'lucide-react';
import type { ChangeEvent, KeyboardEvent } from 'react';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button.tsx';
import { Calendar } from '@/components/ui/calendar.tsx';
import { Input } from '@/components/ui/input.tsx';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover.tsx';
import { cn } from '@/lib/utils.ts';

const DATE_FORMAT = 'dd/MM/yyyy';
const DATE_PATTERN = /^\d{2}\/\d{2}\/\d{4}$/;
const DATE_DIGIT_LIMIT = 8;

type DatePickerInputProps = {
    value: Date | undefined | null;
    onChange: (date: Date | null) => void;
    disabled?: boolean | ((date: Date) => boolean);
    inputDisabled?: boolean;
    fromYear?: number;
    toYear?: number;
    placeholder?: string;
    className?: string;
};

function formatDate(value: Date | undefined | null) {
    return value ? format(value, DATE_FORMAT) : '';
}

function isSameFormattedDate(input: string, date: Date) {
    return format(date, DATE_FORMAT) === input;
}

function formatDateInput(value: string) {
    const digits = value.replace(/\D/g, '').slice(0, DATE_DIGIT_LIMIT);
    const day = digits.slice(0, 2);
    const month = digits.slice(2, 4);
    const year = digits.slice(4);

    if (digits.length >= 4) return `${day}/${month}/${year}`;
    if (digits.length >= 2) return `${day}/${month}`;

    return day;
}

export function DatePickerInput({
    value,
    onChange,
    disabled,
    inputDisabled,
    fromYear,
    toYear,
    placeholder,
    className,
}: DatePickerInputProps) {
    const selectedDate = value ?? undefined;
    const [open, setOpen] = useState(false);
    const [month, setMonth] = useState<Date | undefined>(selectedDate);
    const [inputValue, setInputValue] = useState(formatDate(selectedDate));
    const [invalid, setInvalid] = useState(false);
    const isInputDisabled = inputDisabled ?? disabled === true;
    const disabledDates = typeof disabled === 'function' ? disabled : undefined;

    useEffect(() => {
        setInputValue(formatDate(selectedDate));
        setMonth(selectedDate);
        setInvalid(false);
    }, [selectedDate]);

    const isSelectableDate = (date: Date) => !disabledDates?.(date);

    const getValidDate = (input: string) => {
        if (!DATE_PATTERN.test(input)) return null;

        const parsedDate = parse(input, DATE_FORMAT, new Date());

        if (!isValid(parsedDate) || !isSameFormattedDate(input, parsedDate)) {
            return null;
        }

        if (!isSelectableDate(parsedDate)) return null;

        return parsedDate;
    };

    const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
        const nextValue = formatDateInput(event.target.value);
        setInputValue(nextValue);

        if (!nextValue) {
            setInvalid(false);
            return;
        }

        if (nextValue.length < DATE_FORMAT.length) {
            setInvalid(false);
            return;
        }

        const parsedDate = getValidDate(nextValue);

        if (!parsedDate) {
            setInvalid(true);
            return;
        }

        setInvalid(false);
        setMonth(parsedDate);
        onChange(parsedDate);
    };

    const handleInputBlur = () => {
        if (!inputValue) {
            setInputValue(formatDate(selectedDate));
            setInvalid(false);
            return;
        }

        if (inputValue.length < DATE_FORMAT.length) {
            setInputValue(formatDate(selectedDate));
            setInvalid(false);
            return;
        }

        if (!getValidDate(inputValue)) {
            setInvalid(true);
            return;
        }

        setInputValue(formatDate(selectedDate));
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key !== 'Enter') return;
        if (!inputValue) return;
        if (inputValue.length === DATE_FORMAT.length && !invalid) return;

        event.preventDefault();

        if (inputValue.length <= DATE_FORMAT.length) {
            setInvalid(true);
        }
    };

    const handleSelect = (date: Date | undefined) => {
        const nextDate = date ?? null;
        onChange(nextDate);
        setInputValue(formatDate(nextDate));
        setMonth(nextDate ?? undefined);
        setInvalid(false);
        setOpen(false);
    };

    const maskStart = inputValue.length;

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <div className={cn('relative w-full', className)}>
                <Input
                    value={inputValue}
                    onChange={handleInputChange}
                    onBlur={handleInputBlur}
                    onKeyDown={handleKeyDown}
                    placeholder=''
                    disabled={isInputDisabled}
                    className='h-8 rounded-lg border-border bg-background pr-9 font-normal dark:border-input dark:bg-input/30'
                    inputMode='numeric'
                    autoComplete='off'
                    maxLength={DATE_FORMAT.length}
                    aria-label={placeholder ?? DATE_FORMAT}
                    aria-invalid={invalid}
                />
                <span
                    aria-hidden='true'
                    className='pointer-events-none absolute top-0 left-0 flex h-8 select-none items-center px-2.5 pr-9 font-normal text-base md:text-sm'
                >
                    <span className='text-transparent'>
                        {DATE_FORMAT.slice(0, maskStart)}
                    </span>
                    <span className='text-muted-foreground/50'>
                        {DATE_FORMAT.slice(maskStart)}
                    </span>
                </span>
                <PopoverTrigger asChild>
                    <Button
                        type='button'
                        variant='ghost'
                        size='icon'
                        disabled={isInputDisabled}
                        className='absolute top-0 right-0 h-8 rounded-l-none px-2 text-muted-foreground hover:text-foreground'
                        aria-label={placeholder ?? DATE_FORMAT}
                    >
                        <CalendarIcon className='h-4 w-4' />
                    </Button>
                </PopoverTrigger>
            </div>
            <PopoverContent className='w-auto p-0' align='start'>
                <Calendar
                    mode='single'
                    selected={selectedDate}
                    onSelect={handleSelect}
                    disabled={disabledDates}
                    month={month}
                    onMonthChange={setMonth}
                    defaultMonth={selectedDate}
                    fromYear={fromYear}
                    toYear={toYear}
                    captionLayout='dropdown'
                    initialFocus
                />
            </PopoverContent>
        </Popover>
    );
}
