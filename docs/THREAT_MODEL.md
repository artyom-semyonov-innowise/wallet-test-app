# Threat Model

Security threats and mitigation measures for the mobile wallet implementation.

## Assets

1. Private Key - Cryptographic key for transaction signing
2. Transaction Integrity - Protection against transaction tampering
3. Replay Protection - Prevention of signature reuse

## Threats and Mitigations

### 1. Private Key Compromise

**Threat**: Unauthorized access to private key enabling arbitrary transaction signing.

**Mitigation**:
- Private key stored exclusively in Android Keystore
- Key never leaves secure hardware/enclave
- JavaScript layer has no access to private key (only signature returned)
- All signing operations performed in native layer
- No key material transmitted over network

**Risk Level**: LOW

---

### 2. Replay Attacks

**Threat**: Reuse of intercepted valid transaction signatures.

**Mitigation**:
- Strictly increasing nonce per transaction
- Last used nonce stored in EncryptedSharedPreferences
- Atomic nonce validation and saving (synchronized) prevents race conditions
- Native layer validates nonce > lastUsedNonce before signing
- Backend validates nonce (defense in depth)

**Risk Level**: LOW

---

### 3. Transaction Tampering

**Threat**: Modification of transaction data before or after signing.

**Mitigation**:
- Canonical transaction format: `amount|currency|nonce`
- Signature computed over canonical data in native layer
- Backend verifies signature before processing
- JavaScript cannot modify signed data without invalidating signature

**Risk Level**: LOW

---

### 4. Key Invalidation

**Threat**: Key deletion or inaccessibility preventing transaction signing.

**Mitigation**:
- Error handling for key not found scenarios
- Application detects key existence
- User can regenerate key pair
- Clear error messages

**Risk Level**: MEDIUM (data loss, not security breach)

---

### 5. Man-in-the-Middle Attacks

**Threat**: Interception and modification of client-backend communication.

**Mitigation**:
- Current: Mock backend (no network security)
- Production: HTTPS/TLS 1.3, certificate pinning, proper SSL validation

**Risk Level**: HIGH (current), LOW (production)

---

### 6. Side-Channel Attacks

**Threat**: Information extraction through timing, power consumption, or cache analysis.

**Mitigation**:
- Hardware-backed Android Keystore (when available)
- Cryptographic operations in secure enclave
- Constant-time operations in underlying libraries

**Risk Level**: LOW

---

### 7. Application-Level Attacks

**Threat**: Malicious code execution or privilege escalation.

**Mitigation**:
- Private key inaccessible even with root access (hardware-backed keystore)
- No sensitive data in JavaScript bundle
- Production: Code obfuscation, root detection recommended

**Risk Level**: MEDIUM

---

### 8. Social Engineering / User Error

**Threat**: User tricked into signing malicious transactions.

**Mitigation**:
- Clear transaction display before signing
- Production: Transaction confirmation screens, biometric authentication
- Nonce prevents accidental duplicate transactions

**Risk Level**: MEDIUM

---

## Security Assumptions

1. Device not completely compromised (root detection recommended for production)
2. Android Keystore security model reliability
3. Backend properly validates signatures and nonces
4. Production uses HTTPS/TLS with certificate validation

## Security Boundaries

- **Secure**: Android Keystore (native layer)
- **Unsafe**: JavaScript layer, network transmission
- **Trust**: Backend signature verification

## Residual Risks

1. Key loss if device lost/damaged (no recovery mechanism)
2. No key migration between devices
3. Single-device nonce management (no multi-device sync)

## Production Recommendations

See `PRODUCTION.md` for production security enhancements.
