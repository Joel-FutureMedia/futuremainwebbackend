# Testing the WayaSpot SMS System in Postman

This walks through testing every SMS system endpoint in Postman, in the order you'd actually use
them, with sample data you can copy-paste. It assumes the project (with the SMS system merged in)
is running locally on `http://localhost:8585` with Kafka and MinIO up.

---

## 0. Postman setup

Create a Postman **Environment** called `WayaSpot Local` with these variables (leave the values
blank for now, we'll fill them in as we go):

| Variable | Initial value |
|---|---|
| `baseUrl` | `http://localhost:8585` |
| `clientToken` | *(empty)* |
| `adminToken` | *(empty)* |
| `senderId` | *(empty)* |
| `tierId` | *(empty)* |
| `orderId` | *(empty)* |
| `companyAppId` | *(empty)* |
| `batchId` | *(empty)* |

For every request below that returns a token or an id you'll need later, open the **Tests** tab of
that request in Postman and add a one-liner to save it automatically, e.g.:

```javascript
pm.environment.set("clientToken", pm.response.json().token);
```

(the exact line to use is given under each request that produces something worth saving).

For every authenticated request, add header:

```
Authorization: Bearer {{clientToken}}
```

(or `{{adminToken}}` for admin endpoints).

---

## 1. Log in

### 1a. Admin login
A super admin is already seeded on startup.

```
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "admin@wayaspot.com",
  "password": "admin123"
}
```
**Tests tab:** `pm.environment.set("adminToken", pm.response.json().token);`

### 1b. Register a test client
```
POST {{baseUrl}}/api/auth/register/user
Content-Type: application/x-www-form-urlencoded

email=testclient@example.com
password=Passw0rd!
```
This sends a verification email over Kafka. For local testing, either click the verification link
from the email (Kafka → EmailConsumer → your configured SMTP), or manually set
`emailVerified = true` for that user in the database if you don't have SMTP wired up locally.

### 1c. Client login
```
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "email": "testclient@example.com",
  "password": "Passw0rd!"
}
```
**Tests tab:** `pm.environment.set("clientToken", pm.response.json().token);`

From here on, every client request uses `Authorization: Bearer {{clientToken}}` and every admin
request uses `Authorization: Bearer {{adminToken}}`.

---

## 2. Complete the client profile

```
POST {{baseUrl}}/api/clients/profile
Authorization: Bearer {{clientToken}}
Content-Type: application/json

{
  "FullName": "Tom",
  "LastName": "Nangolo",
  "Phone": "+264812223333",
  "Country": "Namibia",
  "City": "Windhoek",
  "Address": "12 Independence Ave",
  "CompanyName": "Nangolo Traders",
  "CompanyEmail": "billing@nangolotraders.com",
  "CompanyPhone": "+264812223333",
  "CompanyAddress": "12 Independence Ave, Windhoek"
}
```

---

## 3. Choose an account type (needed before the account can become PAID)

```
POST {{baseUrl}}/api/clients/account-type?AccountType=STANDARD
Authorization: Bearer {{clientToken}}
```
Valid values: `STARTER`, `STANDARD`, `BUSINESS`.

This automatically:
- starts the client's 30-day billing cycle (`accountCycleStartDate` = today, `nextBillingDate` =
  today + 30)
- generates and emails (via Kafka) an invoice for that account type

---

## 4. Make the account PAID (required for every sms-sending endpoint)

### 4a. Upload proof of payment for the account
```
POST {{baseUrl}}/api/clients/pop/upload
Authorization: Bearer {{clientToken}}
Content-Type: multipart/form-data

file: (attach any PDF/PNG, e.g. "proof.pdf")
note: "Paid via bank transfer"
```

### 4b. Admin approves it
```
GET {{baseUrl}}/api/clients/proofofpyments/pending
Authorization: Bearer {{adminToken}}
```
Copy the `id` of the proof of payment you just uploaded, then:
```
POST {{baseUrl}}/api/clients/proofofpyments/{id}/approve
Authorization: Bearer {{adminToken}}
```
The client's account status is now `PAID`. Confirm with:
```
GET {{baseUrl}}/api/clients/get-profile
Authorization: Bearer {{clientToken}}
```

---

## 5. Sender ID applications

