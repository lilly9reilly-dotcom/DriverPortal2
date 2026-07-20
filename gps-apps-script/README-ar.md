# GPS Apps Script منفصل

هذا المجلد يجهز عزل GPS عن ملف الحسابات، لكنه غير منشور وغير مربوط بالتطبيق حتى الآن.

## الملفات

- `Main.gs`: backend خاص بالـ GPS فقط.
- `appsscript.json`: إعدادات Apps Script.

## ما يدعمه السكربت

- `action=health`
- `action=gps`
- `action=drivers`
- `action=get_drivers`
- `action=route`
- `action=cleanup`

## خطوات الربط لاحقًا

1. إنشاء Google Sheet مستقل باسم مثل `DriverPortal GPS`.
2. نسخ بيانات `gps_tracking` و`gps_history` إليه أو ترك السكربت ينشئها من جديد.
3. وضع ID الملف الجديد مكان `REPLACE_WITH_GPS_SPREADSHEET_ID` في `Main.gs`.
4. نشر Apps Script كـ Web App.
5. اختبار `?action=health` ثم إرسال GPS تجريبي.
6. بعد نجاح الاختبار فقط، تغيير `GoogleSheetConfig.GPS_EXEC_ENDPOINT` في التطبيق إلى رابط Web App الجديد.

## ملاحظة أمان

لا تحذف أوراق GPS من ملف الحسابات قبل التأكد أن التطبيق يرسل ويقرأ من الملف الجديد بنجاح.
