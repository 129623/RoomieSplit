# API & Data Model Design

This document outlines the data structures and server-side API endpoints required to support the RoomieSplit Android application.

## 1. Data Models

### User
Represents a registered user of the application.
```json
{
  "id": "user_001",
  "username": "alex_chen",
  "displayName": "Alex Chen",
  "avatarUrl": "https://example.com/avatars/alex.jpg",
  "email": "alex@example.com",
  "created_at": "2023-10-01T12:00:00Z"
}
```

### Ledger (Dorm/Group)
Represents a shared accounting group (e.g., a Dorm).
```json
{
  "id": "ledger_302",
  "name": "302寝室日常",
  "description": "302 的日常开销",
  "ownerId": "user_001",
  "currency": "CNY",
  "members": [
    {
      "userId": "user_001",
      "role": "ADMIN",
      "status": "ACTIVE"
    },
    {
      "userId": "user_002",
      "role": "MEMBER",
      "status": "ACTIVE"
    }
  ],
  "stats": {
    "totalExpense": 2450.00,
    "pendingSettlement": 24.50
  },
  "createdAt": "2023-10-01T10:00:00Z"
}
```

### Transaction (Bill)
Represents a single expense record.
```json
{
  "id": "tx_1001",
  "ledgerId": "ledger_302",
  "amount": 185.50,
  "currency": "CNY",
  "category": "Food", // E.g., Dining, Transport, Shopping
  "description": "超市采购",
  "date": "2023-10-24T18:30:00Z",
  "payerId": "user_001",
  "splitType": "EQUAL", // EQUAL, WEIGHTED, EXACT
  "participants": [
    {
      "userId": "user_001",
      "owingAmount": 61.83,
      "paidAmount": 185.50
    },
    {
      "userId": "user_002",
      "owingAmount": 61.83,
      "paidAmount": 0
    },
    {
      "userId": "user_003",
      "owingAmount": 61.84,
      "paidAmount": 0
    }
  ],
  "note": "#Grocery",
  "images": []
}
```

### Debt (Settlement)
Represents the net owing status between two users within a ledger.
```json
{
  "fromUserId": "user_002",
  "toUserId": "user_001",
  "amount": 120.00,
  "ledgerId": "ledger_302"
}
```

### Notification
Represents a system or user triggered alert.
```json
{
  "id": "notif_505",
  "userId": "user_001",
  "type": "PAYMENT_REMINDER", // or "INVITE_REQUEST"
  "title": "还款提醒",
  "message": "您需要支付 Sarah $12 的披萨费用。",
  "isRead": false,
  "timestamp": "2023-10-25T09:00:00Z",
  "actionUrl": "roomiesplit://app/debt/tx_1001"
}
```

## 2. API Endpoints

### Authentication (Detailed)

#### 1. Login
Authenticate a user and retrieve a session token.
*   **POST** `/api/v1/auth/login`
*   **Request Body**:
    ```json
    {
      "email": "alex@example.com",
      "password": "securePassword123"
    }
    ```
*   **Response (200 OK)**:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 3600, // Seconds
      "refreshToken": "d8723f4g...",
      "user": {
        "id": "user_001",
        "username": "alex_chen",
        "email": "alex@example.com",
        "displayName": "Alex Chen",
        "avatarUrl": "..."
      }
    }
    ```
*   **Error Responses**:
    *   **400 Bad Request**: Missing email or password.
    *   **401 Unauthorized**: 
        ```json
        { "code": "AUTH_FAILED", "message": "Invalid email or password" }
        ```

#### 2. Register
Create a new user account.
*   **POST** `/api/v1/auth/register`
*   **Request Body**:
    ```json
    {
      "email": "newuser@example.com",
      "password": "securePassword123", // Min length 8
      "displayName": "New User"
    }
    ```
*   **Response (201 Created)**:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 3600,
      "user": { ... } // Same User object as login
    }
    ```
