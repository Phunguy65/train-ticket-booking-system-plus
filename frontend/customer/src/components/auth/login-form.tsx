'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { AlertCircleIcon, CheckCircle2Icon, Loader2Icon } from 'lucide-react';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { PasswordInput } from '@/components/auth/password-input.tsx';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
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
import { useRouter } from '@/i18n/routing.ts';
import { loginMutation } from '@/lib/api/index.ts';
import { resolveLoginError } from '@/lib/auth-errors.ts';
import { showNetworkErrorToast } from '@/lib/toast.ts';
import { type LoginFormValues, loginSchema } from '@/lib/validations/auth.ts';

export function LoginForm() {
    const t = useTranslations('Auth.login');
    const tValidation = useTranslations('Validation');
    const tErrors = useTranslations('Errors');
    const router = useRouter();
    const searchParams = useSearchParams();
    const registered = searchParams.get('registered') === 'true';

    const form = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: { email: '', password: '' },
    });

    const mutation = useMutation({
        ...loginMutation(),
        onSuccess: () => {
            router.push('/');
        },
        onError: (error) => {
            form.setValue('password', '');
            showNetworkErrorToast(error, {
                network: tErrors('networkError'),
                unknown: tErrors('unknownError'),
            });
        },
    });

    const onSubmit = (values: LoginFormValues) => {
        mutation.mutate({
            body: {
                email: values.email,
                password: values.password,
            },
        });
    };

    const formError = mutation.isError
        ? resolveLoginError(mutation.error, {
              invalidCredentials: tErrors('invalidCredentials'),
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
                {registered ? (
                    <Alert variant='success' role='status'>
                        <CheckCircle2Icon className='h-4 w-4' />
                        <AlertDescription>
                            {t('registerSuccess')}
                        </AlertDescription>
                    </Alert>
                ) : null}

                {formError ? (
                    <Alert variant='destructive'>
                        <AlertCircleIcon className='h-4 w-4' />
                        <AlertTitle>{formError.message}</AlertTitle>
                    </Alert>
                ) : null}

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
                                    autoComplete='current-password'
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
