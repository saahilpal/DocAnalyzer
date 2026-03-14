# Backend Integration Specification (Frontend Reference)
Static analysis source: current `master` codebase (post-refactor), no runtime execution.

## 1) System Overview

### Architecture
The backend is a layered Express service:

1. `routes` define HTTP API paths under `/api/v1`.
2. `middleware` handles auth, validation, and rate limiting.
3. `controllers` orchestrate request flow.
4. `services` implement business workflows:
- auth/session lifecycle
- upload parsing/indexing
- RAG retrieval + generation
- async job queue
5. `database` migration/bootstrap logic for SQLite schema.
6. `parsers` normalize text from uploaded files.

### Major components
- Express API + Zod validation
- SQLite (`better-sqlite3`) persistence
- JWT auth + server-side auth session records
- Multipart uploads (`multer`) with signature/MIME/extension checks
- Background jobs (`job_queue`) for indexing/chat
- Local embeddings (`@xenova/transformers`)
- Vector similarity search in SQLite chunk store
- SSE streaming for chat responses

### RAG flow (high level)
1. User uploads document to a session.
2. File is validated and stored on disk.
3. Background indexing job parses text and chunks it.
4. Local ONNX embedding model creates vectors.
5. Vectors are stored in `chunks` table.
6. Chat request embeds query, searches chunks by cosine similarity.
7. Retrieved context is injected into prompt.
8. Gemini generates final answer (sync/async/stream).

### Session-document interaction
- Sessions are chat containers.
- Documents are attached to a session.
- Chat is blocked until at least one document is indexed and no documents are still processing/failed for that session (`PDF_NOT_READY`).

### Core data flow
`HTTP -> route -> validation/auth -> controller -> service(s) -> SQLite/filesystem -> response`

---

## 2) API Endpoints

Base URL prefix: `/api/v1`  
Response envelope:
- success: `{ "ok": true, "data": ... }`
- failure: `{ "ok": false, "error": { "code", "message", "retryable" } }`

## Endpoint Matrix

