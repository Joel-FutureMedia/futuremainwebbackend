# MASTER PROMPT FOR CURSOR AI

You are a Senior Java Spring Boot Architect with over 15 years of experience.

Your task is to build a COMPLETE, PRODUCTION-READY REST API backend for a Future Media Admin Panel.

The project must be clean, scalable, secure, and follow enterprise standards.

---

# TECHNOLOGY STACK

Use ONLY the following technologies:

* Java 21
* Spring Boot 3+
* Spring Security
* Spring Data JPA
* PostgreSQL
* JWT Authentication
* Maven
* Lombok
* application.properties (NOT application.yml)
* Multipart File Upload
* Java Mail Sender
* HTML Email Templates

---

# PROJECT ARCHITECTURE

STRICTLY use ONLY these packages:

config/
controllers/
models/
repositories/
services/

DO NOT create DTO packages.

DO NOT create utility packages.

DO NOT create helper packages.

DO NOT create mapper packages.

Everything must stay inside these five packages.

---

# MODEL STYLE

Every entity MUST contain:

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
```

Example

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name="users")
public class User {

}
```

Use Lombok.

Use JPA annotations.

Use UUID or Long IDs consistently.

---

# DEPENDENCY INJECTION

Use

```java
@Autowired
```

throughout the project.

Do not use constructor injection.

---

# DATABASE

Use PostgreSQL.

Generate all tables automatically.

Use Spring Data JPA repositories.

---

# AUTHENTICATION

Implement Spring Security with JWT.

Users login using

Email

Password

Passwords must be encrypted using BCrypt.

After login return

JWT Token

User details

Role

---

# USER ROLES

There are only two roles.

SUPER_ADMIN

USER

---

# DEFAULT SUPER ADMIN

Whenever the application starts it must automatically check whether the Super Admin exists.

If not, automatically create it.

Email

[joel@simplyfound.com.na](mailto:joel@simplyfound.com.na)

Password

admin123

Password must be encrypted.

The Super Admin should always exist.

Never delete this account automatically.

---

# USER MANAGEMENT

Super Admin can

Create Super Admin

Create User

Update User

Delete User

Update Super Admin

Delete Super Admin

View all users

Search users

Enable user

Disable user

Normal users cannot manage users.

---

# FILE STORAGE

Create upload folders automatically.

Support

Images

Videos

Documents

Validate uploads.

Allowed Images

jpg

jpeg

png

webp

Allowed Videos

mp4

mov

avi

Allowed Documents

pdf

doc

docx

Reject everything else.

Return meaningful validation messages.

---

# FEATURE 1

LATEST NEWS

CRUD

Fields

id

title

description

image/video

createdBy

approvedBy

approvalStatus

createdDate

updatedDate

Workflow

Normal User creates News

Status becomes

PENDING

Super Admin reviews it.

Super Admin can

Approve

Reject

Only APPROVED news should be available on the public website endpoint.

Users may edit or delete their own news before approval.

Super Admin can edit/delete everything.

Every News supports

Image OR Video only.

Never allow both.

---

# FEATURE 2

LATEST EVENTS

CRUD

Fields

Title

Description

Image or Video

Date Created

Created By

Users can

Create

Update

Delete

View

Only images/videos allowed.

---

# FEATURE 3

GALLERY

Users upload

Single Image

or

Multiple Images

Gallery only supports Images.

No videos.

Gallery API should support

Upload one image

Upload many images

Delete image

Update image

Get all images

---

# FEATURE 4

VACANCY PORTAL

ADMIN SIDE

Admins create vacancies.

Fields

Title

Description

Closing Date

Image OR PDF

Created By

Update

Delete

Publish

PUBLIC SIDE

Applicants fill

Full Name

Email

Phone

Location

Upload CV

Only allow

PDF

DOC

DOCX

Reject Images.

Admin Panel

View Applicants

Download CV

Delete Applicant

Search Applicants

---

# FEATURE 5

CONTACT US

Public endpoint.

Fields

Full Name

Email

Phone

Subject

Message

Date

When submitted

Save into database.

Also email the submission to

[joelkalimbwe3@gmail.com](mailto:joelkalimbwe3@gmail.com)

Create beautiful HTML email.

Use logo

https://futuremedia.simplyfound.com.na/assets/logo.png

Design professionally.

Include

