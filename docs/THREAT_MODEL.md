# Threat Model

This document outlines the security threats considered in the mobile wallet implementation and the measures taken to mitigate them.

## Assets to Protect

1. **Private Key** - The cryptographic private key used to sign transactions
2. **Transaction Integrity** - Ensuring transactions cannot be tampered with
3. **Replay Protection** - Preventing the reuse of valid transaction signatures

## Threat Analysis

### 1. Private Key Compromise

**Threat**: An attacker gains access to the private key, allowing them to sign arbitrary transactions.

**Attack Vectors**:
- Memory extraction from compromised device
- JavaScript layer accessing the private key
- Key storage in insecure locations (SharedPreferences, plain files)
- Key transmission over network

**Mitigation**:
- ✅ Private key is stored exclusively in Android Keystore
- ✅ Private key never leaves the secure hardware (if available) or secure enclave
- ✅ JavaScript layer has no access to private key - only signature is returned
- ✅ Key operations (signing) performed entirely in native layer
- ✅ No key material ever transmitted over network

**Risk Level**: **LOW** (with proper implementation)

---

### 2. Replay Attacks

**Threat**: An attacker intercepts a valid transaction signature and replays it multiple times to execute the same transaction repeatedly.

**Attack Vectors**:
- Network interception of signed transaction
- Malicious backend replaying transactions
- Client-side signature reuse

**Mitigation**:
- ✅ Nonce-based replay protection:
  - Each transaction must include a strictly increasing nonce
  - Last used nonce stored locally (SharedPreferences)
  - Native layer validates nonce > lastUsedNonce before signing
  - Backend should also validate nonce (mock backend demonstrates this)
- ✅ Transaction includes timestamp-like nonce ensuring uniqueness
- ✅ Failed transactions don't increment nonce (prevents gaps)

**Risk Level**: **LOW** (with proper nonce management)

---

### 3. Transaction Tampering

**Threat**: An attacker modifies transaction data (amount, currency) before or after signing.

**Attack Vectors**:
- Modifying transaction JSON in JavaScript layer
- Man-in-the-middle attacks
- Memory manipulation

**Mitigation**:
- ✅ Transaction data is canonicalized before signing (format: `amount|currency|nonce`)
- ✅ Signature is computed over canonical transaction data in native layer
- ✅ JavaScript layer cannot modify signed transaction data without invalidating signature
- ✅ Backend verifies signature before processing (mock shows structure)

**Risk Level**: **LOW**

---

### 4. Key Invalidation/Deletion

**Threat**: Key is deleted or becomes inaccessible, preventing transaction signing.

**Attack Vectors**:
- Factory reset
- App uninstallation
- Keystore corruption
- Device lockout exceeding attempts

**Mitigation**:
- ✅ Error handling for key not found scenarios
- ✅ Clear error messages to user
- ✅ Application can detect key existence and prompt for regeneration
- ✅ User can regenerate key pair if needed

**Risk Level**: **MEDIUM** (data loss, but not security breach)

---

### 5. Man-in-the-Middle (MITM) Attacks

**Threat**: Attacker intercepts and modifies communication between client and backend.

**Attack Vectors**:
- Network interception
- Compromised backend
- SSL/TLS downgrade attacks

**Mitigation**:
- ⚠️ **Current Implementation**: Mock backend (no network security)
- ✅ **Production Requirements**:
  - HTTPS/TLS 1.3 for all API communication
  - Certificate pinning
  - Proper SSL validation
  - Backend signature verification

**Risk Level**: **HIGH** (in current mock, but addressed in production design)

---

### 6. Side-Channel Attacks

**Threat**: Attacker extracts key information through timing, power consumption, or cache analysis.

**Attack Vectors**:
- Timing attacks on cryptographic operations
- Power analysis
- Cache timing attacks

**Mitigation**:
- ✅ Android Keystore uses hardware-backed security (when available)
- ✅ Cryptographic operations performed in secure enclave
- ✅ Constant-time operations in underlying cryptographic libraries

**Risk Level**: **LOW** (hardware-backed keystore mitigates)

---

### 7. Application-Level Attacks

**Threat**: Malicious code execution or privilege escalation in the application.

**Attack Vectors**:
- Code injection
- Root/jailbreak access
- Reverse engineering

**Mitigation**:
- ✅ Private key not accessible even with root access (hardware-backed keystore)
- ⚠️ Code obfuscation recommended for production
- ⚠️ Root detection recommended for production
- ✅ No sensitive data in JavaScript bundle

**Risk Level**: **MEDIUM** (mitigated by Keystore, but app security should be hardened)

---

### 8. Social Engineering / User Error

**Threat**: User is tricked into signing malicious transactions or leaking credentials.

**Attack Vectors**:
- Phishing attacks
- Malicious transaction prompts
- User confusion

**Mitigation**:
- ✅ Clear transaction display before signing
- ⚠️ **Production**: Add transaction confirmation screens
- ⚠️ **Production**: Add user authentication (biometrics/PIN) for signing
- ✅ Nonce prevents accidental duplicate transactions

**Risk Level**: **MEDIUM** (requires user education and UI improvements)

---

## Security Assumptions

1. **Device Security**: Assumes device is not completely compromised (rooted/jailbroken detection recommended for production)
2. **Android Keystore**: Relies on Android Keystore security model
3. **Backend Trust**: Assumes backend properly validates signatures and nonces (mock demonstrates structure)
4. **Network Security**: Production must use HTTPS/TLS with proper certificate validation

## Security Boundaries

- **Secure Boundary**: Android Keystore (native layer)
- **Unsafe Boundary**: JavaScript layer, network transmission
- **Trust Boundary**: Backend signature verification

## Residual Risks

1. **Key Recovery**: If device is lost/damaged, private key cannot be recovered (by design, but may be unacceptable for users)
2. **Key Migration**: No mechanism for key migration between devices
3. **Multi-device Sync**: Nonce management doesn't support multiple devices (would need backend coordination)

## Recommendations for Production

See `PRODUCTION.md` for detailed recommendations on addressing residual risks and enhancing security for production deployment.
