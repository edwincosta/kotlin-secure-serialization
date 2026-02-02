# Changelog

## 1.1.0-RC2 - 2026-02-02
- Changed: `SecureCryptoEngine.encrypt` and `SecureCryptoEngine.decrypt` now return nullable `String?` to allow signaling failures.
- Changed: version bumped from 1.1.0-RC1 to 1.1.0-RC2.

## 1.1.0-RC1 - 2026-02-02
- Added: hook to customize deserialization of decrypted non-primitive types via `deserializeNonPrimitiveKind` in `SecureSerializer`.
- Changed: version bumped from 1.0.0 to 1.1.0-RC1.
