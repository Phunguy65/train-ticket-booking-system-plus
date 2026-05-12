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
            {/* Station building - Ga Hà Nội inspired */}
            <rect
                fill='var(--accent)'
                opacity='.15'
                height='130'
                rx='4'
                width='240'
                x='90'
                y='90'
            />

            {/* Pointed triangular roof - iconic Ga Hà Nội */}
            <path
                d='M90 90L210 30L330 90Z'
                fill='var(--primary)'
                opacity='.85'
            />
            <path
                d='M130 90L210 50L290 90Z'
                fill='var(--primary)'
                opacity='.6'
            />

            {/* Clock tower */}
            <rect
                fill='var(--primary)'
                opacity='.7'
                height='20'
                width='16'
                x='202'
                y='45'
                rx='2'
            />
            <circle
                cx='210'
                cy='55'
                r='6'
                stroke='var(--primary-foreground)'
                strokeWidth='1.5'
                fill='none'
            />
            <path
                d='M210 52v3l2 2'
                stroke='var(--primary-foreground)'
                strokeWidth='1'
                strokeLinecap='round'
            />

            {/* Arched windows */}
            <g opacity='.85'>
                <path
                    d='M120 120a20 20 0 0140 0v30h-40z'
                    fill='var(--background)'
                    opacity='.8'
                />
                <path
                    d='M170 120a20 20 0 0140 0v30h-40z'
                    fill='var(--background)'
                    opacity='.8'
                />
                <path
                    d='M220 120a20 20 0 0140 0v30h-40z'
                    fill='var(--background)'
                    opacity='.8'
                />
                <path
                    d='M270 120a20 20 0 0140 0v30h-40z'
                    fill='var(--background)'
                    opacity='.8'
                />
            </g>

            {/* Window details */}
            <path
                d='M140 125v25M190 125v25M240 125v25M290 125v25'
                stroke='currentColor'
                strokeOpacity='.15'
                strokeWidth='1'
            />

            {/* Platform canopy */}
            <rect
                fill='var(--primary)'
                height='6'
                width='300'
                x='60'
                y='220'
                rx='2'
            />
            <path
                d='M80 220v40M340 220v40'
                stroke='currentColor'
                strokeOpacity='.3'
                strokeWidth='4'
            />

            {/* Lantern 1 */}
            <g opacity='.8'>
                <line
                    x1='140'
                    y1='220'
                    x2='140'
                    y2='200'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
                <ellipse
                    cx='140'
                    cy='205'
                    rx='8'
                    ry='12'
                    fill='var(--secondary)'
                    opacity='.7'
                />
                <ellipse
                    cx='140'
                    cy='205'
                    rx='5'
                    ry='8'
                    fill='var(--primary)'
                    opacity='.4'
                />
                <path
                    d='M135 195h10'
                    stroke='var(--secondary)'
                    strokeWidth='1.5'
                />
                <path
                    d='M138 217h4'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
            </g>

            {/* Lantern 2 */}
            <g opacity='.8'>
                <line
                    x1='280'
                    y1='220'
                    x2='280'
                    y2='200'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
                <ellipse
                    cx='280'
                    cy='205'
                    rx='8'
                    ry='12'
                    fill='var(--secondary)'
                    opacity='.7'
                />
                <ellipse
                    cx='280'
                    cy='205'
                    rx='5'
                    ry='8'
                    fill='var(--primary)'
                    opacity='.4'
                />
                <path
                    d='M275 195h10'
                    stroke='var(--secondary)'
                    strokeWidth='1.5'
                />
                <path
                    d='M278 217h4'
                    stroke='var(--secondary)'
                    strokeWidth='1'
                />
            </g>

            {/* Platform tracks */}
            <path
                d='M40 260h340'
                stroke='currentColor'
                strokeOpacity='.25'
                strokeWidth='4'
            />
            <path
                d='M40 266h340'
                stroke='currentColor'
                strokeOpacity='.25'
                strokeWidth='4'
            />
            <path
                d='M60 260v6M90 260v6M120 260v6M150 260v6M180 260v6M210 260v6M240 260v6M270 260v6M300 260v6M330 260v6M360 260v6'
                stroke='currentColor'
                strokeOpacity='.15'
                strokeWidth='3'
            />

            {/* Station name plaque */}
            <rect
                fill='var(--secondary)'
                opacity='.3'
                height='14'
                width='80'
                x='170'
                y='165'
                rx='3'
            />

            {/* Cityscape silhouette background */}
            <path
                d='M0 180h30v-20h15v20h20v-35h12v35h25v-15h18v15h300v100H0z'
                fill='currentColor'
                opacity='.04'
            />
        </svg>
    );
}
