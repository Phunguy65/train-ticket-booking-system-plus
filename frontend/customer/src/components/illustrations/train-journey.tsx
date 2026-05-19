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
            {/* Karst mountains background */}
            <path
                d='M0 180C20 140 40 100 70 90C100 80 110 130 140 120C170 110 180 70 210 60C240 50 260 100 290 90C320 80 340 50 370 60C400 70 420 110 420 110V280H0Z'
                fill='currentColor'
                opacity='.08'
            />
            <path
                d='M0 200C30 170 50 140 80 135C110 130 130 160 160 150C190 140 200 110 230 105C260 100 280 140 310 130C340 120 360 90 390 100C410 107 420 130 420 130V280H0Z'
                fill='var(--accent)'
                opacity='.2'
            />

            {/* Rice paddy terraces */}
            <path
                d='M0 230C60 225 120 220 180 222C240 224 300 218 360 220C390 221 420 225 420 225V280H0Z'
                fill='var(--accent)'
                opacity='.35'
            />
            <path
                d='M0 245C80 240 160 238 240 240C320 242 380 237 420 240V280H0Z'
                fill='var(--accent)'
                opacity='.5'
            />

            {/* Train tracks */}
            <path
                d='M40 210h340'
                stroke='currentColor'
                strokeOpacity='.2'
                strokeWidth='3'
            />
            <path
                d='M40 216h340'
                stroke='currentColor'
                strokeOpacity='.2'
                strokeWidth='3'
            />
            <path
                d='M60 210v6M90 210v6M120 210v6M150 210v6M180 210v6M210 210v6M240 210v6M270 210v6M300 210v6M330 210v6M360 210v6'
                stroke='currentColor'
                strokeOpacity='.15'
                strokeWidth='4'
            />

            {/* Train body - Vietnamese red */}
            <rect
                fill='var(--primary)'
                height='50'
                rx='12'
                width='180'
                x='120'
                y='158'
            />
            {/* Train nose */}
            <path
                d='M285 162h20c14 0 24 10 24 24v16h-44v-40Z'
                fill='var(--primary)'
                opacity='.85'
            />
            <path
                d='M325 175a4 4 0 014 4v10a4 4 0 01-4 4h-2a4 4 0 01-4-4v-10a4 4 0 014-4h2Z'
                fill='var(--secondary)'
            />

            {/* Windows with gold trim */}
            <rect
                fill='var(--primary-foreground)'
                height='22'
                opacity='.9'
                rx='5'
                width='34'
                x='135'
                y='168'
            />
            <rect
                fill='var(--secondary)'
                height='1.5'
                opacity='.6'
                rx='1'
                width='38'
                x='133'
                y='165'
            />
            <rect
                fill='var(--primary-foreground)'
                height='22'
                opacity='.9'
                rx='5'
                width='34'
                x='180'
                y='168'
            />
            <rect
                fill='var(--secondary)'
                height='1.5'
                opacity='.6'
                rx='1'
                width='38'
                x='178'
                y='165'
            />
            <rect
                fill='var(--primary-foreground)'
                height='22'
                opacity='.9'
                rx='5'
                width='34'
                x='225'
                y='168'
            />
            <rect
                fill='var(--secondary)'
                height='1.5'
                opacity='.6'
                rx='1'
                width='38'
                x='223'
                y='165'
            />

            {/* Wheels */}
            <circle cx='155' cy='210' fill='currentColor' opacity='.6' r='10' />
            <circle cx='155' cy='210' fill='var(--background)' r='4' />
            <circle cx='270' cy='210' fill='currentColor' opacity='.6' r='10' />
            <circle cx='270' cy='210' fill='var(--background)' r='4' />

            {/* Lotus flowers foreground */}
            <g opacity='.7'>
                {/* Lotus 1 */}
                <ellipse
                    cx='60'
                    cy='258'
                    fill='var(--accent)'
                    opacity='.5'
                    rx='18'
                    ry='8'
                />
                <path
                    d='M60 250C60 250 55 244 55 240C55 236 58 234 60 233C62 234 65 236 65 240C65 244 60 250 60 250Z'
                    fill='var(--primary)'
                    opacity='.7'
                />
                <path
                    d='M60 250C60 250 50 246 48 242C46 238 48 235 50 234C52 236 54 238 56 241C58 244 60 250 60 250Z'
                    fill='var(--primary)'
                    opacity='.5'
                />
                <path
                    d='M60 250C60 250 70 246 72 242C74 238 72 235 70 234C68 236 66 238 64 241C62 244 60 250 60 250Z'
                    fill='var(--primary)'
                    opacity='.5'
                />

                {/* Lotus 2 */}
                <ellipse
                    cx='380'
                    cy='252'
                    fill='var(--accent)'
                    opacity='.5'
                    rx='14'
                    ry='6'
                />
                <path
                    d='M380 245C380 245 376 240 376 237C376 234 378 232 380 231C382 232 384 234 384 237C384 240 380 245 380 245Z'
                    fill='var(--primary)'
                    opacity='.6'
                />
                <path
                    d='M380 245C380 245 372 242 371 239C370 236 372 234 374 233C375 235 377 237 378 239C379 241 380 245 380 245Z'
                    fill='var(--primary)'
                    opacity='.4'
                />
                <path
                    d='M380 245C380 245 388 242 389 239C390 236 388 234 386 233C385 235 383 237 382 239C381 241 380 245 380 245Z'
                    fill='var(--primary)'
                    opacity='.4'
                />

                {/* Lotus 3 - small bud */}
                <ellipse
                    cx='100'
                    cy='265'
                    fill='var(--accent)'
                    opacity='.4'
                    rx='10'
                    ry='5'
                />
                <path
                    d='M100 260C100 260 97 256 97 254C97 252 99 251 100 250C101 251 103 252 103 254C103 256 100 260 100 260Z'
                    fill='var(--primary)'
                    opacity='.5'
                />
            </g>

            {/* Gold accent line on train */}
            <path
                d='M120 195h180'
                stroke='var(--secondary)'
                strokeOpacity='.6'
                strokeWidth='2'
            />
        </svg>
    );
}
