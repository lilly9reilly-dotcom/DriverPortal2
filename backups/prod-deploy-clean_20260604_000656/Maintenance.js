// ========================================
// Maintenance Engine
// ========================================

// ========================================
// saveMaintenance
// ========================================
function saveMaintenance(data){

  if(!data){
    return {success:false, message:"No data"};
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheetName = "سجل الصيانة";
  var sheet = ss.getSheetByName(sheetName);

  if(!sheet){
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow([
      "رقم الطلب",
      "اسم السائق",
      "السيارة",
      "العطل",
      "الحالة",
      "تاريخ الطلب",
      "تاريخ الإصلاح",
      "نوع الصيانة",
      "التكلفة",
      "الكراج",
      "ملاحظات"
    ]);
  }

  var driver = String(data.driver || data.driverName || "").trim();
  var vehicle = String(data.vehicle || data.carNumber || "").trim();
  var problem = String(data.problem || "").trim();
  var price = Number(data.price || 0);

  var time = Utilities.formatDate(new Date(),"Asia/Baghdad","yyyy-MM-dd HH:mm:ss");
  var requestId = Utilities.getUuid();

  sheet.appendRow([
    requestId,
    driver,
    vehicle,
    problem,
    "pending",
    time,
    "",
    "",
    price,
    "",
    ""
  ]);

  return {success:true};
}


// ========================================
// getMaintenanceRequests
// ========================================
function getMaintenanceRequests(params){

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("سجل الصيانة");

  if(!sheet){
    return {success:true,requests:[]};
  }

  var carNumber = "";
  if(params && params.carNumber){
    carNumber = String(params.carNumber).trim();
  }

  var dataSheet = sheet.getDataRange().getValues();
  var requests = [];

  for(var i=1;i<dataSheet.length;i++){

    var vehicle = String(dataSheet[i][2] || "").trim();

    if(carNumber != "" && vehicle != carNumber){
      continue;
    }

    requests.push({
      requestId: dataSheet[i][0],
      driver: dataSheet[i][1],
      vehicle: vehicle,
      problem: dataSheet[i][3],
      status: dataSheet[i][4],
      requestDate: dataSheet[i][5],
      repairDate: dataSheet[i][6] || "",
      type: dataSheet[i][7] || "",
      cost: String(dataSheet[i][8] || ""),
      location: dataSheet[i][9] || "",
      notes: dataSheet[i][10] || ""
    });

  }

  return {
    success:true,
    requests:requests
  };
}