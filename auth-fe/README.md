# auth-fe — React Admin Frontend

React 19 + TypeScript + Vite + Tailwind CSS admin frontend for the auth platform.

---

## Prerequisites

- Node.js 20+
- Backend running at `http://localhost:8080` (see `auth-be/README.md`)

---

## Setup

```bash
cd auth-fe
npm install
npm run dev
```

App: http://localhost:5173

---

## Environment

Create `auth-fe/.env` (copy from `.env.example` if present, or set manually):

```properties
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=corporate
VITE_KEYCLOAK_CLIENT_ID=fp-auth-client
VITE_AUDIT_PAGE_SIZE=10
```

---

## Scripts

| Command | Description |
|---|---|
| `npm run dev` | Start dev server (hot reload) |
| `npm run build` | Production build (`dist/`) |
| `npm run preview` | Preview production build locally |
| `npm run lint` | ESLint check |

---

## Stack

| | |
|---|---|
| Framework | React 19 + TypeScript |
| Build | Vite 6 |
| Styling | Tailwind CSS v4 |
| Routing | React Router v7 |
| HTTP | Axios |
| Forms | react-select, country-state-city (sign-up form) |
