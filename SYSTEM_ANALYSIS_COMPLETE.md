# 📊 تحليل شامل للنظام الكامل
## United Companies - Driver Portal System

---

## 🏗️ البنية العامة للنظام

النظام عبارة عن **منصة إدارة النقل والشحنات** تتكون من 4 أقسام رئيسية:

```
┌─────────────────────────────────────────────────────────┐
│       نظام بوابة السائقين - Driver Portal              │
├──────────┬──────────────┬──────────────┬──────────────┤
│ Android  │  iOS         │  Desktop     │  Web/Cloud   │
│ Mobile   │  Mobile      │  Apps        │  Apps Script │
└──────────┴──────────────┴──────────────┴──────────────┘
```

---

## 📱 1. تطبيقات الهاتف الذكي

### أ) تطبيق Android الرئيسي
**الاسم**: `DriverPortal` / `com.driver.portal.wasel`  
**الإصدار**: 1.0 (رقم الإصدار: 9)  
**نوع**: تطبيق Android Native  
**اللغة**: Kotlin + Jetpack Compose  
**الملف النهائي**: `DriverPortal2-GasTransport.apk`

**المسار**: `app/src/main/`

**الميزات الرئيسية**:
- ✅ واجهة مستخدم حديثة (Material Design 3)
- ✅ نظام إدارة الرحلات (Trips)
- ✅ تتبع المسافة والوقود (GPS & Fuel)
- ✅ إدارة المشاكل والصيانة (Issues & Maintenance)
- ✅ محفظة رقمية (Wallet)
- ✅ نظام الحالة (Status)
- ✅ إدارة التنبيهات (Alerts)

**متطلبات الجهاز**: 
- الحد الأدنى: Android 7.0 (API 24)
- الهدف: Android 14 (API 34)

---

### ب) تطبيق iOS
**الاسم**: Driver Portal iOS  
**نوع**: تطبيق iOS Native  
**اللغة**: Swift/Objective-C  
**المسار**: `ios/DriverPortalIOS/`

**الملفات**:
- `project.yml` - إعدادات المشروع
- `ExportOptions-AdHoc.plist` - إعدادات التوزيع (خارجي)
- `ExportOptions-AppStore.plist` - إعدادات متجر التطبيقات

**الميزات**: نفس ميزات Android

**الحالة**: 🔄 قيد التطوير

---

## 🖥️ 2. تطبيقات سطح المكتب

### أ) لوحة المدير العام (Admin Desktop)
**الاسم**: `driverportal-desktop-admin`  
**الإصدار**: 3.7.6  
**المنصة**: Windows Desktop (Electron)  
**الاسم العربي**: لوحة المدير العام  
**المسار**: `desktop-admin/`

**المكونات الرئيسية**:
- 📊 لوحة معلومات المدير (Admin Dashboard)
- 👥 إدارة السائقين (Driver Management)
- 📋 إدارة الشحنات (Shipment Management)
- 💰 تقارير الأداء المالية (Financial Reports)
- 🗺️ خريطة التتبع المباشر (Live Tracking Map)
- 📈 لوحات المعلومات المتقدمة (Advanced Dashboards)

**الملفات الرئيسية**:
```
desktop-admin/
├── main.js                    # نقطة دخول Electron
├── preload.js                 # معالج الأمان
├── login-page.html            # صفحة تسجيل الدخول
├── dashboard.html             # واجهة لوحة المعلومات
├── dashboard.js               # منطق لوحة المعلومات
├── advanced-dashboard.js      # لوحات إحصائية متقدمة
├── app.css                    # أنماط التطبيق (محدّث)
├── dashboard.css              # أنماط لوحة المعلومات (محدّث)
├── manager-config.json        # إعدادات المدير
├── package.json               # معلومات npm
└── preload.js                 # معالج الأمان
```

**المتطلبات**:
- Node.js 20 LTS أو أحدث
- Windows 7 أو أحدث
- الاتصال بالإنترنت

---

### ب) بوابة الشركات (Company Desktop)
**الاسم**: `driverportal-desktop-company`  
**الإصدار**: 1.0.5  
**المنصة**: Windows Desktop (Electron)  
**الاسم العربي**: بوابة الشركات  
**المسار**: `desktop-company/`

**المكونات الرئيسية**:
- 🏢 لوحة معلومات الشركة (Company Dashboard)
- 📊 تقارير الأداء (Performance Reports)
- 👥 إدارة الموظفين (Employee Management)
- 🚗 إدارة الأسطول (Fleet Management)
- 💼 إدارة الحسابات (Account Management)
- 📱 واجهة متجاوبة

