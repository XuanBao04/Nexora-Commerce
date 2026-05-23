---
name: react-style-tailwind-skill
description: Rules and guidelines for creating responsive, interactive, and consistent UI/UX components using React 18+ and Tailwind CSS.
---

## Scope & Activation Rules
Activate this skill when:
- Creating or editing React components (`.tsx`, `.jsx`) in the frontend client.
- Defining UI styling, animations, or layouts using Tailwind utility classes.
- Managing interactive component states (hover, focus, disabled, active).
- Building responsive page sections or custom layout grids.

---

## System Directives

### DO
1. **Design Mobile-First Responsive Layouts:** Always define base styling for mobile screens first, then build up progressively to larger breakpoints using prefixes (`md:`, `lg:`, `xl:`).
2. **Safely Merge Dynamic/Variant Classes:** Always use a custom class merging utility `cn(...)` built on `clsx` and `tailwind-merge` to resolve style conflicts on custom variants (such as buttons, badges, inputs).
3. **Explicitly Set All Interactive States:** Ensure every focusable or clickable element includes clear focus visual indicators (`focus-visible:ring-2 focus-visible:ring-offset-2`), hover transitions, and disabled states (`disabled:opacity-50 disabled:cursor-not-allowed`).
4. **Optimize Images Against Layout Shifts:** Image elements must always be bounded by height-restricting container frames combined with `object-cover` and explicit width/height dimensions to prevent layout shifts during image lazy loading.

### DO NOT
1. **Never Hardcode Arbitrary Colors:** Do not use arbitrary tailwind colors (e.g., `text-[#ff4500]`, `bg-[#12a4b8]`). Rely strictly on the configured Tailwind system design tokens (`primary`, `secondary`, `neutral`, `error`, `success`).
2. **Never Write Block-Long Class Strings:** Do not write long class strings exceeding 15 utilities on a single line. Instead, extract complex styles into presentational sub-components, helper variables, or style variant definitions.

---

## Production Reference Implementation

### 1. Unified CSS Variant Merging Utility (`utils/cn.ts`)
```typescript
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Merges class names safely, resolving Tailwind utility conflicts.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

### 2. Premium Button Component (`components/ui/Button.tsx`)
```typescript
import React from 'react';
import { cn } from '../../utils/cn';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = React.memo(({
  className,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  children,
  ...props
}) => {
  // Enforce structured variant options
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-lg transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-600 disabled:opacity-50 disabled:cursor-not-allowed select-none active:scale-[0.98]';

  const variants = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800 focus-visible:ring-blue-600',
    secondary: 'bg-gray-100 text-gray-800 hover:bg-gray-200 active:bg-gray-300 focus-visible:ring-gray-400',
    outline: 'border border-gray-300 bg-transparent text-gray-700 hover:bg-gray-50 active:bg-gray-100 focus-visible:ring-gray-400',
    danger: 'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 focus-visible:ring-red-600',
  };

  const sizes = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-6 py-3 text-lg',
  };

  return (
    <button
      className={cn(baseStyles, variants[variant], sizes[size], className)}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <span className="inline-flex items-center gap-2">
          <svg className="animate-spin h-5 w-5 text-current" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          Loading...
        </span>
      ) : (
        children
      )}
    </button>
  );
});

Button.displayName = 'Button';
```

### 3. Premium Responsive Card Component (`components/ui/ProductCard.tsx`)
```typescript
import React from 'react';
import { cn } from '../../utils/cn';

interface ProductCardProps {
  name: string;
  price: number;
  imageUrl: string;
  isAvailable?: boolean;
}

export const ProductCard: React.FC<ProductCardProps> = React.memo(({
  name,
  price,
  imageUrl,
  isAvailable = true,
}) => {
  return (
    <div className="group flex flex-col border border-gray-200 rounded-2xl bg-white p-4 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
      {/* Frame limiting dimensions to prevent layout shifts */}
      <div className="relative w-full h-40 sm:h-48 md:h-52 overflow-hidden rounded-xl bg-gray-50">
        <img
          src={imageUrl}
          alt={name}
          className={cn(
            "w-full h-full object-cover transition-transform duration-300 group-hover:scale-105",
            !isAvailable && "grayscale"
          )}
          loading="lazy"
        />
        {!isAvailable && (
          <span className="absolute top-2 right-2 bg-red-100 text-red-800 text-xs font-semibold px-2.5 py-0.5 rounded-full">
            Out of Stock
          </span>
        )}
      </div>

      <div className="mt-4 flex flex-col flex-1 justify-between">
        <div>
          <h3 className="text-base font-semibold text-gray-900 group-hover:text-blue-600 line-clamp-2 transition-colors duration-200">
            {name}
          </h3>
        </div>
        <div className="mt-3 flex items-center justify-between">
          <span className="text-lg font-bold text-gray-900">${price.toFixed(2)}</span>
          <button
            disabled={!isAvailable}
            className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed text-white text-sm font-semibold rounded-lg transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600"
          >
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
});

ProductCard.displayName = 'ProductCard';
```

---

## Anti-Patterns & Automated Fixes

### Anti-Pattern: Hardcoded Color Utilities
```typescript
// ❌ INCORRECT: Hardcodes arbitrary color strings
<div className="bg-[#12a4b8] border border-[#ff4500]">Oops</div>
```
#### Fix: Use Standard Tailwinds Palette Tokens
```typescript
//  CORRECT: Resolves using theme design tokens
<div className="bg-sky-600 border border-red-500">Perfect</div>
```

### Anti-Pattern: Direct String Concatenation for Dynamic Variants
```typescript
// ❌ INCORRECT: Collides tailwind bg classes together
const Button = ({ active }) => (
  <button className={`bg-blue-600 ${active ? 'bg-green-600' : ''}`}>Click</button>
);
```
#### Fix: Wrap with custom merging utility
```typescript
//  CORRECT: Merges and strips conflicting utilities safely
const Button = ({ active }) => (
  <button className={cn('bg-blue-600', active && 'bg-green-600')}>Click</button>
);
```

---

## Verification Commands
Verify frontend styles and CSS output:
```bash
# Type check components
cd frontend && npx tsc --noEmit

# Test build with Tailwind preprocessing
npm run build
```
