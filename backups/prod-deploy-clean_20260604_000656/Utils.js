// ========================================
// اسم المحرك: Utils
// المسؤولية: دوال مساعدة
// ========================================


// ========================================
// json
// تحويل البيانات إلى JSON
// ========================================
function json(data){
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}


// ========================================
// getOrCreateSheet
// إنشاء شيت إذا غير موجود
// ========================================
function getOrCreateSheet(ss, name) {

  var sheet = ss.getSheetByName(name);

  if (!sheet) {
    sheet = ss.insertSheet(name);

    // إذا شيت المعمل
    if (name.startsWith("F_")) {
      sheet.appendRow([
        "رقم الوصل",
        "اسم السائق",
        "رقم السيارة",
        "الكمية",
        "اسم المعمل",
        "رابط الصورة",
        "تاريخ التفريغ"
      ]);
    } 
    // شيت التحميل
    else {
      sheet.appendRow([
        "رقم الوصل",
        "اسم السائق",
        "رقم السيارة",
        "تاريخ التحميل",
        "تاريخ التفريغ",
        "الكمية",
        "المالك",
        "المحطة",
        "رابط الصورة",
        "وقت الإرسال",
        "لترات الكاز",
        "رقم البوجر",
        "المسافة",
        "سعر النقل",
        "الإجمالي",
        "ملاحظات"
      ]);
    }
  }

  return sheet;
}

// ========================================
// processFile
// رفع صورة إلى Google Drive
// ========================================
function processFile(data, name){
  try{
    var folderName = "صور_النظام";
    var folders = DriveApp.getFoldersByName(folderName);
    var folder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);

    var base64Data = data.split(',')[1];
    var contentType = data.substring(data.indexOf(":")+1, data.indexOf(";"));

    var blob = Utilities.newBlob(
      Utilities.base64Decode(base64Data),
      contentType,
      name
    );

    var file = folder.createFile(blob);
    return file.getUrl();

  } catch(e){
    Logger.log("Image upload error: " + e);
    return "";
  }
}