**الملفات الرئيسية**:
```
desktop-company/
├── main.js                    # نقطة دخول Electron
├── preload.js                 # معالج الأمان
├── login-page.html            # صفحة تسجيل الدخول
├── company-dashboard.html     # لوحة معلومات الشركة
├── company-app.js             # منطق الشركة
├── dashboard.html             # واجهة عامة
├── advanced-dashboard.js      # لوحات إحصائية
├── app.css                    # أنماط التطبيق (محدّث)
├── dashboard.css              # أنماط لوحة المعلومات (محدّث)
├── company.css                # أنماط خاصة بالشركة
├── manager-config.json        # إعدادات الشركة
├── package.json               # معلومات npm
└── preload.js                 # معالج الأمان
```

**الميزات الخاصة**:
- ✅ واجهة مخصصة للشركات
- ✅ إدارة متقدمة للحسابات
- ✅ تقارير تفصيلية
- ✅ لوحات إحصائية

---

## ☁️ 3. تطبيقات الويب والسحابة

### أ) نظام Google Apps Script الرئيسي
**الاسم**: `Main Apps Script`  
**المسار**: `apps-script/`  
**المنطقة الزمنية**: America/New_York

**الملفات**:
```
apps-script/
├── Main.gs              # الملف الرئيسي (JavaScript)
├── Admin.html           # واجهة الإدارة
├── appsscript.json      # إعدادات Google Apps Script
└── README.md            # التوثيق
```

**الميزات**:
- 📊 معالجة البيانات الديناميكية
- 🔐 نظام المصادقة
- 📧 خدمات البريد الإلكتروني
- 📈 تقارير تلقائية
- 🗂️ إدارة ملفات Google Drive

---

### ب) نظام تطبيق GPS
**الاسم**: `GPS Apps Script`  
**المسار**: `gps-apps-script/`  
**المنطقة الزمنية**: Asia/Baghdad

**الملفات**:
```
gps-apps-script/
├── Main.gs              # منطق نظام GPS
├── appsscript.json      # الإعدادات
└── README-ar.md         # توثيق عربي
```

**الميزات**:
- 🗺️ معالجة بيانات GPS
- 📍 تتبع الموقع الفوري
- 🛣️ حساب المسافات
- ⏱️ حساب أوقات الرحلات
- 📊 تقارير الحركة

---

### ج) نظام الملف الحي (Live Core)
**الاسم**: `live-core`  
**المسار**: `live-core/`  
**الملفات**:
- `appsscript.json` - الإعدادات

**الغرض**: نظام المزامنة الفورية والبيانات المباشرة

---

## 🔄 4. أنظمة مساعدة ونسخ احتياطية

### أ) النسخ النشطة
```
temp-active-sync/          # نظام المزامنة الحالي
temp-prod-deploy-clean/    # نسخة الإنتاج النظيفة
temp-prod-sync/            # نسخة الإنتاج المزامنة
temp-prod-v555/            # إصدار إنتاج سابق
```

### ب) النسخ الاحتياطية
```
backups/
├── apps-script_20260604_000641/
├── apps-script_post_ops_20260604_002312/
├── apps-script_pre_dashboard_fix_20260604_005245/
├── google_sheet_safe_step_20260702/
├── prod-deploy-clean_20260604_000656/
└── safe_cleanup_20260619_035551/
```

---

## 📚 5. الملفات والمستندات الأساسية

### أ) ملفات البناء والإعدادات
```
build.gradle.kts              # إعدادات البناء الرئيسية
settings.gradle.kts           # إعدادات المشروع
gradle.properties             # خصائص Gradle
app/build.gradle.kts          # إعدادات تطبيق Android
local.properties              # الخصائص المحلية
keystore.properties           # معلومات التوقيع الرقمي
company-release-key.jks       # مفتاح التوقيع للشركة
```

### ب) ملفات التوثيق
```
docs/
├── clean-rebuild-master-plan-ar.md           # خطة إعادة البناء
├── company-version-control-ar.md             # التحكم في الإصدارات
├── gas-system-schema.md                      # مخطط النظام
├── migration-sheet-account-2026-07-19.md     # هجرة الحسابات
├── release-artifacts-2026-07-07.md           # تقارير الإصدار
├── system-operations-runbook-ar.md           # دليل التشغيل
└── DESIGN_UPDATE_v2.0.md                     # تحديث التصميم الجديد
```

### ج) ملفات التكوين الخاصة
```
gradle/libs.versions.toml      # إصدارات المكتبات
.github/                        # إعدادات GitHub (CI/CD)
.vscode/                        # إعدادات VS Code
.idea/                          # إعدادات IntelliJ IDEA
```

---

## 🔐 6. الملفات الحساسة والأمان

```
company-release-key.jks        # مفتاح التوقيع الرقمي 🔑
keystore.properties            # كلمات المرور والمفاتيح 🔒
local.properties               # الإعدادات المحلية 🔒
.git/ (مهم)                    # إدارة الإصدارات
```

---

## 📦 7. التطبيقات والملفات القابلة للتوزيع

```
DriverPortal2-GasTransport.apk    # تطبيق Android النهائي (v1.0)
release/                          # مجلد الإصدارات
build/                            # مجلدات البناء المؤقتة
```

---

## 🛠️ 8. المتطلبات والتقنيات

