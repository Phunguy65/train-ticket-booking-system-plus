'use client';

import {
    AlertCircleIcon,
    CheckCircle2Icon,
    ClockIcon,
    RefreshCwIcon,
    XCircleIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Badge } from '@/components/ui/badge.tsx';
import type { PaymentStatus } from '@/lib/customer-utils.ts';

type PaymentStatusBadgeProps = {
    status?: PaymentStatus | string;
};

export function PaymentStatusBadge({ status }: PaymentStatusBadgeProps) {
    const tStatus = useTranslations('Status');

    const config = getStatusConfig(status);

    return (
        <Badge variant={config.variant} className='gap-1'>
            <config.icon className='h-3 w-3' aria-hidden='true' />
            <span>{tStatus(status as PaymentStatus)}</span>
        </Badge>
    );
}

function getStatusConfig(status?: PaymentStatus | string): {
    variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'success';
    icon: typeof CheckCircle2Icon;
} {
    switch (status) {
        case 'PAID':
            return { variant: 'success', icon: CheckCircle2Icon };
        case 'PENDING':
            return { variant: 'secondary', icon: ClockIcon };
        case 'FAILED':
            return { variant: 'destructive', icon: XCircleIcon };
        case 'REFUNDED':
            return { variant: 'outline', icon: RefreshCwIcon };
        default:
            return { variant: 'outline', icon: AlertCircleIcon };
    }
}