### 5a. Submit a Company Sender ID application
```
POST {{baseUrl}}/api/clients/sms/senderid/company
Authorization: Bearer {{clientToken}}
Content-Type: multipart/form-data

registeredCompanyName: Nangolo Traders CC
physicalAddress: 12 Independence Ave, Windhoek
country: Namibia
postalAddress: PO Box 4021, Windhoek
fullLegalName: Tomas Nangolo
email: billing@nangolotraders.com
mobileNumber: +264812223333
officeTelephone: +264612223333
passportOrIdNumber: 88010112345
taxIdNumber: TIN-99887766
preferredSenderId: NANGOLO
certificateOfIncorporation: (attach a PDF file)
taxRegistrationCertificate: (attach a PDF file)
```
**Tests tab:** `pm.environment.set("companyAppId", pm.response.json().id);`

This immediately generates and emails an invoice (Kafka) for the application fee.

### 5b. Upload proof of payment for the application
```
POST {{baseUrl}}/api/clients/sms/senderid/company/{{companyAppId}}/proof-of-payment
Authorization: Bearer {{clientToken}}
Content-Type: multipart/form-data

file: (attach a PDF/PNG)
note: "Paid via EFT"
```

### 5c. Admin reviews and approves — enters the actual Sender ID to grant
```
GET {{baseUrl}}/api/admin/sms/senderid/company/pending
Authorization: Bearer {{adminToken}}
```
```
POST {{baseUrl}}/api/admin/sms/senderid/company/{{companyAppId}}/approve?approvedSenderId=NANGOLO
Authorization: Bearer {{adminToken}}
```
(Or to reject instead: `POST .../company/{{companyAppId}}/reject?reason=Documents unclear, please resubmit`.)

Approving:
- creates a usable `SenderId` row (`NANGOLO`) owned by this client only
- emails the client the decision via Kafka

### 5d. Individual application (same idea, different fields)
```
POST {{baseUrl}}/api/clients/sms/senderid/individual
Authorization: Bearer {{clientToken}}
Content-Type: multipart/form-data

fullLegalName: Maria Shivute
idOrPassportNumber: 91020567890
physicalAddress: 45 Sam Nujoma Drive, Windhoek
country: Namibia
email: maria.shivute@example.com
phoneNumber: +264851234567
preferredSenderId: MARIA
nationalIdOrPassport: (attach a PDF/image file)
taxRegistrationCertificate: (attach a PDF file)
```
Then approve the same way: `POST /api/admin/sms/senderid/individual/{id}/approve?approvedSenderId=MARIA`.

### 5e. Existing Sender ID application (client already owns one elsewhere)
```
POST {{baseUrl}}/api/clients/sms/senderid/existing
Authorization: Bearer {{clientToken}}
Content-Type: application/json

{
  "companyName": "Nangolo Traders CC",
  "physicalAddress": "12 Independence Ave, Windhoek",
  "companyEmail": "billing@nangolotraders.com",
  "companyPhoneNumber": "+264812223333",
  "senderIdNameOrNumber": "NGLTRADE"
}
```
Then: `POST /api/admin/sms/senderid/existing/{id}/approve?approvedSenderId=NGLTRADE`.

### 5f. See the sender ids this client can now pick from
```
GET {{baseUrl}}/api/clients/sms/senderid/mine
Authorization: Bearer {{clientToken}}
```
**Tests tab:** `pm.environment.set("senderId", pm.response.json()[0].value);`

---

## 6. Buying SMS (pricing tiers → order → balance)

### 6a. Admin creates a pricing tier ("tie")
```
POST {{baseUrl}}/api/admin/sms/tiers?label=Starter Pack&smsVolume=100&price=50.00
Authorization: Bearer {{adminToken}}
```
This is your "100 SMS = N$50" example. Create a few more if you like, e.g.
`smsVolume=500&price=200.00`.

**Tests tab:** `pm.environment.set("tierId", pm.response.json().id);`

### 6b. Client lists available tiers
```
GET {{baseUrl}}/api/clients/sms/tiers
Authorization: Bearer {{clientToken}}
```

### 6c. Client orders a tier
```
POST {{baseUrl}}/api/clients/sms/orders?tierId={{tierId}}
Authorization: Bearer {{clientToken}}
```
**Tests tab:** `pm.environment.set("orderId", pm.response.json().id);`

