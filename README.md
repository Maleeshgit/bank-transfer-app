# Bank Transfer System (Assignment 01)

Spring Boot + Thymeleaf + Spring Data JPA + MySQL + Lombok.

## Run on Windows

1. Start XAMPP and click **Start** beside **MySQL**. The default port is `3306`.
2. In PowerShell, open this folder and run:

   ```powershell
   mvn spring-boot:run
   ```

3. Open <http://localhost:8080>. The first start inserts Kamal, Nimali and Saman.
4. If MySQL has a password, copy `secrets.properties.example` to
   `src/main/resources/secrets.properties`, fill in the password, and uncomment
   `spring.config.import=optional:file:./secrets.properties` in
   `application.properties`.

The database `bank_db` is created automatically. The application demonstrates:

* `@Transactional`: a transfer commits all changes or rolls them all back.
* `@Query`: JPQL, native SQL, aggregate and update queries.
* `REQUIRES_NEW`: failed transfers are audited after the transfer rollback.

## Verify the build

```powershell
mvn -DskipTests package
mvn test
```

`mvn test` requires MySQL to be running because the context test connects to
`bank_db`.

## Assignment test sequence

1. Transfer `5000` from `100001` to `100002`; balances should become `45000` and
   `30000`.
2. Transfer `20000` from `100003`; it must fail and leave balances unchanged.
3. Select the same sender and receiver; it must fail.
4. Transfer `1000` with **Simulate a system failure** checked; the withdrawal must
   be rolled back, while a `FAILED` audit row remains.
5. Test empty and zero amounts.
6. Add account `100004`, then try the same account number again.
7. Search a customer and open account history.

Useful MySQL checks:

```sql
USE bank_db;
SELECT * FROM customers;
SELECT * FROM bank_transactions ORDER BY id DESC;
SELECT SUM(balance) FROM customers;
```

## Common errors

* **Communications link failure**: start MySQL and verify port `3306`.
* **Access denied for root**: configure the password in `secrets.properties`.
* **Rollback does not happen**: keep `org.springframework.transaction.annotation.Transactional`,
  do not catch the exception inside `BankService.transfer`, and use InnoDB tables.
* **Named parameter not bound**: make each `:parameter` name match its `@Param`.
* **Port 8080 already in use**: stop the other process or add
  `server.port=8081` to `application.properties`.
