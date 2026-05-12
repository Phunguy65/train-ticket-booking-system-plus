type CloudsDecorationProps = {
    className?: string;
};

export function CloudsDecoration({ className }: CloudsDecorationProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 320 140'
            xmlns='http://www.w3.org/2000/svg'
        >
            {/* Cloud 1 */}
            <path
                d='M40 70c0-12 10-22 22-22 4-10 14-18 26-18 16 0 28 12 30 27 8 2 14 10 14 19 0 11-9 20-20 20H52c-7 0-12-5-12-12v-1c0-7 5-13 12-13Z'
                fill='currentColor'
                opacity='.08'
            />

            {/* Cloud 2 */}
            <path
                d='M180 50c0-10 8-18 18-18 3-8 12-14 22-14 13 0 24 10 25 22 7 2 12 8 12 16 0 9-7 16-16 16h-48c-6 0-10-4-10-10v-1c0-6 4-11 10-11Z'
                fill='currentColor'
                opacity='.06'
            />

            {/* Cloud 3 */}
            <path
                d='M100 100c0-8 6-14 14-14 3-6 9-11 17-11 10 0 18 8 19 17 5 1 9 6 9 12 0 7-6 13-13 13H108c-5 0-8-3-8-8v-1c0-4 3-8 8-8Z'
                fill='currentColor'
                opacity='.05'
            />

            {/* Lantern 1 - Hội An style */}
            <g opacity='.75'>
                <line
                    x1='260'
                    y1='20'
                    x2='260'
                    y2='40'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
                <path
                    d='M252 45c0-4 3.5-8 8-8s8 4 8 8c0 6-3 12-8 14-5-2-8-8-8-14Z'
                    fill='var(--secondary)'
                    opacity='.7'
                />
                <path
                    d='M255 45c0-3 2.2-5 5-5s5 2 5 5c0 4-2 8-5 9-3-1-5-5-5-9Z'
                    fill='var(--primary)'
                    opacity='.35'
                />
                <path
                    d='M257 38h6'
                    stroke='var(--secondary)'
                    strokeWidth='1.5'
                    strokeLinecap='round'
                />
                <line
                    x1='259'
                    y1='59'
                    x2='261'
                    y2='63'
                    stroke='var(--secondary)'
                    strokeWidth='0.8'
                />
            </g>

            {/* Lantern 2 */}
            <g opacity='.6'>
                <line
                    x1='70'
                    y1='25'
                    x2='70'
                    y2='42'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
                <path
                    d='M63 46c0-3.5 3-7 7-7s7 3.5 7 7c0 5-2.5 10-7 12-4.5-2-7-7-7-12Z'
                    fill='var(--secondary)'
                    opacity='.6'
                />
                <path
                    d='M66 46c0-2.5 1.8-4.5 4-4.5s4 2 4 4.5c0 3.5-1.5 7-4 8-2.5-1-4-4.5-4-8Z'
                    fill='var(--primary)'
                    opacity='.3'
                />
                <path
                    d='M67 40h6'
                    stroke='var(--secondary)'
                    strokeWidth='1.5'
                    strokeLinecap='round'
                />
                <line
                    x1='69'
                    y1='58'
                    x2='71'
                    y2='61'
                    stroke='var(--secondary)'
                    strokeWidth='0.8'
                />
            </g>

            {/* Lantern 3 - smaller */}
            <g opacity='.5'>
                <line
                    x1='200'
                    y1='80'
                    x2='200'
                    y2='93'
                    stroke='var(--secondary)'
                    strokeWidth='0.8'
                />
                <path
                    d='M194 96c0-3 2.7-5.5 6-5.5s6 2.5 6 5.5c0 4-2 8-6 9.5-4-1.5-6-5.5-6-9.5Z'
                    fill='var(--secondary)'
                    opacity='.6'
                />
                <path
                    d='M197 91h6'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                    strokeLinecap='round'
                />
            </g>
        </svg>
    );
}
