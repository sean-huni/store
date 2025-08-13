# Store Application

The Store application keeps track of customers and orders in a database.

# Assumptions

This README assumes you're using a posix environment. It's possible to run this on Windows as well:

* Instead of `./gradlew` use `gradlew.bat`
* The syntax for creating the Docker container is different. You could also install PostgreSQL on bare metal if you
  prefer

# Prerequisites

This service assumes the presence of a postgresql 16.2 database server running on localhost:5433 (note the non-standard
port)
It assumes a username and password `admin:admin` can be used.
It assumes there's already a database called `store`

You can start the PostgreSQL instance like this:

```shell
docker run -d \
  --name postgres \
  --restart always \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=store \
  -v postgres:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:16.2 \
  postgres -c wal_level=logical
```

# Running the application

You should be able to run the service using

```shell
./gradlew bootRun
```

The application uses Liquibase to migrate the schema. Some sample data is provided. You can create more data by reading
the documentation in utils/README.md

# Data model

An order has an ID, a description, and is associated with the customer which made the order.
A customer has an ID, a name, and 0 or more orders.

# API

Two endpoints are provided:

* /order
* /customer

Each of them supports a POST and a GET. The data model is circular - a customer owns a number of orders, and that order
necessarily refers back to the customer which owns it.
To avoid loops in the serializer, when writing out a Customer or an Order, they're mapped to CustomerDTO and OrderDTO
which contain truncated versions of the dependent object - CustomerOrderDTO and OrderCustomerDTO respectively.

The API is documented in the OpenAPI file OpenAPI.yaml. Note that this spec includes part of one of the tasks below (the
new /products endpoint)

# Tasks

1. Extend the order endpoint to find a specific order, by ID
2. Extend the customer endpoint to find customers based on a query string to match a substring of one of the words in
   their name
3. Users have complained that in production the GET endpoints can get very slow. The database is unfortunately not
   co-located with the application server, and there's high latency between the two. Identify if there are any
   optimisations that can improve performance
4. Add a new endpoint /products to model products which appear in an order:
    * A single order contains 1 or more products.
    * A product has an ID and a description.
    * Add a POST endpoint to create a product
    * Add a GET endpoint to return all products, and a specific product by ID
    * In both cases, also return a list of the order IDs which contain those products
    * Change the orders endpoint to return a list of products contained in the order

# Bonus points

1. Implement a CI pipeline on the platform of your choice to build the project and deliver it as a Dockerized image

# Notes on the tasks

Assume that the project represents a production application.
Think carefully about the impact on performance when implementing your changes
The specifications of the tasks have been left deliberately vague. You will be required to exercise judgement about what
to deliver - in a real world environment, you would clarify these points in refinement, but since this is a project to
be completed without interaction, feel free to make assumptions - but be prepared to defend them when asked.
There's no CI pipeline associated with this project, but in reality there would be. Consider the things that you would
expect that pipeline to verify before allowing your code to be promoted
Feel free to refactor the codebase if necessary. Bad choices were deliberately made when creating this project.

# Additional Tasks

1. OpenAPI - Done
2. Security - Done
3. Jacoco - Done
4. Faker Data - Done

# Why not H2

PostgreSQL scripts have a slightly different syntax compared to H2 (e.g BigSerial), as a result some script WILL fail
when switching
to postgreSQL

# Decision Record

- Deleted CustomerOrderDTO & OrderCustomerDTO -> No need for an additional truncated DTO objects for fields that can be
  nullified when field-data is not required, and chosen an alternative way to break circular DTO dependencies

# Executing the DevOps pipeline

- Using SDKMan for Dev Env: `sdk i java 24.0.2-graalce && sdk env init && sdk env`
-

`docker compose -f src/test/resources/dc/test-tools.yml down -v --remove-orphans && docker compose -f src/test/resources/dc/test-tools.yml up -d`
- Execute sonarqube analysis with the following command:
  `./gradlew sonar -Dsonar.projectKey=Equal-Experts -Dsonar.projectName='Equal-Experts' -Dsonar.host.url=http://localhost:9000 -Dsonar.token=sqp_eafffb588bf1d4d9b2c5c6f350254a70bb11793f`
