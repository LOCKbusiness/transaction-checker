"use strict";
exports.__esModule = true;
var bitcoinjs_message_1 = require("bitcoinjs-message");
var jellyfish_network_1 = require("@defichain/jellyfish-network");
var fs = require('fs');
// ...
try {
    var filePath = void 0;
    if (process.argv.length > 2) {
        filePath = process.argv[2];
    }
    if (filePath) {
        //let fileName = filePath + "M:/Development/workspace/lockbusiness/transaction-checker/transactionchecker/data/js/message-verification.json";
        var fileName = filePath + "/message-verification.json";
        //console.log("FILE NAME: " + fileName);
        var jsonString = fs.readFileSync(fileName);
        var objArray = JSON.parse(jsonString);
        //console.log("SIZE: " + objArray.length);
        for (var _i = 0, objArray_1 = objArray; _i < objArray_1.length; _i++) {
            var obj = objArray_1[_i];
            //console.log("MESSAGE: " + obj.message);
            obj.isValid = messageVerification(obj.message, obj.address, obj.signature);
        }
        var jsonOutputString = JSON.stringify(objArray, null, 2);
        fs.writeFileSync(fileName, jsonOutputString);
        process.exit(0);
    }
}
catch (e) {
}
process.exit(-1);
// ...
function messageVerification(message, address, signature) {
    var flags = [31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42];
    for (var _i = 0, flags_1 = flags; _i < flags_1.length; _i++) {
        var flag = flags_1[_i];
        var flagByte = Buffer.alloc(1);
        flagByte.writeInt8(flag);
        var sigBuffer = Buffer.from(signature, 'base64').slice(1);
        sigBuffer = Buffer.concat([flagByte, sigBuffer]);
        var candidateSig = sigBuffer.toString('base64');
        try {
            var isValid = (0, bitcoinjs_message_1.verify)(message, address, candidateSig, jellyfish_network_1.MainNet.messagePrefix);
            if (isValid)
                return true;
        }
        catch (e) { }
    }
    return false;
}
