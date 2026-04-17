'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { AlertCircleIcon, Loader2Icon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { PasswordInput } from '@/components/auth/password-input.tsx';
import { Alert, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form.tsx';
import { Input } from '@/components/ui/input.tsx';
import { Link, useRouter } from '@/i18n/routing.ts';
import { registerMutation } from '@/lib/api/index.ts';
import { resolveRegisterError } from '@/lib/auth-errors.ts';
import { showNetworkErrorToast } from '@/lib/toast.ts';
import {
    type RegisterFormValues,
    registerSchema,
} from '@/lib/validations/auth.ts';

export function RegisterForm() {
    const t = useTranslations('Auth.register');
    const tLogin = useTranslations('Auth.login');
    const tValidation = useTranslations('Validation');
    const tErrors = useTranslations('Errors');
    const router = useRouter();

    const form = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            fullName: '',
            email: '',
            password: '',
            confirmPassword: '',
        },
    });

    const mutation = useMutation({
        ...registerMutation(),
        onSuccess: () => {
            // Redirect to login with ?registered=true query param so the
            // login page can show a success banner.
            router.push('/login?registered=true');
        },
        onError: (error) => {
            // Toast network/technical errors; inline Alert handles API fails
            showNetworkErrorToast(error, {
                network: tErrors('networkError'),
                unknown: tErrors('unknownError'),
            });
        },
    });

    const onSubmit = (values: RegisterFormValues) => {
        mutation.mutate({
            body: {
                email: values.email,
                password: values.password,
                fullName: values.fullName,
            },
        });
    };

    const formError = mutation.isError
        ? resolveRegisterError(mutation.error, {
              emailExists: tErrors('emailExists'),
              unknownError: tErrors('unknownError'),
          })
        : null;

    const translateFieldError = (message: string | undefined) => {
        if (!message) return null;
        return tValidation(message as Parameters<typeof tValidation>[0]);
    };

    return (
        <Form {...form}>
            <form
                onSubmit={form.handleSubmit(onSubmit)}
                className='flex flex-col gap-4'
                noValidate
            >
                {formError ? (
                    <Alert variant='destructive'>
                        <AlertCircleIcon className='h-4 w-4' />
                        <AlertTitle>
                            {formError.message}
                            {formError.showLoginLink ? (
                                <>
                                    {' '}
                                    <Link
                                        href='/login'
                                        className='font-medium underline underline-offset-4'
                                    >
                                        {tLogin('submit')}
                                    </Link>
                                </>
                            ) : null}
                        </AlertTitle>
                    </Alert>
                ) : null}

                <FormField
                    control={form.control}
                    name='fullName'
                    render={({ field, fieldState }) => (
                        <FormItem>
                            <FormLabel>{t('fullName')}</FormLabel>
                            <FormControl>
                                <Input
                                    type='text'
                                    autoComplete='name'
                                    placeholder={t('fullNamePlaceholder')}
                                    disabled={mutation.isPending}
                                    {...field}
                                />
                            </FormControl>
                            <FormMessage>
                                {translateFieldError(fieldState.error?.message)}
                            </FormMessage>
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='email'
                    render={({ field, fieldState }) => (
                        <FormItem>
                            <FormLabel>{t('email')}</FormLabel>
                            <FormControl>
                                <Input
                                    type='email'
                                    autoComplete='email'
                                    placeholder={t('emailPlaceholder')}
                                    disabled={mutation.isPending}
                                    {...field}
                                />
                            </FormControl>
                            <FormMessage>
                                {translateFieldError(fieldState.error?.message)}
                            </FormMessage>
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='password'
                    render={({ field, fieldState }) => (
                        <FormItem>
                            <FormLabel>{t('password')}</FormLabel>
                            <FormControl>
                                <PasswordInput
                                    autoComplete='new-password'
                                    placeholder={t('passwordPlaceholder')}
                                    disabled={mutation.isPending}
                                    {...field}
                                />
                            </FormControl>
                            <FormMessage>
                                {translateFieldError(fieldState.error?.message)}
                            </FormMessage>
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='confirmPassword'
                    render={({ field, fieldState }) => (
                        <FormItem>
                            <FormLabel>{t('confirmPassword')}</FormLabel>
                            <FormControl>
                                <PasswordInput
                                    autoComplete='new-password'
                                    placeholder={t(
                                        'confirmPasswordPlaceholder',
                                    )}
                                    disabled={mutation.isPending}
                                    {...field}
                                />
                            </FormControl>
                            <FormMessage>
                                {translateFieldError(fieldState.error?.message)}
                            </FormMessage>
                        </FormItem>
                    )}
                />

                <Button
                    type='submit'
                    className='mt-2'
                    disabled={mutation.isPending}
                >
                    {mutation.isPending ? (
                        <>
                            <Loader2Icon
                                className='h-4 w-4 motion-safe:animate-spin'
                                aria-hidden='true'
                            />
                            {t('submitting')}
                        </>
                    ) : (
                        t('submit')
                    )}
                </Button>
            </form>
        </Form>
    );
}