- Sonarque analysis can be found at: [SonarQube](http://localhost:9000)
- Take note of the Generated Token

# GraalVM Native Image Support

This application now supports GraalVM native image compilation for Java 24, which provides the following benefits:

- Faster startup time (milliseconds instead of seconds)
- Lower memory footprint
- Reduced CPU usage
- Smaller container images
- No JVM warmup period

## Building a Native Image

To build a native image, you need GraalVM installed. You can use SDKMan:

```shell
sdk i java 24.0.2-graalce
sdk env
```

Then build the native image with:

```shell
./gradlew nativeCompile
```

The native executable will be created in `build/native/nativeCompile/store`.

### Using the build-native-local.sh Script

Alternatively, you can use the provided `build-native-local.sh` script to build the native image:

```shell
./build-native-local.sh
```

This script:

- Checks if GraalVM's native-image tool is installed
- Cleans the project
- Builds the native image with optimized settings
- Verifies the build was successful
- Makes the executable file executable

The script provides additional parameters to the build process:

- `-Porg.gradle.java.installations.auto-download=false`
- `-Dspring.native.mode=reflection`
- `-Dspring.native.verbose=true`

## Running the Native Image

After building, you can run the native executable directly:

```shell
build/native/nativeCompile/store --spring.profiles.active=dev --DB_HOST=localhost:5433 --DB_NAME=store --DB_PASS=postgres --DB_USER=postgres
```

## Native Image Configuration

The native image is configured with:

- Support for Java 24 virtual threads
- HTTP/HTTPS protocol support
- Reflection configuration for Spring Boot components
- Resource inclusion for configuration files
- Memory optimization settings

If you encounter issues with the native image, you can run the application with the agent to generate additional
configuration:

```shell
./gradlew bootRun -Pargs="--spring.profiles.active=dev" -Pagent
```

This will generate additional configuration files in `src/main/resources/META-INF/native-image/`.

# API Documentation

## Prerequisites

Before interacting with the API, ensure you have:

1. **Database Setup**: PostgreSQL database running on localhost:5433
2. **Application Running**: Start the application using one of the methods below

## Starting the Application

### JVM Mode (Recommended for Development)

```bash
./gradlew bootRun --args="--spring.profiles.active=dev --DB_HOST=localhost:5433 --DB_NAME=store --DB_PASS=postgres --DB_USER=postgres"
```

### Native Image Mode (Production)

```bash
# First build the native image
./gradlew nativeCompile
# Then run it
build/native/nativeCompile/store --spring.profiles.active=dev --DB_HOST=localhost:5433 --DB_NAME=store --DB_PASS=postgres --DB_USER=postgres
```

## API Documentation Resources

- **OpenAPI Specification**: http://localhost:8080/v3/api-docs
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Base URL**: http://localhost:8080

## Authentication Flow

The API uses JWT (JSON Web Token) authentication. All endpoints except authentication endpoints require a valid JWT
token.

### 1. Register a New User

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/register' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Alice",
    "lastName": "Wonderland", 
    "email": "alice@example.com",
    "password": "password123"
  }'
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### 2. Authenticate Existing User

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/authenticate' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "alice@example.com",
    "password": "password123"
  }'
```

### 3. Refresh Token

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/refresh-token' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_REFRESH_TOKEN'
```

## API Endpoints

### Customer Management

#### Get All Customers

```bash
curl -X 'GET' \
  'http://localhost:8080/customers?page=0&limit=10&sortDir=ASC' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Get Customer by ID

```bash
curl -X 'GET' \
  'http://localhost:8080/customers/1' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Create Customer

```bash
curl -X 'POST' \
  'http://localhost:8080/customers' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -d '{
    "name": "John Doe"
  }'
```

#### Search Customers by Name

