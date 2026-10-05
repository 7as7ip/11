# LAITNO برای اندروید (APK)

این پوشه یک پروژهٔ کامل اندروید است. فایل HTML شما داخل `app/src/main/assets/www/index.html` قرار گرفته و یک لایهٔ اندرویدی (`android-shim.js`) به اولش اضافه شده.

## چه چیزهایی نسبت به نسخهٔ مرورگر عوض شد
| مشکل در مرورگر | در برنامهٔ اندروید |
|---|---|
| بعد از بستن مرورگر دسترسی پوشه می‌پرید | پوشه‌ها ثابت‌اند: **`/LAITNO/Images`** (عکس‌ها) و **`/LAITNO/Audio`** (تلفظ/موزیک). دسترسی یک بار در اولین اجرا گرفته می‌شود و همیشه یادش می‌ماند |
| `showDirectoryPicker` روی اندروید کار نمی‌کرد | روی حافظهٔ واقعی گوشی پیاده‌سازی شد (خواندن/نوشتن/حذف/اسکن) |
| تلفظ (speechSynthesis) در WebView نیست | به موتور TTS خود اندروید وصل شد |
| تمرین تلفظ با میکروفون | به تشخیص گفتار اندروید وصل شد |
| اعلان یادآوری فقط وقتی صفحه باز بود | اعلان واقعی + یادآوری روزانه حتی وقتی برنامه بسته است |
| دانلود پشتیبان/ZIP/تقویم | ذخیره در **`/LAITNO/Backups`** و **`/LAITNO/Exports`** |
| پشتیبان خودکار به فایل | کار می‌کند (فایل در `/LAITNO/Backups`) |
| اشتراک، کپی، روشن ماندن صفحه هنگام دانلود | به اندروید وصل شد |

تنها تغییر داخل کد خود برنامه: در اسکن پوشهٔ صوت، اندازهٔ فایل از فهرست اندروید خوانده می‌شود تا اسکن هزاران فایل سریع باشد.

## دسترسی‌هایی که در اولین اجرا پرسیده می‌شود
1. میکروفون (تمرین تلفظ)
2. اعلان (اندروید ۱۳ به بالا)
3. حافظه: اندروید ۱۰ و پایین‌تر پنجرهٔ معمولی؛ اندروید ۱۱ به بالا صفحهٔ تنظیمات «دسترسی به همهٔ فایل‌ها» باز می‌شود، کلید LAITNO را روشن کن و برگرد.

اگر دسترسی حافظه را ندهی، برنامه باز هم کار می‌کند ولی فایل‌ها در پوشهٔ خصوصی برنامه (`Android/data/ir.laitno.app`) ذخیره می‌شوند و با حذف برنامه پاک می‌شوند. هر وقت بعداً اجازه بدهی، خودکار به `/LAITNO` منتقل می‌شوند.

## روش ساخت APK

### روش ۱: بدون نصب هیچ برنامه‌ای (GitHub، حتی با گوشی)
1. در github.com یک حساب بساز و یک Repository جدید (مثلاً `laitno-android`) بساز.
2. همهٔ محتوای این پوشه را آپلود کن (Add file → Upload files). پوشهٔ `.github` هم باید آپلود شود.
3. برو به تب **Actions**؛ ساخت خودکار شروع می‌شود (حدود ۵ دقیقه). اگر شروع نشد: `Build LAITNO Release APK` → `Run workflow`.
4. بعد از تیک سبز، روی همان اجرا بزن و از بخش **Artifacts** فایل `LAITNO-apk` را دانلود کن. داخل zip فایل `app-release.apk` است.

### روش ۲: Android Studio روی کامپیوتر
1. Android Studio (نسخهٔ Koala یا جدیدتر) را نصب کن.
2. `File → Open` و همین پوشه را انتخاب کن. صبر کن Gradle Sync تمام شود (بار اول اینترنت لازم است).
3. `Build → Build App Bundle(s) / APK(s) → Build APK(s)`.
4. فایل در `app/build/outputs/apk/debug/app-release.apk` ساخته می‌شود.
5. برای نسخهٔ امضاشده: `Build → Generate Signed App Bundle / APK → APK`.

با خط فرمان (اگر Android SDK و Gradle 8.7 داری): `gradle :app:assembleDebug`

## نصب روی گوشی
فایل APK را به گوشی بفرست، بازش کن و اجازهٔ «نصب از منابع ناشناس» را بده.

## انتقال اطلاعات قبلی
- **پیشرفت و تنظیمات:** در نسخهٔ مرورگر از بخش تنظیمات «خروجی/پشتیبان» بگیر و در برنامه «ورود پشتیبان» بزن.
- **عکس‌ها و تلفظ‌هایی که قبلاً دانلود کردی:** پوشه‌هایشان را با فایل‌منیجر در `LAITNO/Images` و `LAITNO/Audio` کپی کن؛ برنامه خودش پیدا و مرتبشان می‌کند.

## نکته
- دسترسی «همهٔ فایل‌ها» در Google Play محدودیت دارد؛ برای نصب مستقیم APK هیچ مشکلی ندارد.
- برای تلفظ آفلاین، در تنظیمات گوشی موتور Google TTS و بستهٔ زبان انگلیسی را نصب کن.
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
# اطلاعات Keystore آماده

**keystore فایل:** `app/laitno-release.jks`

**مشخصات:**
- نام کاربری: LAITNO
- نام سازمان: Personal
- شهر: Iran
- استان: Iran  
- کشور: IR

**Passwords (برای تنظیم Secrets):**
```
KEYSTORE_PASS: laitno_release_key
KEY_ALIAS: laitno
KEY_PASS: laitno_release_key
```

**اگه می‌خواهی تغییر بدی:**
1. یک keystore جدید بساز (بالا ببین).
2. محتوای جدید رو Base64 کن.
3. GitHub Secrets رو آپدیت کن.

**مدت اعتبار:** ۲۷.۴ سال (تا ۱۳۹۲)