*   **Error Responses**:
    *   **400 Bad Request**: Weak password or invalid email format.
        ```json
        { "code": "WEAK_PASSWORD", "message": "Password must be at least 8 characters" }
        ```
    *   **409 Conflict**: Email already registered.
        ```json
        { "code": "USER_EXISTS", "message": "Email already in use" }
        ```

#### 3. Password Reset (Optional)
Initiate a password reset flow.
*   **POST** `/api/v1/auth/password-reset`
*   **Request**: `{ "email": "..." }`
*   **Response**: `{ "message": "Reset link sent to your email" }`

### User Profile
*   **GET** `/api/v1/users/me`
    *   **Headers**: `Authorization: Bearer <token>`
    *   **Response**: `User` object (including sensitive fields if any).
        ```json
        {
          "id": "...",
          "username": "...",
          "stats": {
             "totalAdvancedMonth": 1250.00,
             "advancedTrend": 12.0, // Percentage
             "myExpenseMonth": 840.00
          }
        }
        ```
*   **PUT** `/api/v1/users/me`
    *   **Request**: Update fields (avatar, displayName).

### Ledgers (Dorms)
*   **GET** `/api/v1/ledgers`
    *   **Response**: List of `Ledger` summaries for current user.
*   **POST** `/api/v1/ledgers`
    *   **Request**: 
    ```json
    { 
      "name": "New Dorm", 
      "description": "Optional description",
      "currency": "CNY",
      "members": ["user_id_1", "user_id_2"] 
    }
    ```
*   **GET** `/api/v1/ledgers/{id}/dashboard`
    *   **Response**: Aggregated stats for the dashboard (My Expense, Total, Roommates status).
    ```json
    {
      "myExpenseCurrentMonth": 850.00,
      "expenseTrend": 5.0, // Percentage
      "totalGroupExpense": 2450.00,
      "roommates": [
         { "userId": "user_002", "status": "joined", "lastActive": "10 min ago" }
      ]
    }
    ```

### Transactions (Bills)
*   **GET** `/api/v1/ledgers/{id}/transactions`
    *   **params**: `page`, `limit`, `month`
    *   **Response**: List of `Transaction` objects (Recent Activity).
*   **POST** `/api/v1/ledgers/{id}/transactions`
    *   **Purpose**: "Add Bill" (记一笔)
    *   **Request**: `Transaction` creation payload.
*   **GET** `/api/v1/custom-categories`
    *   **Response**: List of available categories (Dining, Transport, etc.).

### Debt & Settlement
    *   // See 'Debt Graph & Simplification' section below for graph data
    *   **Response**: Summary Object
    ```json
    {
      "netAsset": +120.00,
      "totalReceivable": 170.00,
      "totalPayable": 50.00
    }
    ```
*   **POST** `/api/v1/ledgers/{id}/settle`
    *   **Request**: `{ "toUserId": "...", "amount": 50.00 }` (Record Payment)

### Calendar
*   **GET** `/api/v1/ledgers/{id}/calendar`
    *   **params**: `year`, `month`
    *   **Response**:
    ```json
    {
      "monthlyTotal": 1250.00,
      "currency": "CNY",
      "days": [
          {
             "date": "2023-10-01",
             "totalAmount": 150.00,
             "hasEvent": true
          },
          {
             "date": "2023-10-05",
             "totalAmount": 45.00,
             "hasEvent": true
          }
      ]
    }
    ```
*   **GET** `/api/v1/ledgers/{id}/calendar/day`
    *   **params**: `date` (YYYY-MM-DD)
    *   **Response**: List of `Transaction` objects for that specific day.

    *   **Response**: List of `Transaction` objects for that specific day.

### Reports (Data Source)
*   **GET** `/api/v1/ledgers/{id}/reports/data`
    *   **Params**: `startDate` (YYYY-MM-DD), `endDate` (YYYY-MM-DD)
    *   **Response**: List of `Transaction` objects (JSON) for the specified range. 
    *   **Note**: This endpoint returns raw data. The client is responsible for generating the Excel/CSV file locally.

