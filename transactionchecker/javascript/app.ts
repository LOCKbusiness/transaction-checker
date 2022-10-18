import { verify, sign } from 'bitcoinjs-message';
import { MainNet, TestNet } from '@defichain/jellyfish-network';

const fs = require('fs');

// ...
try {
  let filePath;

  if (process.argv.length > 2) {
    filePath = process.argv[2];
  }

  if (filePath) {
    //let fileName = filePath + "M:/Development/workspace/lockbusiness/transaction-checker/transactionchecker/data/js/message-verification.json";
    let fileName = filePath + "/message-verification.json";
    //console.log("FILE NAME: " + fileName);

    let jsonString = fs.readFileSync(fileName);
    let objArray = JSON.parse(jsonString);
    //console.log("SIZE: " + objArray.length);

    for (const obj of objArray) {
      //console.log("MESSAGE: " + obj.message);
      obj.isValid = messageVerification(obj.message, obj.address, obj.signature);
    }

    let jsonOutputString = JSON.stringify(objArray, null, 2);
    fs.writeFileSync(fileName, jsonOutputString);
	
	process.exit(0);
  }
} catch (e) {
}

process.exit(-1);

// ...
function messageVerification(message, address, signature): boolean {
  const flags = [31,32,33,34,35,36,37,38,39,40,41,42];

  for (const flag of flags) {
    const flagByte = Buffer.alloc(1);
    flagByte.writeInt8(flag);

    let sigBuffer = Buffer.from(signature, 'base64').slice(1);
    sigBuffer = Buffer.concat([flagByte, sigBuffer]);
  
    const candidateSig = sigBuffer.toString('base64');

    try {
      let isValid = verify(message, address, candidateSig, MainNet.messagePrefix);
      if (isValid) return true;
    } catch (e) {}
  }
  
  return false;
}
