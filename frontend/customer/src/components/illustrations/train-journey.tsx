type TrainJourneyProps = {
    className?: string;
};

export function TrainJourney({ className }: TrainJourneyProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 420 280'
            xmlns='http://www.w3.org/2000/svg'
        >
            <path
                d='M40 198h340'
                stroke='currentColor'
                strokeOpacity='.18'
                strokeWidth='8'
            />
            <path
                d='M68 222h284'
                stroke='currentColor'
                strokeOpacity='.35'
                strokeWidth='6'
            />
            <path
                d='M90 238h244'
                stroke='currentColor'
                strokeOpacity='.18'
                strokeWidth='4'
            />
            <path
                d='M88 220l38 18M152 220l38 18M216 220l38 18M280 220l38 18'
                stroke='currentColor'
                strokeOpacity='.28'
                strokeWidth='5'
            />
            <path
                d='M42 174c38-42 72-62 104-62 38 0 54 30 92 30 34 0 58-48 104-70 18-8 32-10 42-10v112H42Z'
                fill='var(--accent)'
            />
            <path
                d='M62 158c36-30 62-44 92-44 34 0 52 24 84 24 24 0 44-20 68-42'
                stroke='currentColor'
                strokeOpacity='.2'
                strokeWidth='5'
            />
            <rect
                fill='var(--primary)'
                height='62'
                rx='20'
                width='230'
                x='86'
                y='126'
            />
            <path
                d='M300 140h24c18 0 32 14 32 32v16h-56v-48Z'
                fill='currentColor'
                opacity='.72'
            />
            <rect
                fill='var(--primary-foreground)'
                height='26'
                opacity='.9'
                rx='7'
                width='42'
                x='112'
                y='140'
            />
            <rect
                fill='var(--primary-foreground)'
                height='26'
                opacity='.9'
                rx='7'
                width='42'
                x='170'
                y='140'
            />
            <rect
                fill='var(--primary-foreground)'
                height='26'
                opacity='.9'
                rx='7'
                width='42'
                x='228'
                y='140'
            />
            <circle
                cx='130'
                cy='190'
                fill='currentColor'
                opacity='.65'
                r='14'
            />
            <circle
                cx='276'
                cy='190'
                fill='currentColor'
                opacity='.65'
                r='14'
            />
            <circle cx='130' cy='190' fill='var(--background)' r='6' />
            <circle cx='276' cy='190' fill='var(--background)' r='6' />
            <path
                d='M78 126h218'
                stroke='currentColor'
                strokeOpacity='.28'
                strokeWidth='6'
            />
            <circle
                cx='92'
                cy='76'
                fill='var(--primary)'
                opacity='.18'
                r='24'
            />
        </svg>
    );
}