### Notifications
*   **GET** `/api/v1/notifications`
*   **POST** `/api/v1/notifications/{id}/read`

### Invitations
*   **POST** `/api/v1/ledgers/{id}/invite`
*   **POST** `/api/v1/invitations/{inviteId}/accept`
*   **POST** `/api/v1/invitations/{inviteId}/reject`

### Decision System (Polls)
*   **POST** `/api/v1/ledgers/{id}/polls`
    *   **Request**: 
    ```json
    {
      "title": "Tonight's Dinner",
      "options": ["Hotpot", "Pizza", "Sushi"],
      "mode": "VOTE" // or "RANDOM"
    }
    ```
    *   **Response**: `{ "pollId": "poll_001", "status": "ACTIVE" }` (or Result if Random)

*   **POST** `/api/v1/polls/{id}/vote`
    *   **Request**: `{ "optionIndex": 1 }`
    *   **Response**: `{ "message": "Vote recorded" }`

*   **GET** `/api/v1/polls/{id}/result`
    *   **Response**:
    ```json
    {
      "winner": "Hotpot",
      "details": [
        { "option": "Hotpot", "score": 2.5 },
        { "option": "Pizza", "score": 1.0 }
      ]
    }
    ```

### Karma Roulette (Hygiene)
*   **GET** `/api/v1/ledgers/{id}/karma`
    *   **Response**: List of members with their current karma stats.
    *   ```json
        [
          {
             "userId": "user_001",
             "username": "Alex",
             "baseWeight": 100,
             "accumulatedPoints": 15, // Points earned this cycle
             "finalWeight": 55.0, // 100 * 0.7 - 15
             "probability": 12.5, // Percentage
             "isGold": true
          },
          { "userId": "user_002", "finalWeight": 100.0, "probability": 22.5 }
        ]
        ```
*   **POST** `/api/v1/ledgers/{id}/karma/work`
    *   **Request**: 
    ```json
    { 
      "category": "Trash", 
      "points": 5,
      "description": "Took out trash" 
    }
    ```
    *   **Response**: `{ "message": "Karma +5 earned", "newPoints": 20 }`

*   **POST** `/api/v1/ledgers/{id}/karma/draw`
    *   **Request**: `{ "task": "Clean Toilet" }`
    *   **Response**: 
    *   ```json
        { 
          "winnerId": "user_002", 
          "task": "Clean Toilet",
          "message": "User_002 selected",
          "reset": true // Indicates karma was reset after draw
        }
        ```

### Debt Graph & Simplification
*   **GET** `/api/v1/ledgers/{id}/debt-graph`
    *   **Note**: Returns **RAW** original debt relationships (A owes B). Client performs simplification.
    *   **Response**:
    ```json
    {
      "nodes": [
         { "id": "user_a", "name": "Alice", "avatar": "..." },
         { "id": "user_b", "name": "Bob" },
         { "id": "user_c", "name": "Charlie" }
      ],
      "edges": [
         { "from": "user_a", "to": "user_b", "amount": 50.00, "currency": "CNY" },
         { "from": "user_b", "to": "user_c", "amount": 50.00, "currency": "CNY" }
      ]
    }
    ```

### Payment & Settlement
*   **POST** `/api/v1/ledgers/{id}/settle`
    *   **Purpose**: Instant settlement (e.g., WeChat Pay success).
    *   **Request**: 
    ```json
    { 
      "toUserId": "user_c", 
      "amount": 50.00,
      "method": "WECHAT" 
    }
    ```
    *   **Response**: `{ "success": true, "newBalance": 0 }`

*   **POST** `/api/v1/ledgers/{id}/payment-request`
    *   **Purpose**: Offline payment confirmation request.
    *   **Request**: `{ "toUserId": "user_c", "amount": 50.00, "method": "OFFLINE" }`
    *   **Response**: `{ "message": "Confirmation sent to user_c" }`