### للهواتف الذكية:
- **Android Studio** 2024 أو أحدث
- **Kotlin** 1.9+
- **Jetpack Compose** (واجهة مستخدم حديثة)
- **Gradle** 8.0+
- **Xcode** 15+ (لـ iOS)

### لسطح المكتب:
- **Node.js** 20 LTS
- **Electron** (إطار العمل)
- **npm** أو yarn

### للسحابة:
- **Google Apps Script** (V8 Runtime)
- **Google Drive**
- **Google Sheets**
- **Gmail API**

### التخزين والنسخ الاحتياطية:
- **Git** (إدارة الإصدارات)
- **GitHub** (المستودع)
- **Firebase** (محتمل - للمزامنة الفورية)

---

## 📊 9. جدول ملخص المنتجات والتطبيقات

| # | الاسم | النوع | الإصدار | المنصة | الحالة |
|----|-------|------|---------|---------|--------|
| 1 | DriverPortal Android | Mobile | 1.0 | Android 7-14 | ✅ جاهز |
| 2 | DriverPortal iOS | Mobile | 1.0 | iOS 12+ | 🔄 قيد التطوير |
| 3 | Admin Desktop | Desktop | 3.7.6 | Windows | ✅ جاهز |
| 4 | Company Portal Desktop | Desktop | 1.0.5 | Windows | ✅ جاهز |
| 5 | Main Apps Script | Web Backend | V8 | Cloud | ✅ جاهز |
| 6 | GPS Apps Script | Web Backend | V8 | Cloud | ✅ جاهز |
| 7 | Live Core | Web Backend | V8 | Cloud | 🔄 قيد التطوير |

---

## 🎯 10. الوظائف الرئيسية للنظام الكامل

### إدارة الرحلات 🚗
- تسجيل الرحلات الجديدة
- تتبع الرحلات في الوقت الفعلي
- تحديث حالة الرحلات
- تقارير الرحلات

### إدارة الشحنات 📦
- إنشاء وإدارة الشحنات
- تتبع الشحنات
- تأكيد التسليم
- إدارة المستودع

### إدارة المسافة والوقود ⛽
- تسجيل استهلاك الوقود
- حساب المسافات
- تقارير الاستهلاك
- تحسين الأداء

### إدارة الموظفين 👥
- ملفات السائقين
- ملفات الموظفين الإداريين
- إدارة الصلاحيات
- سجل الأداء

### إدارة الأسطول 🚙
- تسجيل المركبات
- صيانة المركبات
- متابعة الفحوصات
- تقارير الحالة

### النظام المالي 💰
- إدارة الحسابات
- تقارير الإيرادات
- تقارير المصروفات
- محافظ رقمية

### التنبيهات والإشعارات 📢
- تنبيهات الطوارئ
- تنبيهات الصيانة
- تنبيهات الرحلات
- رسائل النظام

---

## 🔐 11. أنظمة الأمان والمصادقة

- 🔒 كلمات مرور موثوقة (SHA-256)
- 🔐 مفاتيح توقيع رقمية
- 👤 نظام المستخدمين والأدوار
- 📋 قائمة التحكم بالوصول (ACL)
- 🔄 نظام المزامنة الآمنة

---

## 📈 12. البيانات الإحصائية

**حجم المشروع**:
- ~1000+ ملف
- ~50+ مجلد رئيسي
- ~3+ لغات برمجية (Kotlin, JavaScript, Swift)
- ~4 منصات مختلفة

**حجم التطبيقات**:
- Android APK: ~30-50 MB
- Desktop Admin: ~200-300 MB
- Desktop Company: ~200-300 MB

---

## 🚀 13. خطوات التطوير والنشر

### للهواتف الذكية:
```
Source Code → Build → Sign → APK/IPA → Stores
```

### لسطح المكتب:
```
Source Code → npm install → Build → NSIS Installer → Distribution
```

### للسحابة:
```
Source Code → Apps Script → Deployment → URL
```

---

## 📝 الخلاصة

**النظام عبارة عن منصة متكاملة تتضمن**:
- ✅ **3 تطبيقات هاتفية** (Android + iOS + Web)
- ✅ **2 تطبيق سطح مكتب** (Admin + Company)
- ✅ **3 أنظمة سحابية** (Main + GPS + Live)
- ✅ **نظام أمان متقدم** (توثيق + تشفير)
- ✅ **نظام نسخ احتياطية** (6+ نسخ)
- ✅ **نظام تتبع كامل** (Git + GitHub)

**الحالة الحالية**:
- 🟢 **Android**: جاهز للإنتاج
- 🟡 **iOS**: قيد التطوير
- 🟢 **Desktop Admin**: جاهز للإنتاج
- 🟢 **Desktop Company**: جاهز للإنتاج
- 🟢 **Backend**: جاهز للإنتاج

---

**آخر تحديث**: 2026-07-19  
**الإصدار**: 3.7.6 (Admin Desktop)
