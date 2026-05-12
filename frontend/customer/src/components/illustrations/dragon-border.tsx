type DragonBorderProps = {
    className?: string;
};

export function DragonBorder({ className }: DragonBorderProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 200 40'
            xmlns='http://www.w3.org/2000/svg'
        >
            {/* Dragon body - flowing S-curve */}
            <path
                d='M10 20C30 8 50 32 70 20C90 8 110 32 130 20C150 8 170 32 190 20'
                stroke='var(--secondary)'
                strokeWidth='2.5'
                strokeLinecap='round'
                opacity='.5'
            />

            {/* Scale pattern along the curve */}
            <path
                d='M25 14c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />
            <path
                d='M55 26c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />
            <path
                d='M85 14c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />
            <path
                d='M115 26c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />
            <path
                d='M145 14c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />
            <path
                d='M175 26c2-1 4 0 4 2s-2 3-4 2-2-3 0-4Z'
                fill='var(--secondary)'
                opacity='.4'
            />

            {/* Dragon head (simplified) */}
            <circle
                cx='10'
                cy='20'
                r='4'
                fill='var(--secondary)'
                opacity='.6'
            />
            <circle cx='9' cy='18' r='1' fill='currentColor' opacity='.4' />

            {/* Tail flourish */}
            <path
                d='M190 20c3-2 6-1 8 1'
                stroke='var(--secondary)'
                strokeWidth='1.5'
                strokeLinecap='round'
                opacity='.4'
            />
        </svg>
    );
}
