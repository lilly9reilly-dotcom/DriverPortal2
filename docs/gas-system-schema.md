# نظام الكاز

هذا الملف يعرّف قاعدة البيانات والربط المقترح لتطبيق الكاز بشكل مستقل عن نظام الشركة الحالي.

## الهدف

- عزل إدارة تعبئة الكاز والتسديدات عن نظام الشركة الحالي.
- اعتبار نظام الكاز مصدر الحقيقة الخاص بالمحطة.
- ربط تقارير الشركة لاحقًا بقراءة فقط دون كتابة على الجداول الحالية.

## نمط التنفيذ الحالي (MVP مبسط)

- نعتمد ورقة واحدة فقط داخل Google Sheet لتقليل التعقيد.
- اسم الورقة المقترح: gas_mvp
- نفس الورقة تستقبل التعبئات والتسديدات والإعدادات كأنواع سجلات مختلفة.
- هذا النمط مؤقت كبداية سريعة، ويمكن لاحقًا ترحيله إلى عدة أوراق دون خسارة البيانات.

## جداول Google Sheets المقترحة

ملاحظة: القسم التالي هو التصميم الكامل للتوسع لاحقًا. في البداية سنعمل على ورقة واحدة فقط حسب نمط MVP أعلاه.

### gas_transactions

- tx_id
- tx_number
- created_at
- fill_date
- fuel_type
- station_name
- plate_number
- driver_name
- liters
- price_per_liter
- total_amount
- entered_by
- notes
- sync_status

### gas_vehicles

- vehicle_id
- plate_number
- driver_name_default
- company_source_id
- active
- created_at

### gas_settlements

- settlement_id
- created_at
- amount
- payment_method
- reference_number
- created_by
- notes

### gas_account_ledger

- entry_id
- entry_type
- reference_id
- created_at
- debit_amount
- credit_amount
- balance_after
- notes

### gas_users

- user_id
- username
- role
- active
- created_at

### gas_settings

- key
- value
- updated_at

القيم الإلزامية المقترحة في `gas_settings`:

- default_price_normal
- default_price_commercial

### gas_audit_log

- log_id
- entity_name
- entity_id
- action
- before_json
- after_json
- actor
- created_at

## النسخة الحالية داخل التطبيق

- flavor جديد باسم `gas`
- تخزين محلي مؤقت باستخدام SharedPreferences
- شاشات أولية: التهيئة، تسجيل الدخول، التعبئة، الحركات، التسديدات، التقارير، الإعدادات
- التعبئة تتضمن: تاريخ تعبئة + نوع كاز (اعتيادي/تجاري) + سعر اللتر

## مكان بيانات الكاز في Google Sheet

- ملف Google Sheet:
	https://docs.google.com/spreadsheets/d/1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM/edit
- تبويب التشغيل الحالي في نمط MVP (ورقة واحدة):
	https://docs.google.com/spreadsheets/d/1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM/edit?gid=864258794#gid=864258794
- اسم الورقة المعتمد في نمط MVP: gas_mvp
- نوع السجل داخل نفس الورقة يحدد الغرض: TRANSACTION أو SETTLEMENT أو SETTING

## Endpoint المعتمد حاليًا

- Apps Script exec:
	https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec

الأكشنات المقترحة لنمط الورقة الواحدة:

- gas_mvp_list
- gas_mvp_add_transaction
- gas_mvp_add_settlement
- gas_mvp_get_settings
- gas_mvp_update_settings

## الخطوة التالية

- إنشاء Apps Script مستقل لنظام الكاز فقط.
- ربط `gas_vehicles` بجدول `Drivers` الحالي للقراءة فقط.
- استبدال التخزين المحلي في flavor `gas` بمزامنة مع Google Sheets الجديدة.