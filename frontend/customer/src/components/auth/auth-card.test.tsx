import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AuthCard } from './auth-card.tsx';

describe('AuthCard', () => {
    it('renders title and children content', () => {
        render(
            <AuthCard title='Test Title'>
                <p>Test content</p>
            </AuthCard>,
        );

        expect(screen.getByText('Test Title')).toBeInTheDocument();
        expect(screen.getByText('Test content')).toBeInTheDocument();
    });

    it('renders subtitle when provided', () => {
        render(
            <AuthCard title='Title' subtitle='Test subtitle'>
                <p>Content</p>
            </AuthCard>,
        );

        expect(screen.getByText('Test subtitle')).toBeInTheDocument();
    });

    it('does not render subtitle when not provided', () => {
        render(
            <AuthCard title='Title'>
                <p>Content</p>
            </AuthCard>,
        );

        const descriptions = document.querySelectorAll(
            '[data-slot="card-description"]',
        );
        expect(descriptions).toHaveLength(0);
    });

    it('renders footer when provided', () => {
        render(
            <AuthCard
                title='Title'
                footer={<span data-testid='footer-content'>Footer text</span>}
            >
                <p>Content</p>
            </AuthCard>,
        );

        expect(screen.getByTestId('footer-content')).toBeInTheDocument();
        expect(screen.getByText('Footer text')).toBeInTheDocument();
    });

    it('does not render footer when not provided', () => {
        render(
            <AuthCard title='Title'>
                <p>Content</p>
            </AuthCard>,
        );

        const footers = document.querySelectorAll('[data-slot="card-footer"]');
        expect(footers).toHaveLength(0);
    });

    it('has correct responsive layout classes', () => {
        const { container } = render(
            <AuthCard title='Title'>
                <p>Content</p>
            </AuthCard>,
        );

        // Outer wrapper should center content
        const wrapper = container.firstChild as HTMLElement;
        expect(wrapper).toHaveClass(
            'flex',
            'flex-1',
            'min-h-screen',
            'items-center',
            'justify-center',
        );

        // Card should be max-w-md
        const card = wrapper.querySelector('[data-slot="card"]');
        expect(card).toHaveClass('w-full', 'max-w-md');
    });
});