| Method | Path | Auth | Request body schema | Query params | Success response schema | Common error codes |
|---|---|---|---|---|---|---|
| GET | `/health` | No | none | none | service runtime/queue/memory/cpu object | `RATE_LIMITED` |
| GET | `/ping` | No | none | none | `{ pong: true }` | `RATE_LIMITED` |
| POST | `/auth/register` | No | `{ name:string(1..120), email:email<=320, password:string(8..128) }` | none | `{ message }` (test env may include `otp`) | `VALIDATION_ERROR`, `INVALID_AUTH_INPUT`, `RATE_LIMITED` |
| POST | `/auth/send-otp` | No | `{ email }` | none | `{ message }` (test env may include `otp`) | `VALIDATION_ERROR`, `RATE_LIMITED` |
| POST | `/auth/verify-otp` | No | `{ email, otp:string(len=6) }` | none | `{ message }` | `VALIDATION_ERROR`, `INVALID_OTP`, `RATE_LIMITED` |
| POST | `/auth/login` | No | `{ email, password }` | none | `{ accessToken, refreshToken, expiresAt, user }` | `VALIDATION_ERROR`, `INVALID_CREDENTIALS`, `INACTIVE_ACCOUNT`, `RATE_LIMITED` |
| POST | `/auth/refresh` | No | `{ refreshToken }` | none | `{ accessToken, refreshToken, expiresAt }` | `VALIDATION_ERROR`, `INVALID_TOKEN`, `UNAUTHORIZED`, `RATE_LIMITED` |
| POST | `/auth/request-reset` | No | `{ email }` | none | `{ message }` (test env may include `otp`) | `VALIDATION_ERROR`, `RATE_LIMITED` |
| POST | `/auth/reset-password` | No | `{ email, otp:string(len=6), newPassword:string(8..128) }` | none | `{ message }` | `VALIDATION_ERROR`, `INVALID_OTP`, `RATE_LIMITED` |
| GET | `/auth/me` | Yes | none | none | `{ id, name, email, created_at, is_active }` | `UNAUTHORIZED`, `RATE_LIMITED` |
| POST | `/auth/change-email` | Yes | `{ newEmail }` | none | `{ message, otp? }` (`otp` generally test-only) | `UNAUTHORIZED`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| POST | `/auth/change-email/verify` | Yes | `{ newEmail, otp }` | none | `{ message }` | `UNAUTHORIZED`, `VALIDATION_ERROR`, `INVALID_OTP`, `RATE_LIMITED` |
| GET | `/auth/sessions` | Yes | none | none | `{ sessions: [{ id, device_info, ip_address, created_at, last_used_at }] }` | `UNAUTHORIZED`, `RATE_LIMITED` |
| DELETE | `/auth/sessions/:sessionId` | Yes | none | none | `{ deleted:true }` | `UNAUTHORIZED`, `NOT_FOUND`, `RATE_LIMITED` |
| DELETE | `/auth/session` | Yes | none | none | `{ loggedOut:true }` | `UNAUTHORIZED`, `RATE_LIMITED` |
| GET | `/sessions` | Yes | none | none | `SessionListItem[]` | `UNAUTHORIZED`, `RATE_LIMITED` |
| POST | `/sessions` | Yes | `{ title?: string(1..160) }` default `NewChat` | none | `Session` | `UNAUTHORIZED`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| GET | `/sessions/search` | Yes | none | `q?: string<=160` | `SessionListItem[]` | `UNAUTHORIZED`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| PATCH | `/sessions/:sessionId` | Yes | `{ title:string(1..60) }` | none | `Session` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| GET | `/sessions/:sessionId/meta` | Yes | none | none | `{ id, title, created_at, updated_at, pdfCount, messageCount }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `INVALID_SESSION_ID`, `RATE_LIMITED` |
| GET | `/sessions/:sessionId` | Yes | none | none | `Session + { pdfs: PdfRecord[] }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| DELETE | `/sessions/:sessionId` | Yes | none | none | `{ deleted:true, id }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| POST | `/sessions/:sessionId/pdfs` | Yes | `multipart/form-data`: `file` required, `title` optional | none | `202 { pdfId, sessionId, title, status, jobId, progress, stage, queuePosition }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `MISSING_UPLOAD_FILE`, `INVALID_FILE_MIME`, `INVALID_FILE_EXTENSION`, `INVALID_FILE_SIGNATURE`, `UPLOAD_TOO_LARGE`, `SESSION_DOC_LIMIT`, `RATE_LIMITED` |
| GET | `/sessions/:sessionId/pdfs` | Yes | none | none | `PdfRecord[]` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| POST | `/sessions/:sessionId/chat` | Yes | `{ message:string(1..10000), history?:[{role:user|assistant,text}], responseStyle?:plain|structured }` | `stream=true` optional | `200 ChatResponse` OR `202 ChatJobAck` OR SSE stream | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `PDF_NOT_READY`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| GET | `/sessions/:sessionId/history` | Yes | none | `limit?:digits`, `offset?:digits` | `Message[]` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `VALIDATION_ERROR`, `RATE_LIMITED` |
| DELETE | `/sessions/:sessionId/history` | Yes | none | none | `{ cleared:true }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| GET | `/pdfs/:pdfId` | Yes | none | none | `PdfRecord` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| DELETE | `/pdfs/:pdfId` | Yes | none | `removeFile=true|false` | `{ deleted:true, id }` | `UNAUTHORIZED`, `INVALID_PATH_PARAM`, `RATE_LIMITED` |
| GET | `/jobs/:jobId` | Yes | none | none | `{ id,type,status,progress,stage,createdAt,updatedAt,queuePosition,result?,error? }` | `UNAUTHORIZED`, `NOT_FOUND`, `RATE_LIMITED` |
| POST | `/chat` | Yes | same as session chat, but `sessionId` required in body | `stream=true` optional | same as `/sessions/:sessionId/chat` | `UNAUTHORIZED`, `INVALID_SESSION_ID`, `VALIDATION_ERROR`, `RATE_LIMITED` |

### Type snapshots

`SessionListItem`:
```json
{
  "id": 123,
  "title": "My Session",
  "createdAt": "ISO",
  "updatedAt": "ISO",
  "last_message": "string",
  "last_message_at": "ISO|null",
  "lastMessagePreview": "string",
  "lastMessageAt": "ISO|null",
  "messageCount": 0,
  "pdfCount": 0
}
```

`Session`:
```json
{
  "id": 123,
  "title": "My Session",
  "createdAt": "ISO",
  "updatedAt": "ISO",
  "lastMessageAt": "ISO|null",
  "lastMessagePreview": "string"
}
```

`PdfRecord`:
```json
{
  "id": 10,
  "sessionId": 123,
  "title": "Doc title",
  "filename": "10.pdf",
  "path": "/abs/path/data/uploads/123/10.pdf",
  "type": "pdf|docx|csv|md|txt",
  "status": "processing|indexed|failed",
  "indexedChunks": 42,
  "createdAt": "ISO"
}
```

`ChatResponse` (non-stream):
```json
{
  "answer": "string",
  "formattedAnswer": "string",
  "responseSchema": {
    "format": "structured_sections",
    "sections": [
      { "title": "Answer", "content": "..." }
    ]
  },
  "responseStyle": "structured|plain",
  "sources": [
    { "pdfId": 10, "chunkId": "uuid", "score": 0.91 }
  ],
  "usedChunksCount": 5,
  "sessionTitle": "string",
  "fallback": "optional"
}
```

`ChatJobAck`:
```json
{
  "jobId": "job_...",
  "sessionId": 123,
  "status": "processing",
  "responseStyle": "structured|plain",
  "progress": 0,
  "stage": "retrieving",
  "queuePosition": 1
}
```

---

## 3) Authentication

### Registration flow
1. `POST /auth/register` with `name/email/password`.
2. Backend creates user as inactive.
3. OTP is generated and emailed.
4. Frontend calls `POST /auth/verify-otp` with `email + otp`.
5. User is activated.

### Login flow
1. `POST /auth/login` with `email/password`.
2. Returns:
- `accessToken` (JWT, HS256, currently 15m hardcoded)
- `refreshToken` (opaque random token)
- `expiresAt`
- `user` object
3. Use access token in header:
```http
Authorization: Bearer <accessToken>
```

### JWT and session validation
- Access token payload includes `userId` and `sessionId`.
- `requireAuth` verifies JWT and checks matching active record in `auth_sessions`.
- If invalid/missing: `401 UNAUTHORIZED`.

### Refresh token flow
- `POST /auth/refresh` rotates refresh token.
- Old refresh token is deleted and becomes invalid.
- New access + refresh tokens returned.

### Password reset / OTP flow
1. `POST /auth/request-reset` with email.
2. OTP stored in `password_reset_otps` and emailed.
3. `POST /auth/reset-password` with `email + otp + newPassword`.
4. On success:
- all user reset OTP rows are cleared
- password updated
- all auth sessions and refresh tokens for user are revoked

### Frontend token usage example
```js
const token = login.data.accessToken;
await fetch('/api/v1/sessions', {
  headers: { Authorization: `Bearer ${token}` }
});
```

---

## 4) Document Upload System

### Supported file types
- `pdf` (`application/pdf`)
- `docx` (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`)
- `csv` (`text/csv`)
- `md` (`text/markdown`)
- `txt` (`text/plain`)

