# DriverPortal iPhone

هذه نسخة iPhone أولية مبنية بـ SwiftUI ومهيأة للعمل مع نفس الخادم المستخدم في نسخة الأندرويد.

## الميزات المجهزة
- تسجيل دخول السائق
- شاشة وصل لإرسال البيانات إلى الخادم
- اللوحة الرئيسية
- سجل الوصلات
- مركز التواصل والدعم
- الحساب الشخصي
- حفظ بيانات السائق محلياً

## التشغيل
1. انقل مجلد `ios` إلى جهاز Mac.
2. ثبّت `XcodeGen` إذا لم يكن موجوداً:
   `brew install xcodegen`
3. من داخل مجلد `ios` نفّذ:
   `xcodegen generate`
4. افتح ملف `DriverPortalIOS.xcodeproj` الناتج في Xcode.
5. عدّل `PRODUCT_BUNDLE_IDENTIFIER` و`DEVELOPMENT_TEAM` حسب حساب Apple الخاص بك.
6. شغّل التطبيق على iPhone أو Simulator.

## ملفات iPhone الأساسية
- `DriverPortalIOS/Info.plist` لإعدادات التطبيق وصلاحية الوصول للصور
- `DriverPortalIOS/Configurations/DriverPortalIOS.xcconfig` لإعدادات البناء الخاصة بالآيفون
- `project.yml` لتوليد مشروع Xcode تلقائياً بدل إنشائه يدوياً
- `DriverPortalIOS/Assets.xcassets` لملفات الألوان وأيقونات التطبيق
- `build-ipa.sh` لبناء الأرشيف وتصدير ملف IPA
- `ExportOptions-AdHoc.plist` لتصدير IPA للتثبيت المباشر
- `ExportOptions-AppStore.plist` لتصدير نسخة الرفع إلى App Store Connect

## إخراج IPA
1. افتح `DriverPortalIOS/Configurations/DriverPortalIOS.xcconfig` وعدّل `PRODUCT_BUNDLE_IDENTIFIER`.
2. افتح `ExportOptions-AdHoc.plist` أو `ExportOptions-AppStore.plist` وضع `TEAM_ID` الصحيح بدل `YOUR_TEAM_ID`.
3. على جهاز Mac داخل مجلد `ios` نفّذ:
   `chmod +x build-ipa.sh`
4. لتصدير نسخة Ad Hoc:
   `./build-ipa.sh ExportOptions-AdHoc.plist`
5. لتصدير نسخة App Store:
   `./build-ipa.sh ExportOptions-AppStore.plist`
6. ستجد ملف `IPA` داخل:
   `ios/build/ipa`

## تصدير IPA من غير جهاز Mac (GitHub Actions)
أضفنا Workflow جاهز يعمل على ماكينة macOS سحابية ويُخرج لك ملف IPA حقيقي:

1. ارفع المشروع إلى مستودع GitHub.
2. ادخل إلى تبويب **Actions** واختر **Build iOS IPA**.
3. اضغط **Run workflow** ثم اختر:
   - `unsigned` لبناء IPA غير موقّع (مناسب لإعادة التوقيع لاحقاً عبر Sideloadly/AltStore).
   - `signed-adhoc` أو `signed-appstore` لبناء IPA موقّع رسمياً (يحتاج إعداد الأسرار التالية في إعدادات GitHub → Secrets):
     - `APPLE_TEAM_ID`
     - `IOS_P12_BASE64` (شهادة التوزيع p12 بصيغة base64)
     - `IOS_P12_PASSWORD`
     - `IOS_PROVISIONING_PROFILE_BASE64`
4. بعد انتهاء التشغيل حمّل ملف **DriverPortalIOS-IPA** من قسم Artifacts.

## بناء IPA غير موقّع محلياً على Mac
على جهاز Mac داخل مجلد `ios` نفّذ:
```
chmod +x build-ipa-unsigned.sh
./build-ipa-unsigned.sh
```
سيتم إخراج الملف في:
`ios/build/ipa/DriverPortalIOS-unsigned.ipa`

## ملاحظة مهمة
- IPA غير الموقّع لا يمكن تثبيته مباشرة على iPhone، لكنه قابل لإعادة التوقيع بأي حساب آبل (حتى الحساب المجاني) عبر AltStore أو Sideloadly.
- IPA الموقّع رسمياً (Ad Hoc/App Store) يحتاج عضوية Apple Developer وحساب فعّال.
