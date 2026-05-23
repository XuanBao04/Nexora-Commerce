---
name: react-form-validation-skill
description: Rules and directives for high-performance React Hook Form + Zod validation, asymmetric backend validation mapping, and error state resets.
---

## Scope & Activation Rules
Activate this skill when:
- Designing, creating, or editing forms in the React frontend client (`.tsx`, `.jsx`).
- Configuring validation schemas using `zod` for frontend client verification.
- Integrating React Hook Form (`react-hook-form` and `@hookform/resolvers/zod`).
- Handling backend API submission errors, specifically validation errors (`422 Unprocessable Entity` or `400 Bad Request`) and structural conflicts (`409 Conflict`), mapping them back to dynamic visual fields.
- Building interactive form states including submission status, disabled states, and clean form reset flows.

---

## System Directives

### DO
1. **Centralize Form State with React Hook Form + Zod:** Always build forms utilizing unified schema-driven declarations. Use Zod to declare complete schemas (with strong type-safety, refined regex checks, and localized error messages) and feed them directly into React Hook Form's `useForm` via the Zod resolver.
2. **Implement Asymmetric Backend Error Mapping:** Implement a centralized utility function (`mapBackendErrors`) that accepts backend validation exceptions (e.g., lists of field validation errors returned from Spring Boot inside an `ApiResponse` or standard JSON bodies, including standard `409 Conflict` fields) and programmatically propagates them onto specific Hook Form input fields using `setError('fieldName', { message: '...' })`.
3. **Render Instant Real-Time Visual Feedback:** Render visual error state styles directly adjacent to form inputs. Inputs in error states must show highly visible red focus rings, helper warning text, and appropriate aria attributes (`aria-invalid="true"`, `aria-describedby="..."`) for accessibility.
4. **Ensure Clean Resets on Unmount or Success:** Always execute robust form resets. Ensure all hook values, dirty indicators, and error boundaries are completely cleared by leveraging the `reset()` API on successful transaction or component unmount.

### DO NOT
1. **Never Fragment Form Fields into Isolated State Hook Variables:** Avoid using separate `useState` calls for individual input fields in multi-input forms. This creates fragmented states, excessive re-renders, and complicates synchronization with validation rules.
2. **Never Skip Clearing Errors Prior to Submission:** Do not trigger an API request without first clearing existing field-level or root-level submission errors. Always let the hook structure reset/clear external errors to prevent stale visual errors from confusing the user while a new request is in progress.
3. **Never Hardcode Validation Logic in JSX:** Do not inline validation rules or write checks (e.g., custom email/phone regex checking) inside JSX render code. Keep all validation assertions colocated inside clean, declarative Zod schemas.

---

## Production Reference Implementation

### 1. Centralized Asymmetric Backend Error Mapper (`utils/formErrorMapper.ts`)
```typescript
import { FieldValues, UseFormSetError, Path } from 'react-hook-form';

export interface BackendFieldError {
  field: string;
  message: string;
}

export interface BackendErrorResponse {
  success: boolean;
  message: string;
  code?: string;
  details?: BackendFieldError[] | Record<string, string>;
}

/**
 * Maps standard backend validation and conflict errors directly to React Hook Form fields.
 * Handles both arrays of FieldErrors and key-value error dictionaries.
 */
export function mapBackendErrors<TFieldValues extends FieldValues>(
  errorResponse: unknown,
  setError: UseFormSetError<TFieldValues>
): boolean {
  if (!errorResponse || typeof errorResponse !== 'object') {
    return false;
  }

  const err = errorResponse as BackendErrorResponse;
  let hasMapped = false;

  // Scenario 1: Array of field-level errors (e.g., Spring Boot MethodArgumentNotValidException details)
  if (Array.isArray(err.details)) {
    err.details.forEach((errDetail) => {
      const fieldName = errDetail.field as Path<TFieldValues>;
      setError(fieldName, {
        type: 'server',
        message: errDetail.message,
      });
      hasMapped = true;
    });
  } 
  // Scenario 2: Object key-value pairs (e.g., Record<string, string>)
  else if (err.details && typeof err.details === 'object') {
    Object.entries(err.details).forEach(([key, val]) => {
      if (typeof val === 'string') {
        const fieldName = key as Path<TFieldValues>;
        setError(fieldName, {
          type: 'server',
          message: val,
        });
        hasMapped = true;
      }
    });
  }

  // Fallback: If no explicit field mappings were extracted but a global message exists,
  // we can map it to a generic 'root.serverError' field if supported by Hook Form.
  if (!hasMapped && err.message) {
    setError('root.serverError' as Path<TFieldValues>, {
      type: 'server',
      message: err.message,
    });
    hasMapped = true;
  }

  return hasMapped;
}
```

