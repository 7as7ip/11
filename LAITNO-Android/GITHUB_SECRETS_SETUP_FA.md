# راه‌اندازی GitHub Secrets برای امضای APK

## گام ۱: خود keystore رو ایجاد کن (اختیاری)

اگه می‌خواهی keystore دلخود رو استفاده کنی:

```bash
keytool -genkey -v -keystore laitno-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias laitno -storepass YOUR_KEYSTORE_PASS -keypass YOUR_KEY_PASS \
  -dname "CN=LAITNO,O=Your Organization,L=Your City,ST=Your State,C=IR"
```

**یادت بمونه:** passwords را تغییر بده.

## گام ۲: Keystore رو Base64 کن

اگه keystore موجود داره:
```bash
cat laitno-release.jks | base64 -w0 > keystore.b64
```

محتوای `keystore.b64` رو کپی کن.

## گام ۳: GitHub Secrets رو تنظیم کن

1. برو Repository → **Settings** → **Secrets and variables** → **Actions**.
2. برای هر secret، بزن **New repository secret** و این‌ها رو اضافه کن:

| نام Secret | مقدار |
|---|---|
| `KEYSTORE_B64` | محتوای `keystore.b64` (طولانی ترینه) |
| `KEYSTORE_PASS` | رمز keystore (عموماً `laitno_release_key` اگه keystore دادهٔ شده رو استفاده کردی) |
| `KEY_ALIAS` | نام کلید (معمولاً `laitno`) |
| `KEY_PASS` | رمز کلید (معمولاً همون `KEYSTORE_PASS`) |

## گام ۴: Build Release APK

### روش ۱: Release Workflow
```bash
git tag v13.9.9
git push origin v13.9.9
```

GitHub خودش `Build LAITNO Release APK` را اجرا می‌کند. نتیجه در **Releases** قابل دانلود است.

### روش ۲: دستی Workflow
برو **Actions** → **Build LAITNO Release APK** → **Run workflow** → **Run workflow**.

### روش ۳: تغییر برنامه
اگه می‌خواهی ہمیشه release (نه debug) بساز، `build-apk.yml` رو ویرایش کن:
```yaml
./gradlew :app:assembleRelease --no-daemon
```
بجای:
```yaml
./gradlew :app:assembleDebug --no-daemon
```

## تفاوت Debug و Release

| Debug | Release |
|---|---|
| سریع‌تر ساخت شود | کوچک‌تر در حجم |
| `app-debug.apk` | `app-release.apk` |
| برای تست | برای انتشار در Play Store |
| امضا خودکار توسط SDK | امضا شدهٔ شما |

## اگه Keystore رو گم کردی

اگه `laitno-release.jks` رو پاک کردی یا دسترسیش رو از دست دادی، باید یک keystore جدید بسازی و **تمام secrets** رو آپدیت کنی. اگه نه، GitHub build ناموفق می‌شه.

## نکات امنیتی

- **Keystore را هیچ‌جا آپلود نکن** (جز GitHub Secrets).
- **Passwords را نزن** در کد یا commit messages.
- اگه keystore سری شو، یک جدید بساز و Play Store رو آپدیت کن.
