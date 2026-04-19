import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Stepper, StepperDesktop, StepperMobile } from './stepper.tsx';

describe('Stepper', () => {
    const steps = [
        { id: 1, label: 'Step 1' },
        { id: 2, label: 'Step 2' },
        { id: 3, label: 'Step 3' },
        { id: 4, label: 'Step 4' },
    ];

    describe('StepperDesktop', () => {
        it('renders all step labels', () => {
            render(<StepperDesktop steps={steps} currentStep={1} />);

            expect(screen.getByText('Step 1')).toBeInTheDocument();
            expect(screen.getByText('Step 2')).toBeInTheDocument();
            expect(screen.getByText('Step 3')).toBeInTheDocument();
            expect(screen.getByText('Step 4')).toBeInTheDocument();
        });

        it('marks completed steps with check icon', () => {
            const { container } = render(
                <StepperDesktop steps={steps} currentStep={2} />,
            );

            // Step 1 (index 0) should be completed
            const stepButtons = container.querySelectorAll('button');
            // First step should have completed state
            expect(stepButtons[0]).toContainHTML('svg');
        });

        it('highlights current step', () => {
            render(<StepperDesktop steps={steps} currentStep={1} />);

            const currentStepButton = screen.getByRole('button', {
                name: /step 2/i,
            });
            expect(currentStepButton).toHaveAttribute('aria-current', 'step');
        });

        it('allows backward navigation to completed steps', async () => {
            const onStepClick = vi.fn();

            render(
                <StepperDesktop
                    steps={steps}
                    currentStep={2}
                    onStepClick={onStepClick}
                    backwardOnly
                />,
            );

            // Click on completed step 1 (find by text content)
            const step1Button = screen.getByRole('button', { name: /step 1/i });
            await userEvent.click(step1Button);

            expect(onStepClick).toHaveBeenCalledWith(0);
        });

        it('prevents forward navigation when backwardOnly is true', async () => {
            const onStepClick = vi.fn();

            render(
                <StepperDesktop
                    steps={steps}
                    currentStep={1}
                    onStepClick={onStepClick}
                    backwardOnly
                />,
            );

            // Click on upcoming step 3
            const step3Button = screen.getByRole('button', { name: /step 3/i });
            await userEvent.click(step3Button);

            expect(onStepClick).not.toHaveBeenCalled();
        });
    });

    describe('StepperMobile', () => {
        it('renders compact step format', () => {
            render(
                <StepperMobile
                    steps={steps}
                    currentStep={1}
                    formatLabel={(current, total) =>
                        `Step ${current} of ${total}`
                    }
                />,
            );

            expect(screen.getByText('Step 2 of 4')).toBeInTheDocument();
        });

        it('renders progress dots', () => {
            const { container } = render(
                <StepperMobile steps={steps} currentStep={1} />,
            );

            const dots = container.querySelectorAll('button.rounded-full');
            expect(dots.length).toBe(4);
        });

        it('allows backward navigation on dots', async () => {
            const onStepClick = vi.fn();

            render(
                <StepperMobile
                    steps={steps}
                    currentStep={2}
                    onStepClick={onStepClick}
                    backwardOnly
                />,
            );

            // Find and click the first dot (completed step)
            const dots = screen.getAllByRole('button');
            await userEvent.click(dots[0]);

            expect(onStepClick).toHaveBeenCalledWith(0);
        });
    });

    describe('Stepper (combined)', () => {
        it('renders both desktop and mobile variants', () => {
            const { container } = render(
                <Stepper steps={steps} currentStep={1} />,
            );

            // Both should be present (visibility controlled by CSS)
            expect(container.querySelector('.md\\:block')).toBeInTheDocument();
            expect(container.querySelector('.md\\:hidden')).toBeInTheDocument();
        });
    });
});