### Upload endpoint
`POST /api/v1/sessions/:sessionId/pdfs` (multipart form-data)

Form fields:
- `file` (required)
- `title` (optional)

### Validation layers
- MIME allowlist check
- extension compatibility check
- magic-byte/signature check
- file-size check
- upload path safety check
- per-session document count check

### Limits
- Max upload size: `MAX_UPLOAD_FILE_SIZE_BYTES` (default 30MB)
- Max docs per session: `MAX_DOCS_PER_SESSION` (default 5)
- Max PDF pages: `MAX_PDF_PAGES` (default 150)
- Max extracted text length: `MAX_EXTRACTED_TEXT_LENGTH` (default 2,000,000 chars)

### Indexing workflow
- Upload creates `pdfs` record with `status=processing`.
- Async job `indexPdf` is enqueued.
- Job:
1. parses file
2. chunks text
3. embeds chunks locally
4. stores vectors in `chunks`
5. marks document `indexed` or `failed`

---

## 5) RAG Pipeline (Detailed)

1. **Document upload**
- User posts file to session endpoint.
- Backend stores to `data/uploads/<sessionId>/<pdfId>.<ext>`.

2. **Parsing**
- Parser selected by detected file type.
- PDF parser enforces page limit.
- Text normalized and trimmed.

