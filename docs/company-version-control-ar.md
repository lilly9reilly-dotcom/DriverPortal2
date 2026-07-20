# التحكم بتطبيق مدير الحسابات للشركات

## الهدف

إيقاف النسخ القديمة من تطبيق الشركات، وتشغيل النسخ المعتمدة فقط من خلال سياسة تشغيل من Google Apps Script.

## ما تم تجهيزه في التطبيق

تمت إضافة فحص عند فتح تطبيق الشركة:

- يرسل التطبيق `versionCode`, `versionName`, و `packageName` إلى السيرفر.
- إذا رد السيرفر `allowed=false` تظهر شاشة إيقاف ولا يدخل المستخدم إلى لوحة الشركات.
- إذا لم يتوفر إنترنت أو تعذر التحقق، لا يفتح التطبيق لوحة الشركات.

النسخة الجديدة الحالية:

- `versionCode = 4`
- `versionName = 2.0.2`
- flavor الشركة يظهر كـ `2.0.2-company`

## ما تم تجهيزه في Apps Script

تمت إضافة action:

```text
companyVersionPolicy
```

السياسة الحالية داخل `apps-script/Main.gs`:

```js
enabled: true,
minVersionCode: 4,
latestVersionCode: 4,
latestVersionName: "2.0.2-company"
```

معناها:

- أي نسخة أقل من `4` يتم إيقافها وتطلب تحديث.
- النسخة `4` تعمل.

## إيقاف كل تطبيق الشركات

في `getCompanyVersionPolicy` غيّر:

```js
enabled: true
```

إلى:

```js
enabled: false
```

ثم انشر Apps Script.

## تشغيل نسخة جديدة فقط

عند إصدار نسخة جديدة:

1. ارفع `versionCode` في `app/build.gradle.kts`.
2. ارفع `minVersionCode` في `apps-script/Main.gs` إلى نفس الرقم.
3. انشر Apps Script.
4. صدّر APK/AAB جديد للشركات.

## ملاحظة مهمة عن النسخ القديمة الموجودة عند الشركات

النسخ القديمة التي لا تحتوي فحص `companyVersionPolicy` لا يمكن إيقاف واجهتها من داخل التطبيق بعد أن تم توزيعها.

لكن يمكن تقليل فائدتها عبر تغيير/حماية endpoints التي تعتمد عليها، وهذا يحتاج حذرًا لأن نفس endpoints قد تستخدمها تطبيقات السائق أو الأدمن.

أفضل مسار آمن:

1. نشر `companyVersionPolicy` الآن.
2. توزيع نسخة الشركة الجديدة فقط.
3. بعد التأكد من انتقال الشركات للنسخة الجديدة، نضيف حماية أعمق للعمليات الحساسة حسب نوع التطبيق أو مفتاح شركة.

## التحقق المحلي

- `:app:compileCompanyDebugKotlin` نجح.
- فحص syntax لـ `apps-script/Main.gs` نجح.
