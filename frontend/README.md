# Frontend

Responsive React + TypeScript UI for the AI Companion backend.

## Requirements

- Node.js 20.19+ or 22.12+
- npm
- The backend running at `http://localhost:8080` for live API use

## Development

```sh
npm install
npm run dev
```

Vite proxies `/api` to `http://localhost:8080`. Set `VITE_API_BASE_URL` in
`.env.local` to use another API base URL. The API must permit the frontend origin
when it is not served through the Vite proxy or a same-origin reverse proxy.

## Checks and build

```sh
npm run typecheck
npm run build
npm run preview
```

The backend does not expose conversation history, mood, profile/facts, or
proactive-message endpoints. Chat messages remain in page memory and are not
restored after a reload; the UI does not simulate these missing capabilities.
