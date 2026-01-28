# 🔐 Kotlin Secure Serialization

A small Kotlin library that provides **transparent encryption and decryption of sensitive fields** in data models using `kotlinx.serialization` and annotations.

This project demonstrates, with unit tests, how to keep **domain models clean and free of cryptography concerns** while supporting APIs that return **optionally encrypted fields**.

---

## 🧠 The Problem

Some APIs return sensitive fields in two possible ways:

### Plain text response

```json
{
  "id": 1,
  "firstname": "John",
  "lastname": "Smith",
  "email": "john@company.com"
}
```

### Encrypted response

```json
{
  "id": 1,
  "e2e_iv": "some-iv",
  "e2e_firstname": "BASE64_ENCRYPTED_VALUE",
  "e2e_lastname": "BASE64_ENCRYPTED_VALUE",
  "e2e_email": "BASE64_ENCRYPTED_VALUE"
}
```

This usually forces clients to:

- Duplicate fields in models
- Spread cryptography logic across the codebase
- Pollute domain models with transport and security concerns
- Add complex conditional mapping logic

---

## ✅ The Solution

This library provides:

- A generic `SecureSerializer<T>`
- Annotations to mark sensitive fields
- Automatic encryption during serialization
- Automatic decryption during deserialization
- Domain models **always see plain values**
- Zero cryptography logic in your business/domain code

---

## 🏗️ How It Works

### 1. Models implement `Secure`

```kotlin
interface Secure {
    val e2eIv: String?
}
```

---

### 2. Sensitive fields are annotated

```kotlin
@SecureProperty
val firstName: String?
```

---

### 3. A custom serializer is created

```kotlin
object UserSerializer : SecureSerializer<User>(
    dataClass = User::class,
    keyProvider = SampleKeyProvider(),
    cryptoEngine = SampleCryptoEngine(),
)
```

---

### 4. And applied to the model

```kotlin
@Serializable(with = UserSerializer::class)
data class User(...)
```

---

## ✨ Example Model

```kotlin
@Serializable(with = UserSerializer::class)
data class User(
    val id: Int,

    @SecureProperty
    val firstName: String?,

    @SecureProperty
    val lastName: String?,

    @SecureProperty
    val email: String?,

    override val e2eIv: String? = null,
) : Secure
```

---

## 🧪 What Application Code Sees

```kotlin
val user: User = api.getUser()

println(user.firstName) // Always decrypted
println(user.email)     // Always decrypted
```

The application **never needs to know** whether the API returned encrypted or plain fields.

---

## 🔁 Serialization Behavior

### When serializing:

- If a key is available:
  - Generates an IV
  - Encrypts only fields annotated with `@SecureProperty`
  - Writes `e2e_<field>` and `e2e_iv`
- If no key is available:
  - Serializes normally (plain fields)

### When deserializing:

- If `e2e_iv` is present:
  - Decrypts only annotated fields
- Otherwise:
  - Reads plain fields normally

---

## 🧩 Design Goals

- Keep domain models clean
- No duplicated fields in models
- No cryptography logic outside the serializer
- Backward-compatible with non-encrypted APIs
- Fully transparent to application code
- Easy to adapt to different API protocols

---

## 🔧 Customization

You can customize:

- The IV field name:
```kotlin
secureAttr = "iv"
```

- The encrypted field prefix:
```kotlin
secureAttrPrefix = "enc_"
```

---

## 🛠️ Architecture & Concepts

- Kotlin
- kotlinx.serialization
- Custom `KSerializer<T>`
- Reflection (`KClass`, `KType`, `primaryConstructor`)
- Annotation-driven design
- Clean Architecture principles
- Separation of concerns

---

## ⚠️ Important Security Note

The crypto implementation in the `sample` package is **NOT secure** and exists **only to demonstrate** how the library is wired.

Real applications **must provide** a proper `SecureCryptoEngine` implementation using a real cryptography library.

---

## 📦 Project Structure

```
src/main/kotlin/com/github/edwincosta/secureserialization/
 ├── secure/    # Library code
 └── sample/    # Example usage
```

---

## 🚀 Typical Use Cases

- Mobile apps consuming partially encrypted APIs
- Gradual rollout of E2E encryption
- Backward-compatible security layers
- Clean separation between transport and domain layers

---

## 📄 License

MIT

---

## 🧪 Testing

This project includes unit tests that validate:

- Serialization with and without encryption
- Deserialization of encrypted payloads
- Correct handling of secure vs non-secure fields
- Backward compatibility with plain JSON responses

To run the tests:

```bash
./gradlew test
```