3. **Chunk generation**
- `chunkService.chunkText` with dynamic chunk size/overlap from text size.
- Whitespace normalization applied.

4. **Embedding creation**
- Local model: `Xenova/all-MiniLM-L6-v2`.
- Batched sequential processing for memory safety.

5. **Vector storage**
- Chunks persisted in SQLite `chunks` table:
- `text`, `embedding` (JSON), `embeddingVectorLength`, `chunkKey`, `pdfId`, `sessionId`.

6. **Similarity search**
- Query is embedded locally.
- Search is paginated and bounded by `MAX_CHUNKS_PER_QUERY` (default 2000).
- Cosine similarity ranks chunks.
- Top-k is clamped (`1..5`).

7. **Context injection**
- Prompt includes recent history and top context chunks with score metadata.

8. **AI generation**
- Gemini model candidates used for final answer generation.
- Structured/plain response normalization.
- Fallback answer if no context or generation failure.

---

## 6) Streaming Chat (SSE)

### Triggering stream mode
Use either:
- query `?stream=true`, or
- `Accept: text/event-stream`

### Required headers (recommended)
```http
Authorization: Bearer <accessToken>
Accept: text/event-stream
Content-Type: application/json
```

### Stream event format
Server sends:
- `event: ready`
- `event: progress`
- `event: token`
- `event: done`
- `event: error`

Payload line format:
```text
data: {"ok":true,"data":{...}}
```

### Example stream sequence
```text
event: ready
data: {"ok":true,"data":{"sessionId":123,"status":"streaming"}}

event: progress
data: {"ok":true,"data":{"stage":"retrieving","progress":35}}

event: token
data: {"ok":true,"data":{"token":"partial text"}}

event: done
data: {"ok":true,"data":{"answer":"...","sources":[...]}}

```

### Frontend consumption note
Because endpoint is `POST` with auth header, use `fetch` + stream reader, not plain browser `EventSource` (which is GET-only and limited header control).

---

## 7) Database Structure

Primary DB: SQLite at `DB_PATH` (default `data/studyrag.sqlite`)

### `users`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | user id |
| name | TEXT NOT NULL | display name |
| email | TEXT NOT NULL UNIQUE | login email |
| password_hash | TEXT NOT NULL | bcrypt hash |
| created_at | TEXT NOT NULL | ISO timestamp |
| updated_at | TEXT NOT NULL | ISO timestamp |
| is_active | INTEGER NOT NULL DEFAULT 1 | 0/1 activation |

### `sessions`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | session id |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| title | TEXT NOT NULL | session title |
| createdAt | TEXT NOT NULL | created time |
| updatedAt | TEXT | updated time |
| last_message_at | TEXT | latest message timestamp |
| last_message_preview | TEXT | latest message preview |

