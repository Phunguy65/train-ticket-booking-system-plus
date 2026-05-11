type StationPlatformProps = {
    className?: string;
};

export function StationPlatform({ className }: StationPlatformProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 420 280'
            xmlns='http://www.w3.org/2000/svg'
        >
            <path
                d='M54 214h312'
                stroke='currentColor'
                strokeOpacity='.22'
                strokeWidth='10'
            />
            <path
                d='M74 235h272'
                stroke='currentColor'
                strokeOpacity='.34'
                strokeWidth='6'
            />
            <rect
                fill='var(--accent)'
                height='118'
                rx='22'
                width='280'
                x='70'
                y='72'
            />
            <path
                d='M92 72h236l-34-36H126L92 72Z'
                fill='var(--primary)'
                opacity='.9'
            />
            <path
                d='M122 94h54v68h-54zM202 94h96v54h-96z'
                fill='var(--background)'
                opacity='.82'
            />
            <path
                d='M214 108h72M214 124h72M134 108h30M134 126h30M134 144h30'
                stroke='currentColor'
                strokeOpacity='.25'
                strokeWidth='4'
            />
            <path d='M96 190h236' stroke='var(--primary)' strokeWidth='8' />
            <path
                d='M112 190v38M308 190v38'
                stroke='currentColor'
                strokeOpacity='.32'
                strokeWidth='6'
            />
            <circle
                cx='346'
                cy='58'
                fill='var(--primary)'
                opacity='.18'
                r='24'
            />
            <path
                d='M54 196h312'
                stroke='currentColor'
                strokeOpacity='.14'
                strokeWidth='6'
            />
        </svg>
    );
}
