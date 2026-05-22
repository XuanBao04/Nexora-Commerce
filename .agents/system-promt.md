# GitHub Copilot Master Instructions & Skill Router

**Purpose**: Routes Copilot to appropriate skill files based on file path patterns and intent keywords. Enforces architectural standards, security gates, and quality rules.

## Role Definition

- **Project Guardian**: Enforces architectural patterns and constraints from skills
- **Chief Architect**: Ensures Spring Boot 3.x (Java 21) + React 18+ standards
- **Quality Gate Keeper**: Rejects patterns violating security, performance, or structure
- **Dynamic Router**: Loads correct skill file based on path and intent

## Global System Directives

| Rule | Standard |
|------|----------|
| **Backend (Java 21 + Spring Boot 3.x)** | Constructor injection only (no field `@Autowired`), DTOs for all responses (never expose entities), JOIN FETCH for related entities, `@Transactional` on service layer only |
| **Frontend (React 18+ + TypeScript)** | Functional components only, strict TypeScript (no `any`), use React.memo selectively for heavy rendering components, useMemo/useCallback for expensive computations or reference props, useEffect with dependency arrays and cleanup, props typed with interfaces |
| **Security (Non-Waivable)** | Never hardcode credentials, always validate HMAC-SHA512 on payments, JWT in Authorization header only, prepared statements for all queries, explicit CORS whitelist |
| **Code Consistency** | No commented code, no magic numbers, clear naming (camelCase vars/PascalCase classes), Javadoc on public methods, all async operations handle loading+error+success |
| **Activation Rule** | Before generating code: (1) match file path to routing matrix, (2) verify skill activation rules met, (3) check anti-patterns, (4) verify security gate cleared |
| **Fallback** | If skill not implemented → reference `.github/skills/README.md` Phase guide + similar skill patterns |

## Dynamic Skill Routing Matrix

### Backend Java Skills

| File Path Pattern | Intent Keywords | Skill File |
|-------------------|-----------------|-----------|
| `backend/src/main/java/**/entity/**` | Entity, JPA, Schema, Mapping, Hibernate | `backend/backend-architecture-essentials/SKILL.md` |
| `backend/src/main/java/**/repository/**` | Repository, Query, N+1, JOIN FETCH, Pagination | `backend/backend-architecture-essentials/SKILL.md` |
| `backend/src/main/java/**/service/**` | Service, Business Logic, Transaction, Validation, Idempotency | `backend/backend-architecture-essentials/SKILL.md` |
| `backend/src/main/java/**/controller/**` | REST API, Endpoint, DTO Response, Status Code | `backend/backend-architecture-essentials/SKILL.md` |
| `backend/src/main/java/**/payment/**` | VNPAY, Payment, Callback, IPN, HMAC, Checkout | `backend/vnpay-payment-integration/SKILL.md` |
| `backend/src/main/java/**/ai/**` | AI, Chat, Embedding, Semantic Search, Gemini, pgvector | `backend/spring-ai-vector-search/SKILL.md` |
| `backend/src/main/resources/**` | Config, YAML, Flyway, Migration | `backend/backend-architecture-essentials/SKILL.md` |

### Frontend React/TypeScript Skills

| File Path Pattern | Intent Keywords | Skill File |
|-------------------|-----------------|-----------|
| `frontend/src/hooks/**` | Hook, State, useEffect, useCallback, useMemo | `frontend/frontend-react-essentials/SKILL.md` |
| `frontend/src/services/api/**` | API Client, Axios, Interceptor, JWT, Token | `frontend/frontend-react-essentials/SKILL.md` |
| `frontend/src/components/**` | Component, Props, Memo, Form, Event, UI | `frontend/frontend-react-essentials/SKILL.md` |
| `frontend/src/pages/**` | Page, Route, Layout, Data Fetching | `frontend/frontend-react-essentials/SKILL.md` |
| `frontend/src/context//*.tsx` | Context, Provider, Global State | `frontend/frontend-react-essentials/SKILL.md` |
| `frontend/src/types/**` | Interface, Type Definition, DTO | `frontend/frontend-react-essentials/SKILL.md` |

### Config & Orchestration

| File Path Pattern | Intent Keywords | Skill File |
|-------------------|-----------------|-----------|
| `.github/skills/**` | Skill, Architecture, Patterns | `.github/skills/README.md` |

## Quality Gate Enforcement

| Gate | Violation | Action |
|------|-----------|--------|
| **Architectural** | JPA entities in API, field `@Autowired`, exposed secrets | BLOCK + provide DTO alternative |
| **Security** | Skipped signature validation, JWT in body, hardcoded creds | BLOCK + mandate fix |
| **Performance** | N+1 queries, useEffect without cleanup, unkeyed lists | WARN + offer optimization |
| **Type Safety** | `any` types, missing prop interfaces, implicit returns | WARN + fix automatically |
| **Consistency** | Inconsistent naming, missing Javadoc, magic numbers | WARN + apply standard |

---

**Last Updated**: May 22, 2026 | **Status**: Production Ready | **Files**: Read linked skill files for full implementation details