### `pdfs` (documents table; supports all file types)
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | document id |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| sessionId | INTEGER NOT NULL FK sessions(id) | parent session |
| title | TEXT NOT NULL | document title |
| filename | TEXT NOT NULL | stored file name |
| path | TEXT NOT NULL | absolute file path |
| type | TEXT NOT NULL | `pdf/docx/csv/md/txt` |
| status | TEXT NOT NULL | `processing/indexed/failed` |
| indexedChunks | INTEGER NOT NULL DEFAULT 0 | indexed chunk count |
| createdAt | TEXT NOT NULL | upload time |

### `chunks`
| Column | Type | Purpose |
|---|---|---|
| id | TEXT PK | chunk id (uuid) |
| sessionId | INTEGER NOT NULL FK sessions(id) | session scope |
| pdfId | INTEGER FK pdfs(id) | source document |
| chunkKey | TEXT | idempotency key |
| text | TEXT NOT NULL | chunk content |
| embedding | TEXT NOT NULL | embedding JSON array |
| embeddingVectorLength | INTEGER NOT NULL DEFAULT 0 | embedding dimension |
| createdAt | TEXT NOT NULL | created time |

### `chat_messages`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | message id |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| sessionId | INTEGER NOT NULL FK sessions(id) | session |
| role | TEXT NOT NULL | `user/assistant/system` |
| text | TEXT NOT NULL | message text |
| createdAt | TEXT NOT NULL | timestamp |

### `auth_sessions`
| Column | Type | Purpose |
|---|---|---|
| id | TEXT PK | auth session id (uuid) |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| token_hash | TEXT NOT NULL UNIQUE | hashed opaque token (legacy/session data) |
| expires_at | TEXT NOT NULL | expiration |
| created_at | TEXT NOT NULL | created |
| device_info | TEXT | user-agent snapshot |
| ip_address | TEXT | source IP |
| last_used_at | TEXT | last usage |

### `refresh_tokens`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | row id |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| token_hash | TEXT NOT NULL UNIQUE | hashed refresh token |
| expires_at | TEXT NOT NULL | expiration |
| created_at | TEXT NOT NULL | created |

### `email_otps`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | row id |
| email | TEXT NOT NULL | target email |
| otp_hash | TEXT NOT NULL | hashed OTP |
| expires_at | TEXT NOT NULL | expiration |
| attempts | INTEGER NOT NULL DEFAULT 0 | verification attempts |
| type | TEXT NOT NULL | `register` or `change_email` |
| created_at | TEXT NOT NULL | created |

### `password_reset_otps`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | row id |
| user_id | INTEGER NOT NULL FK users(id) | owner |
| otp_hash | TEXT NOT NULL | hashed reset OTP |
| expires_at | TEXT NOT NULL | expiration |
| attempts | INTEGER NOT NULL DEFAULT 0 | attempts |
| used | INTEGER NOT NULL DEFAULT 0 | consumption flag |
| created_at | TEXT NOT NULL | created |

### `login_attempts`
| Column | Type | Purpose |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | row id |
| email | TEXT NOT NULL | login email |
| ip_address | TEXT NOT NULL | source IP |
| attempts | INTEGER NOT NULL DEFAULT 0 | failed attempts in window |
| locked_until | INTEGER NOT NULL DEFAULT 0 | epoch ms lock |
| window_start | INTEGER NOT NULL | epoch ms window start |

### `job_queue`
| Column | Type | Purpose |
|---|---|---|
| id | TEXT PK | job id |
| type | TEXT NOT NULL | `indexPdf` or `chatQuery` |
| payload | TEXT NOT NULL | JSON payload |
| status | TEXT NOT NULL | `queued/processing/completed/failed` |
| progress | INTEGER NOT NULL DEFAULT 0 | 0..100 |
| stage | TEXT | workflow stage |
| attempts | INTEGER NOT NULL DEFAULT 0 | run attempts |
| maxRetries | INTEGER NOT NULL DEFAULT 3 | retry cap |
| result | TEXT | JSON serialized result |
| error | TEXT | error message |
| createdAt | TEXT NOT NULL | created |
| updatedAt | TEXT NOT NULL | updated |

