type RicePaddyHeroProps = {
    className?: string;
};

export function RicePaddyHero({ className }: RicePaddyHeroProps) {
    return (
        <svg
            aria-hidden='true'
            className={className}
            fill='none'
            viewBox='0 0 1200 400'
            xmlns='http://www.w3.org/2000/svg'
        >
            {/* Far background - karst mountains */}
            <path
                d='M0 250C40 180 80 120 120 100C160 80 180 130 220 110C260 90 280 50 340 40C400 30 420 80 480 70C540 60 560 30 620 25C680 20 700 60 760 50C820 40 840 20 900 30C960 40 1000 80 1040 70C1080 60 1120 90 1160 100C1180 105 1200 120 1200 120V400H0Z'
                fill='currentColor'
                opacity='.06'
            />

            {/* Mid background - softer mountains */}
            <path
                d='M0 300C60 260 100 220 160 210C220 200 260 240 320 230C380 220 400 180 460 175C520 170 560 200 620 190C680 180 720 160 780 165C840 170 880 200 940 195C1000 190 1040 170 1100 180C1140 187 1180 210 1200 220V400H0Z'
                fill='currentColor'
                opacity='.04'
            />

            {/* Train on viaduct */}
            <g opacity='.12'>
                {/* Bridge pillars */}
                <rect
                    x='400'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='450'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='500'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='550'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='600'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='650'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='700'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                <rect
                    x='750'
                    y='200'
                    width='6'
                    height='60'
                    fill='currentColor'
                />
                {/* Bridge deck */}
                <rect
                    x='395'
                    y='195'
                    width='365'
                    height='8'
                    fill='currentColor'
                    rx='2'
                />
                {/* Train */}
                <rect
                    x='480'
                    y='180'
                    width='120'
                    height='16'
                    fill='var(--primary)'
                    rx='6'
                    opacity='.8'
                />
                <rect
                    x='595'
                    y='182'
                    width='30'
                    height='14'
                    fill='var(--primary)'
                    rx='4'
                    opacity='.6'
                />
            </g>

            {/* Foreground - terraced rice paddies */}
            <path
                d='M0 320C100 310 200 305 300 308C400 311 500 315 600 312C700 309 800 305 900 308C1000 311 1100 315 1200 318V400H0Z'
                fill='var(--accent)'
                opacity='.1'
            />
            <path
                d='M0 340C80 335 160 332 240 334C320 336 400 340 480 338C560 336 640 332 720 334C800 336 880 340 960 338C1040 336 1120 334 1200 336V400H0Z'
                fill='var(--accent)'
                opacity='.08'
            />
            <path
                d='M0 360C120 355 240 352 360 354C480 356 600 360 720 358C840 356 960 354 1080 356C1140 357 1200 358 1200 358V400H0Z'
                fill='var(--accent)'
                opacity='.06'
            />

            {/* Water reflections in paddies */}
            <path
                d='M100 345h80M300 350h60M500 342h90M750 348h70M950 345h80'
                stroke='currentColor'
                strokeOpacity='.03'
                strokeWidth='2'
            />

            {/* Small figure with nón lá */}
            <g opacity='.08'>
                <path d='M250 348l4-6h-8z' fill='currentColor' />
                <rect
                    x='251'
                    y='348'
                    width='2'
                    height='8'
                    fill='currentColor'
                />
            </g>
        </svg>
    );
}