```bash
curl -X 'GET' \
  'http://localhost:8080/customers?name=John&page=0&limit=10' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

### Product Management

#### Get All Products

```bash
curl -X 'GET' \
  'http://localhost:8080/products?page=0&limit=10&sortDir=ASC' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Get Product by ID

```bash
curl -X 'GET' \
  'http://localhost:8080/products/1' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Create Product

```bash
curl -X 'POST' \
  'http://localhost:8080/products' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -d '{
    "description": "Sample Product",
    "sku": "12345678-1234-1234-1234-123456789012"
  }'
```

### Order Management

#### Get All Orders

```bash
curl -X 'GET' \
  'http://localhost:8080/orders?page=0&limit=10&sortDir=ASC' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Get Order by ID

```bash
curl -X 'GET' \
  'http://localhost:8080/orders/1' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

#### Create Order

```bash
curl -X 'POST' \
  'http://localhost:8080/orders' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -d '{
    "description": "Sample Order",
    "customerId": 1,
    "productIds": [1, 2]
  }'
```

## Query Parameters

### Pagination Parameters

- `page`: Page number (0-indexed, minimum: 0)
- `limit`: Number of items per page (minimum: 5)
- `sortBy`: Field to sort by (optional)
- `sortDir`: Sort direction (`ASC` or `DESC`)

### Example with Pagination

```bash
curl -X 'GET' \
  'http://localhost:8080/products?page=1&limit=20&sortBy=id&sortDir=DESC' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

## Error Handling

The API returns structured error responses with the following format:

```json
{
  "name": "BAD_REQUEST",
  "message": "Validation failed. Please check your input",
  "violations": [
    {
      "field": "email",
      "rjctValue": "invalid-email",
      "errMsg": "Invalid email. Enter a valid email",
      "errCode": "auth.400.000"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Common HTTP Status Codes

- `200 OK`: Successful GET requests
- `201 Created`: Successful POST requests
- `400 Bad Request`: Validation errors or malformed requests
- `401 Unauthorized`: Missing or invalid authentication token
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists (e.g., duplicate email)
- `500 Internal Server Error`: Server-side errors

## Testing with Swagger UI

1. Navigate to http://localhost:8080/swagger-ui/index.html
2. Click "Authorize" button
3. Enter your JWT token in the format: `Bearer YOUR_ACCESS_TOKEN`
4. Test endpoints directly from the browser interface

## Complete Workflow Example

Here's a complete example of registering a user, creating a customer, product, and order:

```bash
# 1. Register user and get token
RESPONSE=$(curl -s -X 'POST' \
  'http://localhost:8080/auth/register' \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Alice",
    "lastName": "Wonderland",
    "email": "alice@example.com", 
    "password": "password123"
  }')

# Extract access token (requires jq)
TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

# 2. Create a customer
CUSTOMER_RESPONSE=$(curl -s -X 'POST' \
  'http://localhost:8080/customers' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name": "John Doe"}')

CUSTOMER_ID=$(echo $CUSTOMER_RESPONSE | jq -r '.id')

# 3. Create a product
PRODUCT_RESPONSE=$(curl -s -X 'POST' \
  'http://localhost:8080/products' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "description": "Sample Product",
    "sku": "12345678-1234-1234-1234-123456789012"
  }')

PRODUCT_ID=$(echo $PRODUCT_RESPONSE | jq -r '.id')

# 4. Create an order
curl -X 'POST' \
  'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"description\": \"Sample Order\",
    \"customerId\": $CUSTOMER_ID,
    \"productIds\": [$PRODUCT_ID]
  }"
```

## Database Setup

Ensure PostgreSQL is running with the following configuration:

- Host: localhost
- Port: 5433
- Database: store
- Username: postgres
- Password: postgres

You can start PostgreSQL using Docker:

```bash
docker run --name postgres-store \
  -e POSTGRES_DB=store \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  -d postgres:17-alpine
```

# Potential Areas of Improvements

- Use simplified Paketo Buildpacks (or paketo-buildpacks) built-in tool to build the docker-images from Spring Boot
  Projects.
- Complete the .k8/ yml config for both backend & database namespaces, for the k8 deployments.