This immediately generates and emails an invoice for the order.

### 6d. Upload proof of payment for the order
```
POST {{baseUrl}}/api/clients/sms/orders/{{orderId}}/proof-of-payment
Authorization: Bearer {{clientToken}}
Content-Type: multipart/form-data

file: (attach a PDF/PNG)
note: "Paid via EFT"
```

### 6e. Admin approves — enters how many sms to actually allocate
```
GET {{baseUrl}}/api/admin/sms/orders/pending
Authorization: Bearer {{adminToken}}
```
```
POST {{baseUrl}}/api/admin/sms/orders/{{orderId}}/approve?approvedSmsAmount=100
Authorization: Bearer {{adminToken}}
```
(Or reject: `POST .../orders/{{orderId}}/reject?reason=Payment amount does not match invoice`.)

This adds `100` to the client's `messagesBalance` and emails them the decision.

### 6f. Confirm the balance
```
GET {{baseUrl}}/api/clients/get-profile
Authorization: Bearer {{clientToken}}
```
Check `messagesBalance` in the response — it should now be `100`.

---

## 7. Add a contact (needed for the "send to contacts" endpoints)

```
POST {{baseUrl}}/api/crm/contacts
Authorization: Bearer {{clientToken}}
Content-Type: application/json

{
  "fullName": "John Amutenya",
  "phone": "+264817654321",
  "email": "john.amutenya@example.com"
}
```
Add a second one too, e.g. phone `+264855551234`, so you have something to test "send to all
contacts" and "send to selected contacts" with. Note the `id` values returned — you'll need them
for section 8d.

---

## 8. Sending SMS

All of these require: a `PAID` account (section 4), a sender id you own (section 5), and enough
`messagesBalance` (section 6).

### 8a. Send to one number
```
POST {{baseUrl}}/api/clients/sms/send/number?senderId={{senderId}}&phoneNumber=+264817654321&message=Hello from WayaSpot!
Authorization: Bearer {{clientToken}}
```
**Tests tab:** `pm.environment.set("batchId", pm.response.json().batchId);`

Example response:
```json
{
  "batchId": "b1e2c3d4-...",
  "senderId": "NANGOLO",
  "totalRequested": 1,
  "accepted": 1,
  "rejectedOutOfBalance": 0,
  "rejectedNumbers": [],
  "note": "All recipients queued for sending."
}
```

### 8b. Send to multiple numbers (comma separated)
```
POST {{baseUrl}}/api/clients/sms/send/numbers?senderId={{senderId}}&phoneNumbers=+264817654321,+264855551234&message=Reminder: your invoice is due soon.
Authorization: Bearer {{clientToken}}
```
(You can instead send `phoneNumberList` as a Postman array param if you prefer a real list over a
comma-separated string — both are accepted.)

### 8c. Send to every contact belonging to this client
```
POST {{baseUrl}}/api/clients/sms/send/contacts/all?senderId={{senderId}}&message=Big sale this weekend!
Authorization: Bearer {{clientToken}}
```

### 8d. Send to a hand-picked subset of contacts
```
POST {{baseUrl}}/api/clients/sms/send/contacts/selected?senderId={{senderId}}&contactIds=1&contactIds=2&message=You're invited to our launch event.
Authorization: Bearer {{clientToken}}
```
(Replace `1` and `2` with the actual contact ids from section 7. In Postman, add `contactIds` as a
param twice — once per id — under the **Params** tab.)

### 8e. Test the "out of sms" partial-send behaviour
Send to more numbers than your remaining balance, e.g. if your balance is down to `2`, send to 5
numbers at once via 8b. The response will show `accepted: 2`, `rejectedOutOfBalance: 3`, and list
the skipped numbers in `rejectedNumbers` — none of them get sent, and your balance drops to `0`.

### 8f. Track delivery
```
GET {{baseUrl}}/api/clients/sms/send/logs
Authorization: Bearer {{clientToken}}
```
or just one batch:
```
GET {{baseUrl}}/api/clients/sms/send/logs/{{batchId}}
Authorization: Bearer {{clientToken}}
```
Each entry shows `deliveryStatus` (`QUEUED` → `SUCCESS`/`FAILED`, or `OUT_OF_BALANCE`),
`providerMessageId`, `providerStatusCode` and `cost` once Africa's Talking has responded (this
happens asynchronously via Kafka, so re-run the GET a second or two later if it still shows
`QUEUED`).

