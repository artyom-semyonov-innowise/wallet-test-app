# Trade-offs and Design Decisions

This document explains the key design decisions made in the wallet implementation and the trade-offs involved.

## 1. Android Keystore vs. Software Key Storage

**Decision**: Use Android Keystore System

**Trade-offs**:

✅ **Advantages**:
- Private keys stored in secure hardware (when available)
- Keys never leave secure enclave
- Protection against memory extraction attacks
- OS-level security guarantees
- No need to implement key storage encryption manually

❌ **Disadvantages**:
- Key cannot be exported/recovered if device is lost
- Requires Android API 23+ (Android 6.0+)
- Some devices may not have hardware-backed keystore (falls back to software)
- Less control over key storage format

**Alternative Considered**: Software key storage with encryption
- Would allow key recovery/backup
- More complex to implement securely
- Higher risk of key compromise

**Justification**: Security is paramount for wallet applications. The inability to recover keys is a feature (prevents key theft) rather than a bug, though user experience may require key backup strategies (see Production.md).

---

## 2. ECDSA secp256r1 vs. secp256k1

**Decision**: Use secp256r1 (P-256) curve

**Trade-offs**:

✅ **Advantages**:
- Native support in Android Keystore
- No additional dependencies
- Standard curve with wide support
- Sufficient security for wallet use cases (128-bit security)

❌ **Disadvantages**:
- Not the same curve used by Bitcoin (secp256k1)
- May not be compatible with existing blockchain systems expecting secp256k1

**Alternative Considered**: secp256k1
- Would be compatible with Bitcoin/Ethereum ecosystems
- Requires external library (BouncyCastle or similar)
- More complex implementation

**Justification**: For a test/demo application, secp256r1 provides adequate security and simplicity. For production integration with specific blockchains, secp256k1 may be required.

---

## 3. Nonce Storage: SharedPreferences vs. Keystore

**Decision**: Store last used nonce in SharedPreferences

**Trade-offs**:

✅ **Advantages**:
- Simple implementation
- Fast access (no cryptographic operations)
- Easy to read/write
- Sufficient for single-device use case

❌ **Disadvantages**:
- Not encrypted by default (though can use EncryptedSharedPreferences)
- Could be cleared with app data
- Not protected against tampering (though nonce validation still works)

**Alternative Considered**: Store nonce in Android Keystore
- More secure storage
- Overkill for non-sensitive data
- Slower access
- More complex implementation

**Justification**: Nonce is not sensitive information (it's included in transactions). The security comes from validation logic, not storage encryption. SharedPreferences is sufficient for this use case.

---

## 4. Classic Native Module vs. TurboModule (New Architecture)

**Decision**: Use ReactContextBaseJavaModule (Classic)

**Trade-offs**:

✅ **Advantages**:
- Simpler implementation
- Works with current React Native setup
- Less boilerplate code
- Easier to debug
- Compatible with existing React Native ecosystem

❌ **Disadvantages**:
- Not leveraging new architecture benefits
- May need migration in future
- Slightly less performant (negligible for this use case)

**Alternative Considered**: TurboModule
- Better performance
- Type-safe bridge
- Future-proof (React Native direction)
- More setup complexity

**Justification**: The new architecture is enabled in the project, but for a test application, the classic approach is simpler and sufficient. Migration to TurboModule can be done if needed without changing the core wallet logic.

---

## 5. In-Process Mock Backend vs. Separate Server

**Decision**: In-process mock backend service

**Trade-offs**:

✅ **Advantages**:
- No additional server setup required
- Easier to test and demonstrate
- No network dependencies
- Faster development

❌ **Disadvantages**:
- Not representative of real network conditions
- No actual cryptographic signature verification
- Shared memory space (for demo purposes, acceptable)

**Alternative Considered**: Separate Express/Node.js server
- More realistic
- Could implement actual signature verification
- Better separation of concerns
- More complex setup

**Justification**: For a test/demo application, an in-process mock is sufficient to demonstrate the flow. The structure allows easy replacement with real API calls.

---

## 6. Error Handling: Exceptions vs. Error Codes

**Decision**: Use custom exceptions with clear messages

**Trade-offs**:

✅ **Advantages**:
- Clear error messages for debugging
- Type-safe error handling in Kotlin
- Easy to extend with error codes
- Informative for developers

❌ **Disadvantages**:
- Error messages may leak implementation details (could be sanitized for production)
- Requires proper exception handling throughout

**Alternative Considered**: Error code enum
- More structured
- Easier to internationalize
- Less informative without lookup table

**Justification**: Clear error messages are valuable for development and debugging. Production version can sanitize messages while maintaining error codes.

---

## 7. Transaction Format: JSON vs. Binary

**Decision**: Use JSON for transaction structure, canonical string for signing

**Trade-offs**:

✅ **Advantages**:
- Human-readable
- Easy to work with in JavaScript
- Flexible structure
- Canonical format prevents signature malleability

❌ **Disadvantages**:
- Larger size than binary
- Requires JSON parsing
- Canonical format must be consistent

**Alternative Considered**: Binary protocol buffer format
- Smaller size
- Faster parsing
- More complex implementation
- Less human-readable

**Justification**: JSON is standard, easy to work with, and sufficient for this use case. Canonical string format ensures deterministic signing.

---

## 8. Signature Encoding: Base64 vs. Hex

**Decision**: Base64 encoding

**Trade-offs**:

✅ **Advantages**:
- More compact than hex (33% smaller)
- Standard encoding for binary data
- Easy to work with in JavaScript
- Compatible with web APIs

❌ **Disadvantages**:
- Slightly less readable than hex
- Requires decoding on backend

**Alternative Considered**: Hexadecimal encoding
- Human-readable
- Larger size
- Common in blockchain/crypto contexts

**Justification**: Base64 is more efficient and standard for API communication. Hex could be used if compatibility with specific systems is required.

---

## 9. Nonce Validation: Client-Only vs. Client + Server

**Decision**: Validate on both client and server

**Trade-offs**:

✅ **Advantages**:
- Defense in depth
- Client-side validation prevents unnecessary backend calls
- Server-side validation prevents bypassing client checks
- Follows security best practices

❌ **Disadvantages**:
- Requires coordination between client and server nonce state
- More complex implementation

**Alternative Considered**: Server-only validation
- Single source of truth
- Client could send invalid nonces unnecessarily
- Less efficient

**Justification**: Defense in depth is a security best practice. Client-side validation improves UX (immediate feedback), while server-side validation provides security even if client is compromised.

---

## 10. Key Generation: On-Demand vs. On First Launch

**Decision**: On-demand (user triggers generation)

**Trade-offs**:

✅ **Advantages**:
- User has control
- Clear user intent
- Can be triggered when needed
- Allows for user education before key generation

❌ **Disadvantages**:
- Extra step for user
- Key might not exist when needed

**Alternative Considered**: Automatic generation on first launch
- Seamless UX
- Key always available
- Less user control
- May generate key unnecessarily

**Justification**: For a wallet application, explicit user action for key generation is appropriate from both security and UX perspectives. Users should understand that key generation is a security-critical operation.

---

## Summary

The design prioritizes:
1. **Security** - Private keys never leave secure storage
2. **Simplicity** - Straightforward implementation for test/demo
3. **Clarity** - Easy to understand and demonstrate concepts

Trade-offs generally favor security and simplicity over advanced features, which is appropriate for a test application demonstrating core wallet security concepts.
