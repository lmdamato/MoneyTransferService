# MoneyTransferService
Simple in-memory microservice to handle money deposits, withdrawals, and transfers between users.

## Usage

Just run the `main()` method in class `RestServer`, then hit one of the endpoints, e.g., using `curl` from a terminal.

The service defines the following endpoints:


* `PUT /create/{userId}`
  Create a new user with id {userId}. 
  This operation is idempotent, so trying to create a user who already exists will succeed, 
  but no user account will actually be created.
  
  Returns:
  * 201 Created, if a user with id {userId} did not exist before
  * 204 No Content, if a user with id {userId} already exists
  * 400 Bad Request, if the request is malformed
  
* `GET /balance/{userId}`
  Retrieve the balance for user {userId}.
  
  Returns:
  * 200 OK, with a body containing the user's current balance
  * 400 Bad Request, if the request is malformed
  * 404 Not Found, if a user with id {userId} could not be found
  
* `POST /deposit/{userId}/{amount}`
  Deposit {amount} to {userId}'s account.
  
  Returns:
  * 204 No Content, if the amount is successfully added to {userId}'s balance
  * 400 Bad Request, if the request is malformed, e.g., if {amount} is negative
  * 404 Not Found, if a user with id {userId} could not be found
  
* `POST /withdraw/{userId}/{amount}`
  Withdraw {amount} from {userId}'s account.
  
  Returns:
  * 204 No Content, if {amount} is successfully withdrawn from {userId}'s balance
  * 400 Bad Request, if the request is malformed, e.g., if {amount} is negative
  * 403 Forbidden, if the user does not have enough funds
  * 404 Not Found, if a user with id {userId} could not be found
  
* `POST /transfer/{from}/{to}/{amount}`
  Transfer {amount} from {from}'s account to {to}'s account.
  
  Returns:
  * 204 No Content, if {amount} is successfully transferred
  * 400 Bad Request, if the request is malformed, e.g., if {amount} is negative
  * 403 Forbidden, if {from} does not have enough funds
  * 404 Not Found, if one of the users could not be found


## Sample requests

```
% curl -i -X PUT http://localhost:8080/create/abc1
HTTP/1.1 204 No Content
Date: Sat, 13 Jul 2019 17:12:13 GMT

% curl -i -X POST http://localhost:8080/deposit/abc1/1.23
HTTP/1.1 204 No Content
Date: Sat, 13 Jul 2019 17:20:51 GMT

% curl -i -X GET http://localhost:8080/balance/abc1
HTTP/1.1 200 OK
Connection: keep-alive
Content-Type: application/json
Content-Length: 21
Date: Sat, 13 Jul 2019 17:21:43 GMT

{
  "amount" : 1.23
}

% curl -ik -X POST http://localhost:8080/withdraw/abc1/1.22
HTTP/1.1 204 No Content
Date: Sat, 13 Jul 2019 17:24:58 GMT

% curl -i -X GET http://localhost:8080/balance/abc1
HTTP/1.1 200 OK
Connection: keep-alive
Content-Type: application/json
Content-Length: 21
Date: Sat, 13 Jul 2019 17:25:18 GMT

{
  "amount" : 0.01
}

% curl -i -X PUT http://localhost:8080/create/abc2
HTTP/1.1 201 Created
Connection: keep-alive
Content-Length: 0
Date: Sat, 13 Jul 2019 17:26:05 GMT

% curl -i -X POST http://localhost:8080/transfer/abc1/abc2/0.01
HTTP/1.1 204 No Content
Date: Sat, 13 Jul 2019 17:27:13 GMT

% curl -i -X GET http://localhost:8080/balance/abc1
HTTP/1.1 200 OK
Connection: keep-alive
Content-Type: application/json
Content-Length: 21
Date: Sat, 13 Jul 2019 17:27:35 GMT

{
  "amount" : 0.00
}

% curl -i -X GET http://localhost:8080/balance/abc2
HTTP/1.1 200 OK
Connection: keep-alive
Content-Type: application/json
Content-Length: 21
Date: Sat, 13 Jul 2019 17:27:48 GMT

{
  "amount" : 0.01
}
```