> Using the Africa's Talking **sandbox**, you must add your test phone numbers as simulator
> recipients in the Africa's Talking dashboard first, otherwise the sandbox will reject them.

---

## 9. Negative/edge cases worth testing in Postman

| Scenario | How to trigger it | Expected result |
|---|---|---|
| Sending with an unpaid account | Reject the proof of payment in step 4b instead of approving, then try 8a | `400` — "Your Account is not paid, Please Upload Proof of payment" |
| Sending with a sender id you don't own | Use a random string for `senderId` in 8a | `400` — "That Sender ID does not belong to you or is not active" |
| Sending with zero balance | Spend the whole balance first, then try 8a again | `accepted: 0`, `rejectedOutOfBalance: 1`, note says no more messages left |
| Invalid phone number | Send to `12345` in 8a | Number is silently skipped (not counted against balance); if it's the only number, you'll get a 400 "No valid phone numbers to send to" |
| A client applying with someone else's contact id | Use a contact id that belongs to a different client account in 8d | `400` — "Contact X does not belong to you" |

---

## 10. Renewal reminder invoice (30-day cycle, 6-day-out reminder)

This one runs on a daily scheduler (`RenewalInvoiceScheduler`), not an endpoint, so it can't be
triggered directly from Postman. To verify it locally:
1. After step 3, check the client's `nextBillingDate` via `GET /api/clients/get-profile`.
2. In your database, temporarily set that client's `next_billing_date` to `CURRENT_DATE + 6`.
3. Either wait for the scheduled cron time (`0 0 6 * * *` = 06:00 daily) or temporarily change
   `sms.renewal-reminder.cron` in `application.properties` to something like `0 */2 * * * *` (every
   2 minutes) while testing, then restart the app.
4. Watch the logs / check the client's inbox for a new invoice email, and confirm
   `next_billing_date` moved forward by 30 days and `last_renewal_invoice_sent_on` was set to today.

Remember to put the cron value back to `0 0 6 * * *` before deploying.

---

## Quick reference: all SMS system endpoints

**Client**
- `POST /api/clients/sms/senderid/company` (multipart)
- `POST /api/clients/sms/senderid/individual` (multipart)
- `POST /api/clients/sms/senderid/existing` (json)
- `POST /api/clients/sms/senderid/company/{id}/proof-of-payment` (multipart)
- `POST /api/clients/sms/senderid/individual/{id}/proof-of-payment` (multipart)
- `POST /api/clients/sms/senderid/existing/{id}/proof-of-payment` (multipart)
- `GET  /api/clients/sms/senderid/company/mine`
- `GET  /api/clients/sms/senderid/individual/mine`
- `GET  /api/clients/sms/senderid/existing/mine`
- `GET  /api/clients/sms/senderid/mine`
- `GET  /api/clients/sms/tiers`
- `POST /api/clients/sms/orders?tierId=`
- `POST /api/clients/sms/orders/{id}/proof-of-payment` (multipart)
- `GET  /api/clients/sms/orders/mine`
- `POST /api/clients/sms/send/number?senderId=&phoneNumber=&message=`
- `POST /api/clients/sms/send/numbers?senderId=&phoneNumbers=&message=`
- `POST /api/clients/sms/send/contacts/all?senderId=&message=`
- `POST /api/clients/sms/send/contacts/selected?senderId=&contactIds=&message=`
- `GET  /api/clients/sms/send/logs`
- `GET  /api/clients/sms/send/logs/{batchId}`

**Admin** (require `Authorization: Bearer {{adminToken}}`)
- `GET/POST /api/admin/sms/senderid/company/pending|all|{id}/approve|{id}/reject`
- `GET/POST /api/admin/sms/senderid/individual/pending|all|{id}/approve|{id}/reject`
- `GET/POST /api/admin/sms/senderid/existing/pending|all|{id}/approve|{id}/reject`
- `GET/POST/PUT/DELETE /api/admin/sms/tiers`
- `GET/POST /api/admin/sms/orders/pending|all|{id}/approve|{id}/reject`
