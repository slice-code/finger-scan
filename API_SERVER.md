# API Server — Absensi Finger Reader

## Tech stack

- **Database**: PostgreSQL
- **Language**: any (Node.js, Go, Python, Java, PHP, Rust, etc.)
- **Transport**: HTTPS REST (JSON)

---

## Table of contents

1. [Complete API flow](#complete-api-flow)
2. [`GET /employees`](#1-get-employees)
3. [`POST /register`](#2-post-base_urlregister)
4. [`POST {attendance_url}`](#3-post-attendance_url)
5. [Encryption & decryption](#encryption--decryption-flow)
6. [Matching logic](#matching-logic)
7. [PostgreSQL schema](#postgresql-schema)
8. [Sample implementation (Node.js)](#sample-implementation-nodejs)
9. [Sample implementation (Python)](#sample-implementation-python)
10. [Client defaults & settings](#client-defaults)
11. [Security notes](#security-notes)

---

## Complete API flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    ANDROID APP                                   │
│                                                                  │
│  REGISTRASI:                                                     │
│    GET /employees  ──────────────────────►  Server returns       │
│    (ambil daftar karyawan)                └── [{id, nama}, ...]  │
│         │                                                       │
│    User pilih karyawan + biometric                               │
│         │                                                       │
│    POST /register  ───────────────────────►  Server stores       │
│    AES({employeeId, fingerprintHash})      └── employeeId + hash │
│                                                                  │
│  SCAN/ABSENSI:                                                   │
│    User biometric + hash generation                              │
│         │                                                       │
│    POST {attendance_url}  ───────────────►  Server matches       │
│    AES({fingerprintHash})                 └── hash → employee    │
│                                            └── record attendance │
│         │                                                       │
│    ◄── {status: "matched", employeeId, name}                    │
│    ◌── {status: "not_found", message: "..."}                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. `GET /employees`

Returns the employee list for the Registration screen.  
**No authentication required** on this endpoint (the server seeds the employee data).

| Item | Value |
|---|---|
| Method | `GET` |
| Full path | `{base_url}/employees` |
| Default base URL | `https://api.absensi-online.com/v1` |

### Response `200 OK`

```json
[
  { "id": "EMP001", "nama": "John Doe" },
  { "id": "EMP002", "nama": "Jane Smith" },
  { "id": "EMP003", "nama": "Budi Santoso" }
]
```

| Field | Type | Max | Description |
|---|---|---|---|
| `id` | `string` | 50 | Unique employee identifier (NIP/kode karyawan) |
| `nama` | `string` | 200 | Employee full name |

### Response `5xx`

```json
{ "error": "Internal server error" }
```

The app will show "Gagal memuat data" and a retry button.

### How to seed

Populate the `employees` table with your existing HR data. The `id` field MUST match the `employee_id` column in other tables.

```sql
INSERT INTO employees (employee_id, nama) VALUES
  ('EMP001', 'John Doe'),
  ('EMP002', 'Jane Smith');
```

---

## 2. `POST {base_url}/register`

Registers a fingerprint hash for an employee. Called after the user authenticates via the device biometric prompt.

### Request

| Header | Value | Always |
|---|---|---|
| `Content-Type` | `application/json` | yes |
| `X-Signature` | HMAC-SHA256 Base64 of `encryptedPayload` | yes |
| `X-Encryption-Key` | `AES-256` | yes |

**HTTP body:**

```json
{
  "encryptedPayload": "r7Yx...base64...==",
  "signature": "aB3d...base64...==",
  "timestamp": 1719561600000
}
```

**Decrypted payload** (after AES-256 decrypt):

```json
{
  "employeeId": "EMP001",
  "fingerprintHash": "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890"
}
```

### Response

| Status | Meaning |
|---|---|
| `200 OK` | Fingerprint registered |
| `401 Unauthorized` | HMAC signature invalid |
| `422 Unprocessable` | Missing or invalid fields |

**Success `200`:**

```json
{ "status": "ok", "message": "registered" }
```

**Error `422`:**

```json
{ "status": "error", "message": "employeeId and fingerprintHash required" }
```

### Server logic

```text
1. Verify X-Signature HMAC
2. Decrypt encryptedPayload with AES-256
3. Parse JSON → {employeeId, fingerprintHash}
4. Validate: employeeId must exist in employees table
5. UPDATE employees SET fingerprint_hash = ? WHERE employee_id = ?
6. Return {status: "ok"}
```

---

## 3. `POST {attendance_url}`

Sends a fingerprint hash for **server-side matching** and attendance recording.  
This is the main attendance check-in endpoint.

| Item | Value |
|---|---|
| Method | `POST` |
| Full path | configurable in app settings |
| Default | `https://api.absensi-online.com/v1/attendance` |

### Request

Same encrypted envelope as register:

```json
{
  "encryptedPayload": "sD8f...base64...==",
  "signature": "xYz1...base64...==",
  "timestamp": 1719561600000
}
```

**Decrypted payload:**

```json
{
  "fingerprintHash": "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890"
}
```

### Responses

**✅ Match found `200`:**

```json
{
  "status": "matched",
  "employeeId": "EMP001",
  "name": "John Doe"
}
```

**❌ No match `200`:**

```json
{
  "status": "not_found",
  "message": "Sidik jari tidak dikenal"
}
```

**🔴 Server error:**

| Code | Body |
|---|---|
| `401` | `{"status": "error", "message": "invalid signature"}` |
| `422` | `{"status": "error", "message": "fingerprintHash required"}` |
| `500` | `{"status": "error", "message": "internal error"}` |

### Server logic

```text
1. Verify X-Signature HMAC
2. Decrypt encryptedPayload with AES-256
3. Parse JSON → {fingerprintHash}
4. SELECT * FROM employees WHERE fingerprint_hash = ?
5. If found:
   a. INSERT INTO attendance_logs (employee_id, name, check_in_at, ...)
   b. Return {status: "matched", employeeId, name}
6. If not found:
   a. Return {status: "not_found", message: "Sidik jari tidak dikenal"}
```

---

## Encryption & decryption flow

Both `POST /register` and `POST {attendance_url}` use the same encryption scheme.

### Key derivation

```
secret_key = "AbsensiSecureSecret2026!"     ← UTF-8 string, configurable

AES-256 key = SHA-256(secret_key)          → 32 bytes (hex: 2c...)
HMAC key    = secret_key.getBytes("UTF-8") → raw bytes, NOT hashed
```

### HMAC signature verification (server)

The HMAC signs the **encryptedPayload string**, not the decrypted plaintext.

```
signature = Base64( HMAC-SHA256(encryptedPayload, raw_secret_key_bytes) )
```

**Node.js:**

```javascript
const crypto = require('crypto');

function verifyHmac(encryptedPayload, signature, secretKey) {
  const expected = crypto
    .createHmac('sha256', secretKey)
    .update(encryptedPayload)
    .digest('base64');
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
}
```

**Python:**

```python
import hmac, hashlib, base64

def verify_hmac(encrypted_payload: str, signature: str, secret_key: str) -> bool:
    expected = base64.b64encode(
        hmac.new(
            secret_key.encode('utf-8'),
            encrypted_payload.encode('utf-8'),
            hashlib.sha256
        ).digest()
    ).decode()
    return hmac.compare_digest(expected, signature)
```

**Go:**

```go
import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/base64"
)

func verifyHMAC(payload, signature, secret string) bool {
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(payload))
    expected := base64.StdEncoding.EncodeToString(mac.Sum(nil))
    return hmac.Equal([]byte(expected), []byte(signature))
}
```

### AES-256 decryption (server)

The algorithm is `AES/ECB/PKCS5Padding` (Java name). In other languages, use `AES-128-ECB` with a 256-bit key and PKCS#7 padding.

**Node.js:**

```javascript
const crypto = require('crypto');

function decryptPayload(encryptedBase64, secretKey) {
  const aesKey = crypto.createHash('sha256').update(secretKey).digest(); // 32 bytes
  const decipher = crypto.createDecipheriv('aes-256-ecb', aesKey, null);
  decipher.setAutoPadding(true);
  const encrypted = Buffer.from(encryptedBase64, 'base64');
  const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]);
  return JSON.parse(decrypted.toString('utf-8'));
}
```

**Python:**

```python
import hashlib, base64, json
from Crypto.Cipher import AES

def decrypt_payload(encrypted_b64: str, secret_key: str) -> dict:
    key = hashlib.sha256(secret_key.encode('utf-8')).digest()  # 32 bytes
    cipher = AES.new(key, AES.MODE_ECB)
    raw = cipher.decrypt(base64.b64decode(encrypted_b64))
    # Strip PKCS#7 padding
    pad_len = raw[-1]
    plaintext = raw[:-pad_len].decode('utf-8')
    return json.loads(plaintext)
```

**Go:**

```go
import (
    "crypto/aes"
    "crypto/sha256"
    "encoding/base64"
    "encoding/json"
)

func decryptPayload(encryptedB64, secretKey string) (map[string]interface{}, error) {
    key := sha256.Sum256([]byte(secretKey))
    cipher, _ := aes.NewCipher(key[:])
    encrypted, _ := base64.StdEncoding.DecodeString(encryptedB64)
    decrypted := make([]byte, len(encrypted))
    cipher.Decrypt(decrypted, encrypted)
    // Note: ECB mode requires manual block-by-block decryption in Go
    // See full implementation notes below
    ...
}
```

> **Note for Go**: The standard library `crypto/aes` does not provide ECB mode directly. Use a helper function that decrypts block by block and strips PKCS7 padding.

**Java:**

```java
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public static String decrypt(String encryptedB64, String secretKey) throws Exception {
    MessageDigest sha = MessageDigest.getInstance("SHA-256");
    byte[] keyBytes = sha.digest(secretKey.getBytes("UTF-8"));
    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedB64));
    return new String(decrypted, "UTF-8");
}
```

### Full request parsing (pseudocode for any language)

```
function handleAttendancePost(request):
    // 1. Read headers
    signature = request.header("X-Signature")
    encryptionKeyVersion = request.header("X-Encryption-Key")  // "AES-256"
    
    // 2. Read body
    body = request.json()
    encryptedPayload = body.encryptedPayload
    clientSignature = body.signature
    timestamp = body.timestamp
    
    // 3. Verify HMAC
    if not verifyHmac(encryptedPayload, clientSignature, SECRET_KEY):
        return 401 {status: "error", message: "invalid signature"}
    
    // 4. Decrypt
    plaintext = decryptAES(encryptedPayload, SECRET_KEY)
    data = JSON.parse(plaintext)
    
    // 5. Optional: check timestamp is recent (±5 min)
    if abs(now() - timestamp) > 300000:
        return 422 {status: "error", message: "timestamp expired"}
    
    // 6. Process (register or match)
    return processRequest(data)
```

---

## Matching logic

For the attendance endpoint, after decryption:

```sql
-- Find the employee by fingerprint hash
SELECT employee_id, nama FROM employees
WHERE fingerprint_hash = :hash
LIMIT 1;
```

If found:

```sql
INSERT INTO attendance_logs (employee_id, name, check_in_at, encrypted_payload, signature, raw_timestamp)
VALUES (:employeeId, :name, NOW(), :encryptedPayload, :signature, :timestamp);
```

Then return:

```json
{ "status": "matched", "employeeId": "EMP001", "name": "John Doe" }
```

If not found:

```json
{ "status": "not_found", "message": "Sidik jari tidak dikenal" }
```

---

## PostgreSQL schema

### Table: `employees`

```sql
CREATE TABLE employees (
    id               SERIAL PRIMARY KEY,
    employee_id      VARCHAR(50) UNIQUE NOT NULL,
    nama             VARCHAR(200) NOT NULL,
    fingerprint_hash VARCHAR(128),        -- SHA-256 hex (64 chars), NULL until registered
    registered_at    TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_employees_fingerprint ON employees(fingerprint_hash);
```

### Table: `attendance_logs`

```sql
CREATE TABLE attendance_logs (
    id                SERIAL PRIMARY KEY,
    employee_id       VARCHAR(50) NOT NULL REFERENCES employees(employee_id),
    name              VARCHAR(200) NOT NULL,
    check_in_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    encrypted_payload TEXT,
    signature         TEXT,
    raw_timestamp     BIGINT,              -- original client-side millis timestamp
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_logs_employee ON attendance_logs(employee_id);
CREATE INDEX idx_logs_date ON attendance_logs(check_in_at);
```

---

## Sample implementation (Node.js)

```javascript
const express = require('express');
const crypto = require('crypto');
const { Pool } = require('pg');

const app = express();
const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const SECRET_KEY = process.env.SECRET_KEY || 'AbsensiSecureSecret2026!';

app.use(express.json());

function verifyHmac(payload, signature) {
  const expected = crypto.createHmac('sha256', SECRET_KEY)
    .update(payload).digest('base64');
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
}

function decrypt(encryptedB64) {
  const key = crypto.createHash('sha256').update(SECRET_KEY).digest();
  const decipher = crypto.createDecipheriv('aes-256-ecb', key, null);
  const enc = Buffer.from(encryptedB64, 'base64');
  const dec = Buffer.concat([decipher.update(enc), decipher.final()]);
  return JSON.parse(dec.toString());
}

// GET /employees
app.get('/employees', async (req, res) => {
  const result = await pool.query('SELECT employee_id AS id, nama FROM employees ORDER BY nama');
  res.json(result.rows);
});

// POST /register
app.post('/register', async (req, res) => {
  const { encryptedPayload, signature } = req.body;
  if (!verifyHmac(encryptedPayload, signature))
    return res.status(401).json({ status: 'error', message: 'invalid signature' });

  const data = decrypt(encryptedPayload);
  const { employeeId, fingerprintHash } = data;

  if (!employeeId || !fingerprintHash)
    return res.status(422).json({ status: 'error', message: 'employeeId and fingerprintHash required' });

  await pool.query(
    'UPDATE employees SET fingerprint_hash = $1, updated_at = NOW() WHERE employee_id = $2',
    [fingerprintHash, employeeId]
  );

  res.json({ status: 'ok', message: 'registered' });
});

// POST /attendance
app.post('/attendance', async (req, res) => {
  const { encryptedPayload, signature, timestamp } = req.body;
  if (!verifyHmac(encryptedPayload, signature))
    return res.status(401).json({ status: 'error', message: 'invalid signature' });

  const data = decrypt(encryptedPayload);
  const { fingerprintHash } = data;

  if (!fingerprintHash)
    return res.status(422).json({ status: 'error', message: 'fingerprintHash required' });

  const result = await pool.query(
    'SELECT employee_id, nama FROM employees WHERE fingerprint_hash = $1 LIMIT 1',
    [fingerprintHash]
  );

  if (result.rows.length === 0)
    return res.json({ status: 'not_found', message: 'Sidik jari tidak dikenal' });

  const { employee_id, nama } = result.rows[0];

  await pool.query(
    `INSERT INTO attendance_logs (employee_id, name, check_in_at, encrypted_payload, signature, raw_timestamp)
     VALUES ($1, $2, NOW(), $3, $4, $5)`,
    [employee_id, nama, encryptedPayload, signature, timestamp]
  );

  res.json({ status: 'matched', employeeId: employee_id, name: nama });
});

app.listen(3000, () => console.log('Server running on port 3000'));
```

---

## Sample implementation (Python / Flask)

```python
import hashlib, hmac, base64, json
from flask import Flask, request, jsonify
from Crypto.Cipher import AES
import psycopg2

app = Flask(__name__)
SECRET_KEY = "AbsensiSecureSecret2026!"

def get_db():
    return psycopg2.connect(host=..., dbname=..., user=..., password=...)

def verify_hmac(payload: str, signature: str) -> bool:
    expected = base64.b64encode(
        hmac.new(SECRET_KEY.encode(), payload.encode(), hashlib.sha256).digest()
    ).decode()
    return hmac.compare_digest(expected, signature)

def decrypt(encrypted_b64: str) -> dict:
    key = hashlib.sha256(SECRET_KEY.encode()).digest()
    cipher = AES.new(key, AES.MODE_ECB)
    raw = cipher.decrypt(base64.b64decode(encrypted_b64))
    pad = raw[-1]
    return json.loads(raw[:-pad].decode())

@app.route('/employees', methods=['GET'])
def get_employees():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT employee_id, nama FROM employees ORDER BY nama")
    rows = [{"id": r[0], "nama": r[1]} for r in cur.fetchall()]
    cur.close(); conn.close()
    return jsonify(rows)

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    if not verify_hmac(data['encryptedPayload'], data['signature']):
        return jsonify({"status": "error", "message": "invalid signature"}), 401
    payload = decrypt(data['encryptedPayload'])
    eid, fh = payload['employeeId'], payload['fingerprintHash']
    conn = get_db(); cur = conn.cursor()
    cur.execute("UPDATE employees SET fingerprint_hash = %s WHERE employee_id = %s", (fh, eid))
    conn.commit(); cur.close(); conn.close()
    return jsonify({"status": "ok", "message": "registered"})

@app.route('/attendance', methods=['POST'])
def attendance():
    data = request.json
    if not verify_hmac(data['encryptedPayload'], data['signature']):
        return jsonify({"status": "error", "message": "invalid signature"}), 401
    payload = decrypt(data['encryptedPayload'])
    fh = payload['fingerprintHash']
    conn = get_db(); cur = conn.cursor()
    cur.execute("SELECT employee_id, nama FROM employees WHERE fingerprint_hash = %s", (fh,))
    row = cur.fetchone()
    if not row:
        cur.close(); conn.close()
        return jsonify({"status": "not_found", "message": "Sidik jari tidak dikenal"})
    eid, name = row
    cur.execute("""INSERT INTO attendance_logs (employee_id, name, check_in_at, encrypted_payload, signature, raw_timestamp)
                   VALUES (%s, %s, NOW(), %s, %s, %s)""",
               (eid, name, data['encryptedPayload'], data['signature'], data.get('timestamp')))
    conn.commit(); cur.close(); conn.close()
    return jsonify({"status": "matched", "employeeId": eid, "name": name})

if __name__ == '__main__':
    app.run(port=3000)
```

---

## Client defaults

Configured in `MainViewModel.kt`:

| Setting | Default value | Changed in |
|---|---|---|
| API Base URL | `https://api.absensi-online.com/v1` | Settings tab |
| Attendance URL | `https://api.absensi-online.com/v1/attendance` | Settings tab |
| Secret key | `AbsensiSecureSecret2026!` | Settings tab |
| Offline mode | `false` | Settings tab |

All settings can be changed at runtime via the **Settings** tab (Admin).  
The app uses the **Base URL** for `/employees` and `/register`.  
The **Attendance URL** is independently configurable.

---

## Security notes

### Hash algorithm (fingerprintHash)

```
fingerprintHash = SHA-256("FINGERPRINT_TEMPLATE_V1|{employeeId}")
```

- Deterministic — same `employeeId` always produces the same hash.
- No random seed, no name in the hash.
- This allows the server to match reliably.
- The hash is NOT a real biometric template; it is a simulated identifier.

### HMAC vs AES key difference

| Key | Derivation | Used for |
|---|---|---|
| AES key | `SHA-256(secretKey)` → 32 bytes | AES-256/ECB/PKCS5Payload encryption/decryption |
| HMAC key | `secretKey` as raw UTF-8 bytes | HMAC-SHA256 signature of the encrypted payload |

### Security recommendations (production)

1. **Rotate the secret key** periodically. Signal the active key version via the `X-Encryption-Key` header (e.g., `AES-256-v2`). The app sends `AES-256` by default.
2. **Use HTTPS only** — the encrypted payload is still encrypted, but headers and metadata are visible without TLS.
3. **Timestamp validation** — reject requests with a timestamp older than ±5 minutes to prevent replay attacks.
4. **Rate limiting** — apply per-IP or per-fingerprint rate limiting to prevent brute force.
5. **Never log the secret key** or the decrypted `fingerprintHash` in plain text.
6. **The `GET /employees` endpoint** has no authentication. In production, add an API key or IP whitelist if this is a concern.

### Error responses summary

| HTTP Status | Meaning | Client handling |
|---|---|---|
| `200` | Success (register or matched) | Normal success flow |
| `200` with `status: "not_found"` | Fingerprint unrecognized | Show "Sidik jari tidak dikenal" |
| `401` | HMAC signature mismatch | Log error, contact admin |
| `422` | Missing field in payload | Log error, contact admin |
| `5xx` | Server error | Show "Server error, coba lagi" |

---

## Notes for implementers

- The app's local Room DB is a **cache** only. The server is the authoritative source for fingerprint matching.
- The `attendance_logs` table stores the raw encrypted payload and signature for audit purposes.
- The `fingerprint_hash` column in `employees` is `NULL` until the user registers a fingerprint via the app.
- To re-register a fingerprint, the app will send a new hash. The server should `UPDATE` (not `INSERT`) the fingerprint.
- The `GET /employees` endpoint should return ONLY employees who are active and have not yet registered a fingerprint, OR all employees. The app filters locally if needed.
- If you change the secret key, update both the server and all app instances. There is no key exchange protocol.
