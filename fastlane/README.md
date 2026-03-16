# Arcana Android — Fastlane

## Available Lanes

### 🧪 Testing
| Lane | Command | Description |
|------|---------|-------------|
| `test` | `fastlane test` | Unit tests + JaCoCo coverage |
| `lint` | `fastlane lint` | Lint checks |
| `ci` | `fastlane ci` | lint + test (for CI pipelines) |

### 🔨 Build
| Lane | Command | Description |
|------|---------|-------------|
| `build_debug` | `fastlane build_debug` | Debug APK |
| `build_release` | `fastlane build_release` | Release APK (needs keystore env vars) |

### 🚀 Distribution
| Lane | Command | Description |
|------|---------|-------------|
| `deploy_internal` | `fastlane deploy_internal` | Build + upload to Play Store internal track |
| `promote_to_beta` | `fastlane promote_to_beta` | Promote internal → beta |

---

## Setup

```bash
gem install bundler
bundle install
```

## Release Build Env Vars

```bash
export KEYSTORE_FILE=/path/to/arcana.keystore
export KEY_ALIAS=arcana
export KEY_PASSWORD=<key-password>
export STORE_PASSWORD=<store-password>
```

## Play Store Automation

1. Create a Service Account in Google Play Console → Setup → API access
2. Download JSON key → save as `fastlane/google-play-key.json`
3. Uncomment `json_key_file` in `fastlane/Appfile`
4. Run `fastlane deploy_internal`

---

## Notes

- Application ID: `com.example.arcana` (⚠️ must be changed before Play Store submission)
- Current Play Console: Jr. John (Account: 6685985085136085716), app status: Draft
