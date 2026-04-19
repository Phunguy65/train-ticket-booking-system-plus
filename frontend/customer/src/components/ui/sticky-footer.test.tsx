import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StickyFooter, StickyFooterSpacer } from './sticky-footer.tsx';

describe('StickyFooter', () => {
    it('renders children', () => {
        render(
            <StickyFooter>
                <button>Action Button</button>
            </StickyFooter>,
        );

        expect(
            screen.getByRole('button', { name: 'Action Button' }),
        ).toBeInTheDocument();
    });

    it('applies mobile-only class by default', () => {
        const { container } = render(
            <StickyFooter>
                <span>Content</span>
            </StickyFooter>,
        );

        const footer = container.querySelector('[data-slot="sticky-footer"]');
        expect(footer).toHaveClass('lg:hidden');
    });

    it('does not apply mobile-only class when mobileOnly is false', () => {
        const { container } = render(
            <StickyFooter mobileOnly={false}>
                <span>Content</span>
            </StickyFooter>,
        );

        const footer = container.querySelector('[data-slot="sticky-footer"]');
        expect(footer).not.toHaveClass('lg:hidden');
    });

    it('applies custom className', () => {
        const { container } = render(
            <StickyFooter className='custom-class'>
                <span>Content</span>
            </StickyFooter>,
        );

        const footer = container.querySelector('[data-slot="sticky-footer"]');
        expect(footer).toHaveClass('custom-class');
    });
});

describe('StickyFooterSpacer', () => {
    it('renders with aria-hidden', () => {
        const { container } = render(<StickyFooterSpacer />);

        const spacer = container.querySelector(
            '[data-slot="sticky-footer-spacer"]',
        );
        expect(spacer).toHaveAttribute('aria-hidden', 'true');
    });

    it('applies mobile-only class by default', () => {
        const { container } = render(<StickyFooterSpacer />);

        const spacer = container.querySelector(
            '[data-slot="sticky-footer-spacer"]',
        );
        expect(spacer).toHaveClass('lg:hidden');
    });

    it('does not apply mobile-only class when mobileOnly is false', () => {
        const { container } = render(<StickyFooterSpacer mobileOnly={false} />);

        const spacer = container.querySelector(
            '[data-slot="sticky-footer-spacer"]',
        );
        expect(spacer).not.toHaveClass('lg:hidden');
    });
});
