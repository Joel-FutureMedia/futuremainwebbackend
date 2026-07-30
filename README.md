# Future Media Admin Panel — REST API

Production-ready Spring Boot 3 backend for the Future Media admin panel and public website.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- JavaMail (Resend SMTP)
- Swagger / OpenAPI
- Maven

## Architecture

```
com.futuremedia
├── config/        Security, JWT filter, Swagger, exception handling, data init
├── controllers/   REST endpoints
├── models/        JPA entities + enums + ApiResponse
├── repositories/  Spring Data JPA
└── services/      Business logic, file storage, email
```

## Prerequisites

1. Java 21+
2. Maven 3.9+
3. PostgreSQL running locally

Create the database:

```sql
CREATE DATABASE futuremedia;
```

Update credentials in `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/futuremedia
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Run

```bash
mvn clean spring-boot:run
```

Server: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Default Super Admin

Created automatically on startup if missing:

| Field    | Value                     |
|----------|---------------------------|
| Email    | joel@simplyfound.com.na   |
| Password | admin123                  |
| Role     | SUPER_ADMIN               |

## Authentication

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "joel@simplyfound.com.na",
  "password": "admin123"
}
```

Use the returned JWT:

```http
Authorization: Bearer <token>
```

## Public Endpoints (no JWT)

| Method | Endpoint                         | Description              |
|--------|----------------------------------|--------------------------|
| GET    | /api/news/public                 | Approved news            |
| GET    | /api/news/public/{id}            | Single approved news     |
| GET    | /api/events/public               | All events               |
| GET    | /api/gallery/public              | Gallery images           |
| GET    | /api/vacancies/public            | Published vacancies      |
| GET    | /api/banners/public/latest       | Latest active banner     |
| POST   | /api/applications/apply          | Apply for a job (+ CV)   |
| POST   | /api/contact                     | Contact us form          |
| POST   | /api/subscriptions               | Newsletter subscribe     |

## Admin Endpoints (JWT required)

### Users (SUPER_ADMIN only)

| Method | Endpoint                    |
|--------|-----------------------------|
| POST   | /api/users                  |
| GET    | /api/users                  |
| GET    | /api/users/search?keyword=  |
| GET    | /api/users/{id}             |
| PUT    | /api/users/{id}             |
| DELETE | /api/users/{id}             |
| PUT    | /api/users/{id}/enable      |
| PUT    | /api/users/{id}/disable     |

### News (approval workflow)

| Method | Endpoint                 | Notes                                      |
|--------|--------------------------|--------------------------------------------|
| POST   | /api/news                | multipart: title, description, media       |
| PUT    | /api/news/{id}           | Update                                     |
| DELETE | /api/news/{id}           | Delete                                     |
| GET    | /api/news                | All news (admin)                           |
| GET    | /api/news/pending        | Pending approval                           |
| GET    | /api/news/my             | Current user's news                        |
| PUT    | /api/news/{id}/approve   | SUPER_ADMIN — triggers newsletter email    |
| PUT    | /api/news/{id}/reject    | SUPER_ADMIN                                |

Normal users create news as `PENDING`. Super Admin posts are auto-approved and emailed to subscribers.

### Events

| Method | Endpoint        | Notes                                |
|--------|-----------------|--------------------------------------|
| POST   | /api/events     | multipart: title, description, media |
| PUT    | /api/events/{id}|                                      |
| DELETE | /api/events/{id}|                                      |
| GET    | /api/events     |                                      |

### Gallery

| Method | Endpoint              | Notes                         |
|--------|-----------------------|-------------------------------|
| POST   | /api/gallery/single   | one image                     |
| POST   | /api/gallery/multiple | multiple images (`images`)    |
| PUT    | /api/gallery/{id}     |                               |
| DELETE | /api/gallery/{id}     |                               |
| GET    | /api/gallery          |                               |

### Vacancies

| Method | Endpoint                      | Notes                                   |
|--------|-------------------------------|-----------------------------------------|
| POST   | /api/vacancies                | multipart: title, description, media (image/PDF) |
| PUT    | /api/vacancies/{id}           |                                         |
| DELETE | /api/vacancies/{id}           |                                         |
| PUT    | /api/vacancies/{id}/publish   |                                         |
| PUT    | /api/vacancies/{id}/unpublish |                                         |
| GET    | /api/vacancies                |                                         |

### Job Applications (admin)

| Method | Endpoint                              |
|--------|---------------------------------------|
| GET    | /api/applications                     |
| GET    | /api/applications/vacancy/{vacancyId} |
| GET    | /api/applications/search?keyword=     |
| GET    | /api/applications/{id}                |
| DELETE | /api/applications/{id}                |

### Contact Us (admin list)

| Method | Endpoint           |
|--------|--------------------|
| GET    | /api/contact       |
| GET    | /api/contact/{id}  |
| DELETE | /api/contact/{id}  |

Public submit emails `joelkalimbwe3@gmail.com` and stores the record.

### Subscriptions

| Method | Endpoint                          |
|--------|-----------------------------------|
| GET    | /api/subscriptions                |
| GET    | /api/subscriptions/active         |
| PUT    | /api/subscriptions/{id}/unsubscribe |
| DELETE | /api/subscriptions/{id}           |

### Banners

| Method | Endpoint           | Notes                              |
|--------|--------------------|------------------------------------|
| POST   | /api/banners       | multipart: title, description, image |
| PUT    | /api/banners/{id}  |                                    |
| DELETE | /api/banners/{id}  |                                    |
| GET    | /api/banners       |                                    |

## File Validation

| Feature        | Allowed                         |
|----------------|---------------------------------|
| News / Events  | jpg, jpeg, png, webp, mp4, mov, avi |
| Gallery / Banner | jpg, jpeg, png, webp only     |
| Vacancy media  | images or PDF                   |
| Job CV         | pdf, doc, docx only             |

Uploads are stored under `uploads/` and served at `/uploads/**`.

## Response Format

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

## Roles

- `SUPER_ADMIN` — full access including user management and news approval
- `USER` — content management; news requires approval

## Postman

Import `FutureMedia-API.postman_collection.json` for sample requests.