---

## 8) Environment Variables

### Core runtime
| Variable | Default | Purpose |
|---|---|---|
| NODE_ENV | `development` | environment mode |
| PORT | `4000` | server port |
| HOST | `0.0.0.0` | bind host |
| DB_PATH | `data/studyrag.sqlite` | SQLite file path |
| TRUST_PROXY | `false` | Express proxy trust |
| CORS_ALLOWED_ORIGINS | empty | explicit allowed origins list |
| MAX_REQUEST_BODY_SIZE_BYTES | `2097152` | JSON/urlencoded body limit |

### Auth/security
| Variable | Default | Purpose |
|---|---|---|
| JWT_SECRET | fallback dev secret | JWT signing key |
| AUTH_LOGIN_WINDOW_MS | `900000` | failed login window |
| AUTH_LOGIN_LOCK_MS | `900000` | lockout duration |
| AUTH_LOGIN_MAX_FAILURES | `6` | max failed attempts |

### Upload/parsing limits
| Variable | Default | Purpose |
|---|---|---|
| MAX_UPLOAD_FILE_SIZE_BYTES | `31457280` | max upload size |
| MAX_DOCS_PER_SESSION | `5` | max docs per session |
| MAX_PDF_PAGES | `150` | PDF page cap |
| MAX_CHUNKS_PER_QUERY | `2000` | retrieval scan cap |
| MAX_EXTRACTED_TEXT_LENGTH | `2000000` | parsed text cap |

### RAG/embedding tuning
| Variable | Default | Purpose |
|---|---|---|
| RAG_TOP_K | `5` | top chunks for prompt |
| RAG_CANDIDATE_PAGE_SIZE | `400` | retrieval page size |
| RAG_HISTORY_LIMIT | `12` | history depth in prompt |
| RAG_RESPONSE_STYLE | `structured` | default response style |
| RAG_TOKEN_TO_CHAR_RATIO | `4` | chunk sizing heuristic |
| RAG_CHUNK_TOKENS | `1000` | target chunk tokens |
| RAG_CHUNK_OVERLAP_TOKENS | `200` | overlap tokens |
| LOCAL_EMBEDDING_BATCH_SIZE | `24` | embedding batch size |
| LOCAL_EMBEDDING_BATCH_SIZE_MIN | `8` | min batch clamp |
| LOCAL_EMBEDDING_BATCH_SIZE_MAX | `64` | max batch clamp |

### Gemini generation
| Variable | Default | Purpose |
|---|---|---|
| GEMINI_API_KEY | empty | required for generation |
| GEMINI_MODEL | `gemini-2.5-flash` | model candidate list |

### Cleanup worker
| Variable | Default | Purpose |
|---|---|---|
| CLEANUP_INTERVAL_MS | `900000` | cleanup cycle interval |
| CLEANUP_COMPLETED_JOB_TTL_HOURS | `24` | completed job retention |
| CLEANUP_FAILED_JOB_TTL_HOURS | `72` | failed job retention |
| CLEANUP_TEMP_FILE_TTL_HOURS | `6` | temp file retention |

### SMTP/email
| Variable | Default | Purpose |
|---|---|---|
| SMTP_HOST | `smtp.mailtrap.io` fallback in code | SMTP host |
| SMTP_PORT | `2525` fallback in code | SMTP port |
| SMTP_USER | `user` fallback in code | SMTP user |
| SMTP_PASS | `pass` fallback in code | SMTP password |
| SMTP_FROM | fallback sender string | sender identity |
| APP_NAME | `DocAnalyzer` | email branding |