### 2. Zod Validation Schema Definition (`schemas/registerSchema.ts`)
```typescript
import { z } from 'zod';

export const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(2, 'Full name must contain at least 2 characters')
      .max(100, 'Full name must not exceed 100 characters')
      .regex(/^[a-zA-Z\s]*$/, 'Full name can only contain letters and spaces'),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters long')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number')
      .regex(/[^A-Za-z0-9]/, 'Password must contain at least one special character'),
    confirmPassword: z.string().min(1, 'Confirm password is required'),
    agreeToTerms: z.literal(true, {
      errorMap: () => ({ message: 'You must agree to the Terms & Conditions' }),
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });

export type RegisterFormData = z.infer<typeof registerSchema>;
```

### 3. Premium Register Form Component (`components/auth/RegisterForm.tsx`)
```typescript
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerSchema, RegisterFormData } from '../../schemas/registerSchema';
import { mapBackendErrors } from '../../utils/formErrorMapper';
import { cn } from '../../utils/cn'; // Reuses style tailwind skill utility

export const RegisterForm: React.FC = React.memo(() => {
  const [isSubmittingApi, setIsSubmittingApi] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    reset,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched', // Validates in real time as the user leaves each input field
  });

  const onSubmit = async (data: RegisterFormData) => {
    // 1. Setup Submission State and Clear Stale Errors
    setIsSubmittingApi(true);
    setSuccessMessage(null);
    clearErrors(); // Clears all visual states prior to new request

    try {
      // Simulate API registration request
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      const responseBody = await response.json();

      if (!response.ok) {
        // 2. Perform Asymmetric Backend Error Mapping
        const isMapped = mapBackendErrors<RegisterFormData>(responseBody, setError);
        if (!isMapped) {
          setError('root.serverError', {
            type: 'server',
            message: responseBody.message || 'An unexpected server error occurred.',
          });
        }
        return;
      }

      // 3. Handle Success Flow
      setSuccessMessage('Account created successfully! Check your email to verify.');
      reset(); // Clears all form fields, errors, and state boundaries
    } catch (err) {
      console.error('Failed to submit form:', err);
      setError('root.serverError', {
        type: 'server',
        message: 'Network connection failed. Please try again later.',
      });
    } finally {
      setIsSubmittingApi(false);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto bg-white border border-gray-200 shadow-xl rounded-2xl p-6 sm:p-8">
      <div className="mb-6 text-center">
        <h2 className="text-2xl sm:text-3xl font-extrabold text-gray-900">Create your Account</h2>
        <p className="mt-2 text-sm text-gray-500">Sign up in minutes to access exclusive features</p>
      </div>

      {successMessage && (
        <div className="mb-6 p-4 rounded-xl bg-green-50 border border-green-200 text-green-800 text-sm font-medium animate-fadeIn">
          {successMessage}
        </div>
      )}

      {errors.root?.serverError && (
        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm font-medium animate-fadeIn">
          {errors.root.serverError.message}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
        {/* Full Name Input Field */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="fullName" className="text-sm font-semibold text-gray-700">
            Full Name
          </label>
          <input
            id="fullName"
            type="text"
            placeholder="John Doe"
            disabled={isSubmittingApi}
            aria-invalid={errors.fullName ? 'true' : 'false'}
            {...register('fullName')}
            className={cn(
              "w-full px-4 py-2.5 border rounded-xl text-gray-900 transition-all duration-200 focus:outline-none focus:ring-2",
              errors.fullName
                ? "border-red-500 focus:ring-red-200 focus:border-red-500"
                : "border-gray-300 focus:ring-blue-200 focus:border-blue-500",
              isSubmittingApi && "bg-gray-50 text-gray-400 cursor-not-allowed"
            )}
          />
          {errors.fullName && (
            <span role="alert" className="text-xs font-semibold text-red-600 animate-slideDown">
              {errors.fullName.message}
            </span>
          )}
        </div>

        {/* Email Input Field */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" className="text-sm font-semibold text-gray-700">
            Email Address
          </label>
          <input
            id="email"
            type="email"
            placeholder="you@example.com"
            disabled={isSubmittingApi}
            aria-invalid={errors.email ? 'true' : 'false'}
            {...register('email')}
            className={cn(
              "w-full px-4 py-2.5 border rounded-xl text-gray-900 transition-all duration-200 focus:outline-none focus:ring-2",
              errors.email
                ? "border-red-500 focus:ring-red-200 focus:border-red-500"
                : "border-gray-300 focus:ring-blue-200 focus:border-blue-500",
              isSubmittingApi && "bg-gray-50 text-gray-400 cursor-not-allowed"
            )}
          />
          {errors.email && (
            <span role="alert" className="text-xs font-semibold text-red-600 animate-slideDown">
              {errors.email.message}
            </span>
          )}
        </div>

        {/* Password Input Field */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-semibold text-gray-700">
            Password
          </label>
          <input
            id="password"
            type="password"
            placeholder="••••••••"
            disabled={isSubmittingApi}
            aria-invalid={errors.password ? 'true' : 'false'}
            {...register('password')}
            className={cn(
              "w-full px-4 py-2.5 border rounded-xl text-gray-900 transition-all duration-200 focus:outline-none focus:ring-2",
              errors.password
                ? "border-red-500 focus:ring-red-200 focus:border-red-500"
                : "border-gray-300 focus:ring-blue-200 focus:border-blue-500",
              isSubmittingApi && "bg-gray-50 text-gray-400 cursor-not-allowed"
            )}
          />
          {errors.password && (
            <span role="alert" className="text-xs font-semibold text-red-600 animate-slideDown">
              {errors.password.message}
            </span>
          )}
        </div>

        {/* Confirm Password Input Field */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor="confirmPassword" className="text-sm font-semibold text-gray-700">
            Confirm Password
          </label>
          <input
            id="confirmPassword"
            type="password"
            placeholder="••••••••"
            disabled={isSubmittingApi}
            aria-invalid={errors.confirmPassword ? 'true' : 'false'}
            {...register('confirmPassword')}
            className={cn(
              "w-full px-4 py-2.5 border rounded-xl text-gray-900 transition-all duration-200 focus:outline-none focus:ring-2",
              errors.confirmPassword
                ? "border-red-500 focus:ring-red-200 focus:border-red-500"
                : "border-gray-300 focus:ring-blue-200 focus:border-blue-500",
              isSubmittingApi && "bg-gray-50 text-gray-400 cursor-not-allowed"
            )}
          />
          {errors.confirmPassword && (
            <span role="alert" className="text-xs font-semibold text-red-600 animate-slideDown">
              {errors.confirmPassword.message}
            </span>
          )}
        </div>

        {/* Terms & Conditions Checkbox */}
        <div className="flex flex-col gap-1.5">
          <div className="flex items-start gap-3">
            <input
              id="agreeToTerms"
              type="checkbox"
              disabled={isSubmittingApi}
              aria-invalid={errors.agreeToTerms ? 'true' : 'false'}
              {...register('agreeToTerms')}
              className={cn(
                "h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 transition-colors mt-1",
                errors.agreeToTerms && "border-red-500 focus:ring-red-500"
              )}
            />
            <label htmlFor="agreeToTerms" className="text-sm text-gray-600 select-none">
              I agree to the{' '}
              <a href="/terms" className="text-blue-600 hover:underline">
                Terms of Service
              </a>{' '}
              and{' '}
              <a href="/privacy" className="text-blue-600 hover:underline">
                Privacy Policy
              </a>
              .
            </label>
          </div>
          {errors.agreeToTerms && (
            <span role="alert" className="text-xs font-semibold text-red-600 animate-slideDown">
              {errors.agreeToTerms.message}
            </span>
          )}
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isSubmittingApi}
          className="w-full mt-2 inline-flex items-center justify-center font-bold rounded-xl px-4 py-3 text-white bg-blue-600 hover:bg-blue-700 active:scale-[0.98] transition-all focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmittingApi ? (
            <span className="inline-flex items-center gap-2">
              <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              Registering...
            </span>
          ) : (
            'Register'
          )}
        </button>
      </form>
    </div>
  );
});

RegisterForm.displayName = 'RegisterForm';
```

