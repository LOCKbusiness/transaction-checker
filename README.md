# Transaction Checker

## Transaction Server

### 1. Start Transaction Server
Argument --mainnet:\
Run the Server against the mainnet. If Argument is missing, run Server against the testnet.\
\
Argument --compact:\
Compact the database files. This argument can only be used, if there is no active server running.\
\
Argument --initialsetup:\
Is used, if there is a initial setup with a empty database. The database structure (tables, indices etc.) must be available, but the tables should be empty.\
\
Argument --serveronly:\
Just run the server, but do nothing with the database.

### 2. Server Information
The server detects the latest block number in the database and in the chain. If there are missing blocks they are fetched from the chain and inserted in the database. But there are not only blocks stored, also transactions and addresses.

### 3. Automatic Sync
The server checks every 30 seconds the block numbers and updates the database if needed.