### Migration bootstrap
| Variable | Default | Purpose |
|---|---|---|
| DEFAULT_ADMIN_EMAIL | `admin@local` | seeded admin email |
| DEFAULT_ADMIN_NAME | `Default Admin` | seeded admin name |
| DEFAULT_ADMIN_PASSWORD | generated if blank | seeded admin password |

---

## 9) Error Handling

### Error response format
```json
{
  "ok": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Readable message",
    "retryable": false
  }
}
```

### Global behavior
- Unknown/internal errors are normalized to status `500` with generic message.
- `Zod` validation failures return `422 VALIDATION_ERROR`.
- Invalid JSON returns `400 INVALID_JSON`.
- Oversized body returns `413 PAYLOAD_TOO_LARGE`.
- Unknown route returns `404 NOT_FOUND`.
- `Multer` file-size errors mapped to `UPLOAD_TOO_LARGE`.

### Common error codes
`UNAUTHORIZED`, `VALIDATION_ERROR`, `RATE_LIMITED`, `NOT_FOUND`, `INVALID_PATH_PARAM`, `UPLOAD_TOO_LARGE`, `INVALID_FILE_MIME`, `INVALID_FILE_EXTENSION`, `INVALID_FILE_SIGNATURE`, `SESSION_DOC_LIMIT`, `PDF_NOT_READY`, `INVALID_OTP`, `INVALID_CREDENTIALS`, `INVALID_TOKEN`, `CORS_ORIGIN_BLOCKED`.

### Frontend handling recommendations
1. Branch on HTTP status first (`401`, `403`, `422`, `429`, `500`).
2. Use `error.code` for UX-specific messages.
3. Use `error.retryable` for retry decisions.
4. Respect `Retry-After` header on `429`.
5. Show generic fallback for unknown codes.

---

## 10) System Limits and Protections

### Functional limits
- max docs/session: 5
- upload max size: 30MB
- PDF max pages: 150
- parsed text max length: 2,000,000 chars
- retrieval scan max chunks/query: 2000
- query topK clamped to 1..5
- request message max length: 10,000 chars
- incoming `history` max 100 entries

### Rate limits
- Global `/api/v1`: 100 req/min/IP
- Register: 30 / 15 min
- Login/refresh: 20 / 15 min
- Writes: 80 / min
- Strict reads: 200 / min
- Upload: 16 / min
- Chat: 30 / min

### Security safeguards
- Helmet enabled
- CORS allowlist enforced
- JWT + server-side session check
- SQL uses prepared statements
- upload path traversal protection
- MIME + extension + magic-byte validation
- login brute-force lockout via `login_attempts`
- cleanup worker removes stale temp files/orphan chunks/expired sessions/jobs

### Important integration caveat
CORS methods list is `GET, POST, DELETE, OPTIONS`.  
`PATCH /sessions/:sessionId` exists, but cross-origin browser clients may require backend CORS method update to include `PATCH`.

---

## 11) Project Structure

| Path | Responsibility |
|---|---|
| `src/app.js` | Express app setup, middleware stack, error handlers |
| `src/server.js` | HTTP listen bootstrap + cleanup worker startup |
| `src/config/` | env loading, db bootstrap, logging, Gemini client config |
| `src/routes/` | route wiring and API surface |
| `src/routes/api/v1/` | versioned endpoint modules |
| `src/controllers/` | HTTP orchestration and response shaping |
| `src/services/` | domain/business logic, queue, RAG, auth, upload |
| `src/parsers/` | document text extraction/normalization by type |
| `src/middleware/` | auth, validation, rate limiting |
| `src/database/` | migrations and schema evolution |
| `src/utils/` | generic helpers, SSE helpers, error normalization |
| `src/validations/` | Zod request schemas |
| `src/jobs/` | reserved for background job modules (currently placeholder) |
| `docs/` | architecture/API/deployment docs |

---

## 12) Frontend Integration Guide

