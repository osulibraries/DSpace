// # ReportGenerator Js Mod
// ## Addes jQuery datepicker functionality to the ReportGenerator

jQuery(function ($) {
    //initializer
    $(".date-picker").datepicker({
      dateFormat: "mm/dd/yy"
      //, minDate: new Date(2008, 01, 01)
  });

  //set the minDate
  var minDate = new Date($('input[name=minDate]').val());
  if(typeof minDate !== minDate) {
      //bump 2007-12-31ZTimeZone to 2008-01-01-UTC
      var minDate_utc = new Date(minDate.getUTCFullYear(), minDate.getUTCMonth(), minDate.getUTCDate(),  minDate.getUTCHours(), minDate.getUTCMinutes(), minDate.getUTCSeconds());
      if(typeof minDate_utc !== 'undefined') {
          $(".date-picker").datepicker("option", "minDate", minDate_utc);
      }
  }
});
