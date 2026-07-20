function registerDriver(data){

  if(!data){
    return response(false, "No data");
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("Drivers");

  if(!sheet){
    sheet = ss.insertSheet("Drivers");
    sheet.appendRow(["الاسم","الهاتف","السيارة","تاريخ التسجيل"]);
  }

  var name = String(data.name || "").trim();
  var phone = String(data.phone || "").trim();
  var car = String(data.car || "").trim();

  if(name == "" || phone == "" || car == ""){
    return response(false, "Missing data");
  }

  var rows = sheet.getDataRange().getValues();

  for(var i = 1; i < rows.length; i++){
    var n = String(rows[i][0] || "").trim();
    var p = String(rows[i][1] || "").trim();
    var c = String(rows[i][2] || "").trim();

    if(n == name && p == phone){
      return response(true, "Driver exists", {
        driver: n,
        carNumber: c
      });
    }
  }

  // تسجيل جديد
  sheet.appendRow([
    name,
    phone,
    car,
    Utilities.formatDate(new Date(),"Asia/Baghdad","yyyy-MM-dd")
  ]);

  return response(true, "Driver registered", {
    driver: name,
    carNumber: car
  });
}