![image logo](https://github.com/SashaPrylutskyy/WishPlus/blob/master/src/main/resources/static/logo.png?raw=true)
# WishPlus RESTful API documentation

## 1. Overview

**Project Description: WishPlus — Anonymous Tracking of Meaningful Events and Wish Lists**

WishPlus is a RESTful API designed to facilitate anonymous observation (following/subscription) of significant or selected events of other users. This platform solves the problem of staying informed about important upcoming dates in the lives of individuals you care about, without requiring direct personal connection or constant manual checking.

The core functionality of WishPlus allows users to "subscribe" to specific important dates of other users. The key feature is anonymity – the user being observed does not necessarily know who is following their events.

A few days prior to a subscribed user's important event, WishPlus sends a notification to the follower. This notification prompts the follower to explore the wish list of the user they are following. The wish list is intended to be populated by the user themselves and can include a diverse range of desires, both material (such as a smartphone, car, or apartment) and non-material (like a trip to Mount Pip Ivan).

**Key Features and Problem-Solving:**

* **RESTful API Design:** WishPlus adheres to REST architectural principles, ensuring a scalable, stateless, and well-structured platform for seamless integration with various client applications.
* **Anonymous Event Tracking:** Users can follow significant events of others without the followee being directly aware of the subscription, respecting privacy while enabling thoughtful engagement.
* **Timely Notifications:** Followers receive timely reminders about upcoming important dates, facilitating proactive and considerate actions.
* **Wish List Integration:** The platform connects event awareness with the opportunity to fulfill wishes, providing concrete ideas for gifts or contributions.
* **Diverse Wish Types:** The wish list supports both tangible and intangible desires, allowing users to express a wide range of aspirations.

In essence, WishPlus bridges the gap between knowing about important life events and having actionable insights into potential ways to celebrate or support the individuals involved, all within a privacy-conscious and technologically efficient framework.

## 2. Base URL

All API endpoints are relative to the base URL: `/api`

Example: `http://yourdomain.com/api/users/me`

## 3. Authentication

API endpoints require authentication using JSON Web Tokens (JWT), besides
- `/api/users/register`
- `/api/users/login`
- `/about`

### 3.1. Obtaining a JWT

*   Send a `POST` request to the `/api/users/login` endpoint.
*   **Request Body:** A JSON object representing the user credentials.
    ```json
    {
      "username": "your_username",
      "password": "your_password"
    }
    ```
*   **Success Response:** A `200 OK` response with the JWT string in the response body.
    ```
    eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ5b3V... (JWT token)
    ```
*   **Error Response:** A `401 Unauthorized` response if credentials are invalid.
    ```json
    {
      "error": "Invalid credentials"
    }
    ```

### 3.2. Using the JWT

*   Once obtained, include the JWT in the `Authorization` header for all subsequent requests to protected endpoints.
*   The header format must be `Bearer <token>`.
    ```
    Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ5b3V...
    ```

### 3.3. Token Expiration & Errors

> [!WARNING]  
> Configure `application.properties` files by your own. Current credentials are used as an example.

*   Tokens have a predefined expiration time configured on the server (`jwt.expiration-millis`).
*   If an expired or invalid token is used, the API will respond with a `401 Unauthorized` status code. The `JwtAuthenticationEntryPoint` handles these errors.
    ```json
    // Example for expired token
    {
        "error": "token expired"
    }
    // Example for other authentication issues
    {
        "error": "Full authentication is required to access this resource"
    }
    ```

### 3.4. Public Endpoints

The following endpoints do **not** require authentication:

*   `POST /api/users/login`
*   `POST /api/users/register`

## 4. Error Handling

The API uses standard HTTP status codes to indicate the success or failure of a
request. Errors are generally returned in a JSON format:

```json
{
  "error": "Descriptive error message"
}
```

Common Status Codes:

*   `200 OK`: Request successful.
*   `201 Created`: Resource successfully created (e.g., user registration).
*   `400 Bad Request`: Invalid input, often due to validation errors (e.g., missing fields, invalid format). For validation errors (`MethodArgumentNotValidException`), the response body might contain specific field errors:
    ```json
    {
      "fieldName1": "Error message for fieldName1",
      "fieldName2": "Error message for fieldName2"
    }
    ```
*   `401 Unauthorized`: Authentication failed or is required (missing/invalid JWT).
*   `403 Forbidden`: Authenticated user does not have permission to perform the action (e.g., trying to modify another user's data).
*   `404 Not Found`: The requested resource does not exist (`NoResultException`).
*   `409 Conflict`: The request conflicts with the current state of the resource (e.g., trying to register a username/email that already exists - `DuplicateKeyException`).
*   `500 Internal Server Error`: An unexpected server error occurred.

The `GlobalExceptionHandler` class defines specific handlers for various exceptions like `NoResultException`, `AccessDeniedException`, `DuplicateKeyException`, `MethodArgumentNotValidException`, `NullPointerException`, `CancellationException`, and general `RuntimeException`.

## 5. Endpoints

---

### 5.1. User Endpoints (`/api/users`)

#### 5.1.1. Register User

*   **Method:** `POST`
*   **Path:** `/api/users/register`
*   **Authentication:** No
*   **Description:** Creates a new user account.
*   **Request Body:** `User` object
    ```json
    {
      "email": "user@example.com",
      "username": "newuser123",
      "password": "password123",
      "firstName": "John",
      "lastName": "Doe"
    }
    ```
*   **Success Response:** `201 Created` with the created `User` object (password excluded).
*   **Error Responses:** `400 Bad Request` (validation errors), `409 Conflict` (email/username already exists).

#### 5.1.2. Login User

*   **Method:** `POST`
*   **Path:** `/api/users/login`
*   **Authentication:** No
*   **Description:** Authenticates a user and returns a JWT.
*   **Request Body:** `User` object (username and password required).
    ```json
    {
      "username": "existinguser",
      "password": "password123"
    }
    ```
*   **Success Response:** `200 OK` with the JWT string in the body.
*   **Error Responses:** `401 Unauthorized` (invalid credentials), `404 Not Found` (user not found).

#### 5.1.3. Get Current User

*   **Method:** `GET`
*   **Path:** `/api/users/me`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves the details of the currently authenticated user.
*   **Success Response:** `200 OK` with the `User` object of the authenticated user.
*   **Error Responses:** `401 Unauthorized`.

#### 5.1.4. Get All Users

*   **Method:** `GET`
*   **Path:** `/api/users`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves a list of all registered users.
*   **Success Response:** `200 OK` with a list of `User` objects.
*   **Error Responses:** `401 Unauthorized`.

#### 5.1.5. Get User by ID

*   **Method:** `GET`
*   **Path:** `/api/users/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves the details of a specific user by their ID.
*   **Path Parameters:**
    *   `id` (Long): The ID of the user to retrieve.
*   **Success Response:** `200 OK` with the `User` object.
*   **Error Responses:** `401 Unauthorized`, `404 Not Found`.

#### 5.1.6. Search Users by Username Prefix

*   **Method:** `GET`
*   **Path:** `/api/users/search/{user_prefix}`
*   **Authentication:** Required (JWT)
*   **Description:** Finds users whose usernames start with the given prefix.
*   **Path Parameters:**
    *   `user_prefix` (String): The prefix to search for.
*   **Success Response:** `200 OK` with a list of matching `User` objects.
*   **Error Responses:** `401 Unauthorized`.

#### 5.1.7. Update Current User

*   **Method:** `PUT`
*   **Path:** `/api/users`
*   **Authentication:** Required (JWT)
*   **Description:** Updates the details of the currently authenticated user. Only fields provided in the request body will be updated.
*   **Request Body:** `User` object (include fields to update, e.g., email, firstName, lastName, profilePhoto, username).
    ```json
    {
      "firstName": "Johnny",
      "profilePhoto": "http://example.com/new_photo.jpg"
    }
    ```
*   **Success Response:** `200 OK` with the updated `User` object.
*   **Error Responses:** `400 Bad Request` (validation errors), `401 Unauthorized`, `409 Conflict` (if updated email/username conflicts).

#### 5.1.8. Delete Current User

*   **Method:** `DELETE`
*   **Path:** `/api/users`
*   **Authentication:** Required (JWT)
*   **Description:** Deletes the account of the currently authenticated user. Requires a specific confirmation message.
*   **Request Body:** JSON object with the confirmation message.
    ```json
    {
      "submitMessage": "Delete my account forever!"
    }
    ```
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "User is successfully deleted."
    ```
*   **Error Responses:** `400 Bad Request` (if `submitMessage` is incorrect - returns `CancellationException` message), `401 Unauthorized`.

---

### 5.2. Wishlist Endpoints (`/api/wishlist`)

#### 5.2.1. Create Wish

*   **Method:** `POST`
*   **Path:** `/api/wishlist`
*   **Authentication:** Required (JWT)
*   **Description:** Adds a new wish to the authenticated user's wishlist.
*   **Request Body:** `Wish` object (title is required, description and url are optional).
    ```json
    {
      "title": "New Book",
      "description": "Sci-fi novel",
      "url": "http://example.com/book"
    }
    ```
*   **Success Response:** `200 OK` with the created `Wish` object (including generated ID, user, timestamps).
*   **Error Responses:** `400 Bad Request` (validation errors), `401 Unauthorized`.

#### 5.2.2. Get Wish by ID

*   **Method:** `GET`
*   **Path:** `/api/wishlist/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves a specific wish by its ID.
*   **Path Parameters:**
    *   `id` (Long): The ID of the wish to retrieve.
*   **Success Response:** `200 OK` with the `Wish` object.
*   **Error Responses:** `401 Unauthorized`, `404 Not Found`.

#### 5.2.3. Get All Wishes by User ID

*   **Method:** `GET`
*   **Path:** `/api/wishlist/user/{user_id}`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves all wishes belonging to a specific user.
*   **Path Parameters:**
    *   `user_id` (Long): The ID of the user whose wishes to retrieve.
*   **Success Response:** `200 OK` with a list of `Wish` objects.
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if the user ID does not exist).

#### 5.2.4. Update Wish

*   **Method:** `PUT`
*   **Path:** `/api/wishlist/{wish_id}`
*   **Authentication:** Required (JWT)
*   **Description:** Updates an existing wish. Only the owner of the wish can update it. Only fields provided in the request body will be updated.
*   **Path Parameters:**
    *   `wish_id` (Long): The ID of the wish to update.
*   **Request Body:** `Wish` object (include fields to update, e.g., title, description, url, isArchived).
    ```json
    {
      "description": "Updated description",
      "isArchived": true
    }
    ```
*   **Success Response:** `200 OK` with the updated `Wish` object.
*   **Error Responses:** `400 Bad Request` (validation errors), `401 Unauthorized`, `403 Forbidden` (if not the owner), `404 Not Found`.

#### 5.2.5. Delete Wish

*   **Method:** `DELETE`
*   **Path:** `/api/wishlist/{wish_id}`
*   **Authentication:** Required (JWT)
*   **Description:** Deletes a wish. Only the owner of the wish can delete it.
*   **Path Parameters:**
    *   `wish_id` (Long): The ID of the wish to delete.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "Wish Num.{wish_id} is deleted"
    ```
*   **Error Responses:** `401 Unauthorized`, `403 Forbidden` (if not the owner), `404 Not Found`.

---

### 5.3. Important Date Endpoints (`/api/dates`)

#### 5.3.1. Create Important Date

*   **Method:** `POST`
*   **Path:** `/api/dates`
*   **Authentication:** Required (JWT)
*   **Description:** Adds a new important date record for the authenticated user.
*   **Request Body:** `ImportantDate` object (title and date required).
    ```json
    {
      "title": "Birthday",
      "date": "YYYY-MM-DD" // e.g., "2024-12-25"
    }
    ```
*   **Success Response:** `200 OK` with the created `ImportantDate` object (including generated ID, user).
*   **Error Responses:** `400 Bad Request` (validation errors), `401 Unauthorized`.

#### 5.3.2. Get Important Date by ID

*   **Method:** `GET`
*   **Path:** `/api/dates/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves a specific important date record by its ID.
*   **Path Parameters:**
    *   `id` (Long): The ID of the important date record.
*   **Success Response:** `200 OK` with the `ImportantDate` object.
*   **Error Responses:** `401 Unauthorized`, `404 Not Found`.

#### 5.3.3. Get All Important Dates by User ID

*   **Method:** `GET`
*   **Path:** `/api/dates/user/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Retrieves all important date records for a specific user.
*   **Path Parameters:**
    *   `id` (Long): The ID of the user whose records to retrieve.
*   **Success Response:** `200 OK` with a list of `ImportantDate` objects.
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if user ID does not exist).

#### 5.3.4. Update Important Date

*   **Method:** `PUT`
*   **Path:** `/api/dates/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Updates an existing important date record. Only the owner can update it.
*   **Path Parameters:**
    *   `id` (Long): The ID of the record to update.
*   **Request Body:** `ImportantDate` object (include fields to update, e.g., title, date).
    ```json
    {
      "title": "Anniversary Update",
      "date": "YYYY-MM-DD"
    }
    ```
*   **Success Response:** `200 OK` with the updated `ImportantDate` object.
*   **Error Responses:** `400 Bad Request` (validation errors), `401 Unauthorized`, `403 Forbidden` (if not the owner), `404 Not Found`.

#### 5.3.5. Delete Important Date

*   **Method:** `DELETE`
*   **Path:** `/api/dates/{id}`
*   **Authentication:** Required (JWT)
*   **Description:** Deletes an important date record. Only the owner can delete it.
*   **Path Parameters:**
    *   `id` (Long): The ID of the record to delete.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "Record Num.{id} is successfully deleted."
    ```
*   **Error Responses:** `401 Unauthorized`, `403 Forbidden` (if not the owner), `404 Not Found`.

---

### 5.4. Date Subscription Endpoints (`/api/subscription`)

These endpoints allow the authenticated user (follower) to subscribe/unsubscribe to important dates of another user (followee).

#### 5.4.1. Subscribe to Specific Date

*   **Method:** `POST`
*   **Path:** `/api/subscription/{followee_id}/{imp_date_id}`
*   **Authentication:** Required (JWT)
*   **Description:** Subscribes the authenticated user to a specific important date of another user.
*   **Path Parameters:**
    *   `followee_id` (Long): The ID of the user whose date to subscribe to.
    *   `imp_date_id` (Long): The ID of the specific important date record.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "You've successfully subscribed to this record."
    ```
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if user or date record doesn't exist/belong to followee), `409 Conflict` (if already subscribed).

#### 5.4.2. Subscribe to All Dates of a User

*   **Method:** `POST`
*   **Path:** `/api/subscription/{followee_id}/all`
*   **Authentication:** Required (JWT)
*   **Description:** Subscribes the authenticated user to all current important dates of another user. It also removes stale subscriptions if the followee deleted dates.
*   **Path Parameters:**
    *   `followee_id` (Long): The ID of the user to subscribe to.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "You've successfully subscribed to this user."
    ```
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if followee user doesn't exist).

#### 5.4.3. Unsubscribe from Specific Date

*   **Method:** `DELETE`
*   **Path:** `/api/subscription/{followee_id}/{imp_date_id}`
*   **Authentication:** Required (JWT)
*   **Description:** Unsubscribes the authenticated user from a specific important date of another user.
*   **Path Parameters:**
    *   `followee_id` (Long): The ID of the user whose date to unsubscribe from.
    *   `imp_date_id` (Long): The ID of the specific important date record.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "You've successfully unsubscribed from this record."
    ```
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if user, date record, or subscription doesn't exist).

#### 5.4.4. Unsubscribe from All Dates of a User

*   **Method:** `DELETE`
*   **Path:** `/api/subscription/{followee_id}/all`
*   **Authentication:** Required (JWT)
*   **Description:** Unsubscribes the authenticated user from all important dates of another user.
*   **Path Parameters:**
    *   `followee_id` (Long): The ID of the user to unsubscribe from.
*   **Success Response:** `200 OK` with a success message string.
    ```json
    "You've successfully unsubscribed from this user."
    ```
*   **Error Responses:** `401 Unauthorized`, `404 Not Found` (if followee user doesn't exist or no subscriptions exist).

---

## 6. Data Models

> [!NOTE]  
> These are the primary data structures used in API requests and responses. Note that sensitive fields like `password` aren't included in responses.

### 6.1. User

Represents a user account.

| Field         | Type   | Description                                      | Notes                                        |
| :------------ | :----- | :----------------------------------------------- | :------------------------------------------- |
| `id`          | Long   | Unique identifier for the user.                | Generated by the system.                     |
| `username`    | String | Unique username for login (8-20 characters).   | Required for registration and login.         |
| `email`       | String | User's email address.                            | Unique, required for registration. Write-only. |
| `password`    | String | User's password.                                 | Required for registration and login. Write-only. |
| `firstName`   | String | User's first name.                               | Required for registration.                   |
| `lastName`    | String | User's last name.                                | Required for registration.                   |
| `profilePhoto`| String | URL to the user's profile photo (optional).    |                                              |

*Write-only fields (`email`, `password`) are accepted in requests (like registration or login) but not returned in responses.*

### 6.2. Wish

Represents an item in a user's wishlist.

| Field         | Type    | Description                                      | Notes                               |
| :------------ | :------ | :----------------------------------------------- | :---------------------------------- |
| `id`          | Long    | Unique identifier for the wish.                  | Generated by the system.            |
| `user`        | User    | The user who owns this wish.                     | Only `id` might be present.         |
| `title`       | String  | Title of the wish (2-25 characters).             | Required.                           |
| `description` | String  | Optional description of the wish.                |                                     |
| `url`         | String  | Optional URL related to the wish (must be valid URL). |                                     |
| `createdAt`   | Instant | Timestamp when the wish was created.             | Generated by the system (ISO 8601). |
| `updatedAt`   | Instant | Timestamp when the wish was last updated.        | Updated automatically (ISO 8601).   |
| `isArchived`  | Boolean | Flag indicating if the wish is archived.         | Default: `false`.                   |

### 6.3. ImportantDate

Represents an important date record associated with a user.

| Field  | Type | Description                                    | Notes                             |
| :----- | :--- | :--------------------------------------------- | :-------------------------------- |
| `id`   | Long | Unique identifier for the important date.      | Generated by the system.          |
| `user` | User | The user who owns this date record.          | Only `id` might be present.       |
| `title`| String| Name or title of the important date (e.g., Birthday). | Required.                         |
| `date` | Date | The actual date (e.g., "YYYY-MM-DD").         | Required. Format might vary (e.g., timestamp or specific string format depending on JSON serialization). |

### 6.4. DateSubscription

Represents a subscription link between a follower user and a specific important date of a followee user.

| Field         | Type        | Description                                      | Notes                             |
| :------------ | :---------- | :----------------------------------------------- | :-------------------------------- |
| `id`          | Long        | Unique identifier for the subscription record.   | Generated by the system.          |
| `follower`    | User        | The user who is subscribing.                     | Only `id` might be present.       |
| `followee`    | User        | The user whose date is being subscribed to.      | Only `id` might be present.       |
| `importantDate`| ImportantDate | The specific date record being subscribed to.    | Only `id` might be present.       |
| `subscribedAt`| Instant     | Timestamp when the subscription was created.   | Generated by the system (ISO 8601). |

## 7. CORS Configuration

Cross-Origin Resource Sharing (CORS) is configured to allow requests from specific origins.
> [!IMPORTANT]  
> Configure CORS in an `ApplicationConfiguration.java` file by your own! These predefined fields are used as an example, and later will be transferred into an `application.configuration` file as an environment variables.  

> [!NOTE]
> This configuration is primarily intended for development environments. For production, the allowed origins should be updated to match the production frontend domain.

*   **Allowed Origins:** `http://localhost:3000` (Configured for frontend development).
*   **Allowed Methods:** All standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, etc.).
*   **Allowed Headers:** All headers are permitted.
*   **Credentials:** Allowed (`AllowCredentials` is set to `true`), meaning cookies and `Authorization` headers can be sent from the allowed origin.

## 8. Security Summary

*   **Framework:** Spring Security
*   **Authentication Method:** JWT Bearer Token
*   **Public Paths:**
    *   `POST /api/users/login`
    *   `POST /api/users/register`
    
*   **Authenticated Paths:** All other paths under `/api/**` require a valid JWT in the `Authorization` header.
*   **Session Management:** Stateless (`SessionCreationPolicy.STATELESS`), relying solely on the JWT for authentication state.
*   **CSRF Protection:** Disabled (common for stateless APIs consumed by non-browser clients or SPAs using token auth).
*   **Exception Handling:** Custom `JwtAuthenticationEntryPoint` handles authentication errors (e.g., invalid/expired tokens), returning `401 Unauthorized`. `GlobalExceptionHandler` handles other application and authorization errors.