---

## Anti-Patterns & Automated Fixes

### Anti-Pattern: Fragmented Isolated State Hook Variables
```typescript
// ❌ INCORRECT: Scattered state requires custom validation handlers
const RegisterForm = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  
  const validate = () => {
    if (!email.includes('@')) setEmailError('Invalid email');
  };
};
```
#### Fix: Centralize Form Validation Schema with Zod
```typescript
// ✅ CORRECT: Handled implicitly and unified in form hook
const { register, formState: { errors } } = useForm({
  resolver: zodResolver(registerSchema)
});
```

### Anti-Pattern: Manual Error Checking on Response Callback
```typescript
// ❌ INCORRECT: Inefficient field mapping code duplicated across forms
if (response.status === 422) {
  if (data.field === 'email') setError('email', { message: data.message });
  if (data.field === 'password') setError('password', { message: data.message });
}
```
#### Fix: Leverage Automated Unified Error Mapping Utility
```typescript
// ✅ CORRECT: Asymmetric mapper processes field list seamlessly
if (!response.ok) {
  mapBackendErrors(responseBody, setError);
}
```

### Anti-Pattern: Stale Visual Errors During Re-Submission
```typescript
// ❌ INCORRECT: Triggers API submit while retaining old error styling
const onSubmit = async (data) => {
  setIsLoading(true);
  await apiCall(data); // Stale visual errors remain visible if transaction delays
};
```
#### Fix: Clear Form Error Boundaries Prior to Execution
```typescript
// ✅ CORRECT: Explicitly clear validation states on initiation
const onSubmit = async (data) => {
  setIsLoading(true);
  clearErrors();
  await apiCall(data);
};
```

---

## Verification Commands
Verify type-safety and syntax validity of forms and schema definitions:
```bash
# Type check the frontend code
cd frontend && npx tsc --noEmit

# Run unit tests targeting error mappers or form utilities
npm run test
```