Logo

Header

Table of Details

Footer

Responsive HTML

Create EmailService.java.

---

# FEATURE 6

EMAIL SUBSCRIPTION

Public endpoint.

Subscribers provide

Full Name

Email

Language

Languages

English

Afrikaans

Save subscribers.

Create beautiful Welcome Email.

Use logo

https://futuremedia.simplyfound.com.na/assets/logo.png

Responsive HTML.

Future email marketing must use these subscribers.

Whenever an APPROVED NEWS article is published

Automatically send newsletter to every subscriber.

Email contains

News Image

Title

Description

Read More Button

Logo

Responsive HTML

Professional marketing design.

---

# FEATURE 7

CONTENT MANAGEMENT

Banner Management

CRUD

Fields

Title

Description

Banner Image

Only Image uploads.

Landing page always loads latest active banner.

---

# VALIDATIONS

Validate every request.

Examples

Email format

Required fields

Empty fields

Invalid uploads

Duplicate emails

Password length

Return proper HTTP status codes.

---

# ERROR HANDLING

Return JSON

Example

{
"success": false,
"message":"Email already exists"
}

Create proper exception handling.

---

# API RESPONSE FORMAT

Every endpoint should return

{
"success": true,
"message":"Operation successful",
"data": ...
}

---

# SECURITY

Protect all admin endpoints.

Public endpoints

View Approved News

View Events

View Gallery

Apply Vacancy

Contact Us

Subscribe

Everything else requires JWT.

---

# EMAIL SERVICE

Use Java Mail Sender.

SMTP

spring.mail.host=smtp.resend.com
spring.mail.port=587
spring.mail.username=resend
spring.mail.password=re_Ny6ejuvG_7KkPK6rdQ5zoPY9ZaJLSt3Xd
spring.mail.from=[info@ytnafrica.live](mailto:info@ytnafrica.live)
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
spring.mail.properties.mail.smtp.ssl.trust=smtp.resend.com
spring.mail.properties.mail.smtp.ssl.checkserveridentity=false
spring.mail.properties.mail.debug=false
spring.mail.test-connection=false

---

# application.properties

Generate the complete application.properties including

PostgreSQL

JPA

Multipart Upload

JWT Secret

SMTP

Server Port

File Upload Location

Logging

---

# CONTROLLERS

Create REST Controllers for

Authentication

Users

News

Events

Gallery

Vacancies

Applications

Contact Us

Subscriptions

Banner

Each controller should expose

POST

GET

PUT

DELETE

Where appropriate.

---

# SERVICES

Business logic must remain inside Services.

Controllers should be lightweight.

---

# REPOSITORIES

Create Spring Data JPA repositories for every entity.

---

# SECURITY CONFIGURATION

Implement

JWT Filter

Authentication Provider

Password Encoder

Security Filter Chain

Custom UserDetailsService

Public endpoint configuration

Role-based authorization

---

# FILE VALIDATION

Validate file MIME type.

Reject invalid uploads.

Generate unique filenames.

Store relative path in database.

---

# NEWS APPROVAL WORKFLOW

USER

↓

Creates News

↓

Status = PENDING

↓

SUPER_ADMIN Reviews

↓

Approve / Reject

↓

If Approved

↓

Automatically Publish

↓

Automatically Email Subscribers

---

# SWAGGER

Integrate Swagger/OpenAPI.

Document every endpoint.

---

# TESTING

Ensure every endpoint works in Postman.

Provide sample requests.

Provide sample responses.

---

# CODE QUALITY

Use clean code principles.

Meaningful class names.

Meaningful methods.

Proper comments.

No duplicate logic.

Enterprise-level architecture.

---

# FINAL DELIVERABLES

Generate the entire project including:

* Maven project structure
* pom.xml
* application.properties
* All Models
* All Repositories
* All Services
* All Controllers
* Security Configuration
* JWT Authentication
* Email Service
* HTML Email Templates
* PostgreSQL Integration
* File Upload Logic
* Validation
* Exception Handling
* Swagger Configuration
* SQL initialization for the default Super Admin
* Postman Collection
* API Documentation
* README.md with setup instructions

Build the project incrementally in a logical order (configuration first, then security, then entities, repositories, services, controllers, and finally testing). Ensure the final project compiles successfully with no missing classes, unresolved imports, or runtime errors.
