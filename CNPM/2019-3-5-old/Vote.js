var fs = require("fs");

fs.readFile('123.csv', function (err, data) {
    var table = new Array();
    if (err) {
        console.log(err.stack);
        return;
    }

    ConvertToTable(data, function (table) {
        console.log(table);
    })
});
console.log("程序执行完毕");

function ConvertToTable(data, callBack) {
    data = data.toString();
    var table = new Array();
    var rows = new Array();
    rows = data.split("\r\n");
    for (var i = 0; i < rows.length; i++) {
        table.push(rows[i].split(","));
    }
    callBack(table);
}