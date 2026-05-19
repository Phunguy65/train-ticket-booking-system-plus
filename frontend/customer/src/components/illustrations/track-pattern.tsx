type TrackPatternProps = {
    className?: string;
};

export function TrackPattern({ className }: TrackPatternProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 360 80'
            xmlns='http://www.w3.org/2000/svg'
        >
            {/* Rail lines */}
            <path
                d='M16 28h328M16 52h328'
                stroke='currentColor'
                strokeLinecap='round'
                strokeOpacity='.2'
                strokeWidth='4'
            />

            {/* Cross-ties with lotus buds */}
            <path
                d='M50 24v32M110 24v32M170 24v32M230 24v32M290 24v32M350 24v32'
                stroke='currentColor'
                strokeLinecap='round'
                strokeOpacity='.12'
                strokeWidth='5'
            />

            {/* Lotus buds between ties */}
            <g opacity='.6'>
                {/* Bud 1 */}
                <path
                    d='M80 40C80 40 77 35 77 33C77 31 79 30 80 29C81 30 83 31 83 33C83 35 80 40 80 40Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <ellipse
                    cx='80'
                    cy='42'
                    rx='5'
                    ry='2'
                    fill='var(--accent)'
                    opacity='.5'
                />

                {/* Bud 2 */}
                <path
                    d='M140 40C140 40 137 35 137 33C137 31 139 30 140 29C141 30 143 31 143 33C143 35 140 40 140 40Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <ellipse
                    cx='140'
                    cy='42'
                    rx='5'
                    ry='2'
                    fill='var(--accent)'
                    opacity='.5'
                />

                {/* Bud 3 */}
                <path
                    d='M200 40C200 40 197 35 197 33C197 31 199 30 200 29C201 30 203 31 203 33C203 35 200 40 200 40Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <ellipse
                    cx='200'
                    cy='42'
                    rx='5'
                    ry='2'
                    fill='var(--accent)'
                    opacity='.5'
                />

                {/* Bud 4 */}
                <path
                    d='M260 40C260 40 257 35 257 33C257 31 259 30 260 29C261 30 263 31 263 33C263 35 260 40 260 40Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <ellipse
                    cx='260'
                    cy='42'
                    rx='5'
                    ry='2'
                    fill='var(--accent)'
                    opacity='.5'
                />

                {/* Bud 5 */}
                <path
                    d='M320 40C320 40 317 35 317 33C317 31 319 30 320 29C321 30 323 31 323 33C323 35 320 40 320 40Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <ellipse
                    cx='320'
                    cy='42'
                    rx='5'
                    ry='2'
                    fill='var(--accent)'
                    opacity='.5'
                />
            </g>
        </svg>
    );
}
