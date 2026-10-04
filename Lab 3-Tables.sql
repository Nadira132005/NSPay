
--Table ACCOUNTS 
--Will contain the following columns:
---cardNumber CHAR(16),     
---cardHolderName  VARCHARCHAR (50)
---amount DECIMAL(12,2)                /* upto 9,999,999,999.99 *
---dailyTxLimit     INT                /* maximum number of transactions per day. Set to -1 to disable the limit*/
---dailyTxSumLimit  DECIMAL(12,2),     /* maximum amount spent per day. Set to -1 to disable the limit */
---dailyTx          INT                /* actual number of transactions for the current day */
---dailyTxSum DECIMAL(12,2)               /* actual amount spent for the current day (sum of all of today’s transactions) */

--SQL statement to create the table
CREATE TABLE ACCOUNTS(id BIGINT, cardNumber CHAR(16), cardHolderName VARCHAR(50), amount DECIMAL(12,2), dailyTxLimit INT, dailyTxSumLimit DECIMAL(12,2), dailyTx INT, dailyTxSum DECIMAL(12,2));

--SQL statement to add values in the table         

INSERT INTO ACCOUNTS VALUES ( 1, "1234000000056789", "John Black", 100.00, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 2, "1111222233334444", "Lara Dean", 200.00, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 3, "4321000000056789", "Alice Bond", 1200.00, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 4, "9000111100001111", "Ana Pop", 9000.00, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 5, "4444000000000001", "Dan Popescu", 3300.00, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 6, "9000000000000000", "Maria Ionescu", 6200.10, 1000000, 1000000000.0, 0, 0.00);
INSERT INTO ACCOUNTS VALUES ( 7, "1000000000000009", "Ion Miu", 4360.00, 1000000, 1000000000.0, 0, 0.00);
