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
            <path
                d='M38 82c0-13 10-24 24-24 4-18 20-31 40-31 22 0 40 16 43 37 16 1 29 14 29 30H38V82Z'
                fill='currentColor'
                opacity='.16'
            />
            <path
                d='M176 58c0-10 8-19 19-19 4-14 17-24 32-24 18 0 32 13 35 30 13 1 23 11 23 24H176V58Z'
                fill='currentColor'
                opacity='.24'
            />
            <path
                d='M108 112c0-8 7-15 15-15 3-11 13-18 25-18 14 0 25 10 27 23 10 1 18 9 18 19h-85v-9Z'
                fill='currentColor'
                opacity='.2'
            />
        </svg>
    );
}