### A) Authenticate user
1. Register: `POST /auth/register`
2. Verify OTP: `POST /auth/verify-otp`
3. Login: `POST /auth/login`
4. Store `accessToken` and `refreshToken`
5. Add `Authorization: Bearer <accessToken>` to protected calls

### B) Create a session
`POST /sessions` with optional title.

### C) Upload document
`POST /sessions/:sessionId/pdfs` as multipart:
- field `file` required
- field `title` optional
  Receive `jobId`.

### D) Track indexing completion
Poll `GET /jobs/:jobId` until:
- `status === completed` or
- `status === failed`
  Also `GET /pdfs/:pdfId` to confirm `status`.

### E) Send chat request
- Standard: `POST /sessions/:sessionId/chat`
- Alias: `POST /chat` (must include `sessionId` in body)
  Handle:
- `200` immediate answer
- `202` background job (`jobId` returned)

### F) Stream chat via SSE over fetch
Use `POST /sessions/:sessionId/chat?stream=true` with auth header and parse event stream chunks.

### G) Read/clear history
- `GET /sessions/:sessionId/history?limit=&offset=`
- `DELETE /sessions/:sessionId/history`

### H) Session/document management
- Rename: `PATCH /sessions/:sessionId`
- Delete session: `DELETE /sessions/:sessionId`
- List docs: `GET /sessions/:sessionId/pdfs`
- Delete doc: `DELETE /pdfs/:pdfId?removeFile=true`

### I) Token lifecycle
- Refresh token: `POST /auth/refresh`
- Logout current session: `DELETE /auth/session`
- List/revoke auth sessions via `/auth/sessions`

---

## Request/Response Examples (Key Frontend Calls)

### Login
```json
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

```json
{
  "ok": true,
  "data": {
    "accessToken": "jwt...",
    "refreshToken": "opaque...",
    "expiresAt": "2026-03-20T12:34:56.000Z",
    "user": {
      "id": 1,
      "name": "User",
      "email": "user@example.com",
      "createdAt": "2026-03-10T10:00:00.000Z",
      "updatedAt": "2026-03-10T10:00:00.000Z",
      "isActive": true
    }
  }
}
```

### Create session
```json
POST /api/v1/sessions
{
  "title": "Quarterly Review"
}
```

```json
{
  "ok": true,
  "data": {
    "id": 12,
    "title": "Quarterly Review",
    "createdAt": "ISO",
    "updatedAt": "ISO",
    "lastMessageAt": null,
    "lastMessagePreview": ""
  }
}
```

### Upload file
`multipart/form-data` to `/api/v1/sessions/12/pdfs`

```json
{
  "ok": true,
  "data": {
    "pdfId": 77,
    "sessionId": 12,
    "title": "Q1 Report",
    "status": "processing",
    "jobId": "job_...",
    "progress": 0,
    "stage": "uploading",
    "queuePosition": 1
  }
}
```

### Chat (non-stream)
```json
POST /api/v1/sessions/12/chat
{
  "message": "Summarize the report in 5 bullets.",
  "responseStyle": "structured"
}
```

```json
{
  "ok": true,
  "data": {
    "answer": "...",
    "formattedAnswer": "...",
    "responseSchema": {
      "format": "structured_sections",
      "sections": [
        { "title": "Answer", "content": "..." }
      ]
    },
    "responseStyle": "structured",
    "sources": [
      { "pdfId": 77, "chunkId": "uuid", "score": 0.93 }
    ],
    "usedChunksCount": 5,
    "sessionTitle": "Q1 Review Summary"
  }
}
```

### Job status
```json
GET /api/v1/jobs/job_123
```

```json
{
  "ok": true,
  "data": {
    "id": "job_123",
    "type": "indexPdf",
    "status": "processing",
    "progress": 62,
    "stage": "embedding",
    "createdAt": "ISO",
    "updatedAt": "ISO",
    "queuePosition": 0
  }
}
```

