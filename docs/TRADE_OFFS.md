# Trade-offs and Design Decisions

Key design decisions and trade-offs in the wallet implementation.

## 1. Android Keystore vs. Software Key Storage

**Decision**: Android Keystore System

**Advantages**:
- Hardware-backed storage (when available)
- Keys never leave secure enclave
- Protection against memory extraction
- OS-level security guarantees

**Disadvantages**:
- Key cannot be exported/recovered if device lost
- Requires Android API 23+
- Some devices fall back to software keystore

**Justification**: Security prioritization. Non-recoverable keys prevent key theft.

---

## 2. ECDSA secp256r1 vs. secp256k1

**Decision**: secp256r1 (P-256)

**Advantages**:
- Native Android Keystore support
- No additional dependencies
- Sufficient security (128-bit)

**Disadvantages**:
- Not compatible with Bitcoin/Ethereum (use secp256k1)
- May require curve change for blockchain integration

**Justification**: Adequate security and simplicity for test/demo. Use secp256k1 for blockchain production.

---

## 3. Nonce Storage: EncryptedSharedPreferences vs. Keystore

**Decision**: EncryptedSharedPreferences

**Advantages**:
- Encrypted storage
- Fast access
- Prevents casual tampering

**Disadvantages**:
- Can be cleared with app data
- Vulnerable to determined attackers with root access

**Justification**: Nonce not secret (included in transactions). Security from validation logic and atomic operations.

---

## 4. Classic Native Module vs. TurboModule

**Decision**: ReactContextBaseJavaModule (Classic)

**Advantages**:
- Simpler implementation
- Compatible with current React Native
- Easier to debug

**Disadvantages**:
- Not leveraging new architecture
- May require future migration

**Justification**: Sufficient for test application. Migration possible without core logic changes.

---

## 5. In-Process Mock Backend vs. Separate Server

**Decision**: In-process mock backend

**Advantages**:
- No server setup required
- Easier testing and demonstration
- No network dependencies

**Disadvantages**:
- Not representative of network conditions
- No actual cryptographic signature verification

**Justification**: Sufficient for test/demo. Structure allows easy replacement with real API.

---

## 6. Error Handling: Exceptions vs. Error Codes

**Decision**: Custom exceptions with clear messages

**Advantages**:
- Clear error messages for debugging
- Type-safe error handling
- Easy to extend

**Disadvantages**:
- Messages may leak implementation details (sanitize for production)

**Justification**: Clear messages valuable for development. Production can sanitize while maintaining codes.

---

## 7. Transaction Format: JSON vs. Binary

**Decision**: JSON structure, canonical string for signing

**Advantages**:
- Human-readable
- Easy JavaScript integration
- Canonical format prevents signature malleability

**Disadvantages**:
- Larger size than binary
- Requires JSON parsing

**Justification**: Standard, sufficient, canonical format ensures deterministic signing.

---

## 8. Signature Encoding: Base64 vs. Hex

**Decision**: Base64 encoding

**Advantages**:
- 33% more compact than hex
- Standard for binary data
- JavaScript-friendly

**Disadvantages**:
- Less readable than hex

**Justification**: More efficient and standard for API communication.

---

## 9. Nonce Validation: Client-Only vs. Client + Server

**Decision**: Both client and server with atomic operations

**Advantages**:
- Defense in depth
- Client validation improves UX
- Server validation prevents client bypass
- Synchronized atomic operations prevent race conditions

**Disadvantages**:
- Requires client-server coordination
- Synchronization overhead (negligible)

**Justification**: Security best practice. Atomic operations prevent nonce reuse/gaps.

---

## 10. Key Generation: On-Demand vs. Automatic

**Decision**: On-demand (user-triggered)

**Advantages**:
- User control and intent
- Allows user education

**Disadvantages**:
- Extra user step

**Justification**: Explicit action appropriate for security-critical operation.

---

## Summary

Design prioritizes:
1. **Security** - Private keys never leave secure storage
2. **Simplicity** - Straightforward implementation
3. **Clarity** - Easy to understand and demonstrate

Trade-offs favor security and simplicity over advanced features.
