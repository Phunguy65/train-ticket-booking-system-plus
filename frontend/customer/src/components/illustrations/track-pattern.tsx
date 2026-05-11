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
            <path
                d='M16 22h328M16 58h328'
                stroke='currentColor'
                strokeLinecap='round'
                strokeOpacity='.28'
                strokeWidth='6'
            />
            <path
                d='M42 18l28 44M96 18l28 44M150 18l28 44M204 18l28 44M258 18l28 44M312 18l28 44'
                stroke='var(--primary)'
                strokeLinecap='round'
                strokeOpacity='.42'
                strokeWidth='5'
            />
        </svg>
    );
}
