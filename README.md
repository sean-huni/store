# Store Application

The Store application keeps track of customers and orders in a database.

## About

This project demonstrates modern Java enterprise application development using Spring Boot 4.x, GraalVM native images,
and advanced testing strategies. It serves as a comprehensive example of best practices in microservice architecture,
database optimization, and cloud-native deployment.

## Author & Maintainer

**Project Maintainer**: [Sean Huni/SecuritEase]

- **Website**: [Sean Huni](https://sean-huni.xyz)
- **Support**: [sean2kay@gmail.com](mailto:sean2kay@gmail.com)

For technical questions, feature requests, or contributions, please visit
our [GitHub repository](https://github.com/sean-huni/store) or contact us through the channels above.

**Last Updated**: December 2024

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
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=store \
  -v postgres:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:17-alpine \
  postgres -c wal_level=logical
```

**Note**: The credentials have been updated to use `postgres:postgres` for consistency with the application
configuration and the PostgreSQL container now uses the alpine variant for better performance.

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
when switching to postgreSQL.

# Decision Record

- Deleted CustomerOrderDTO & OrderCustomerDTO -> No need for an additional truncated DTO objects for fields that can be
  nullified when field-data is not required, and chosen an alternative way to break circular DTO dependencies.

# Executing the DevOps pipeline

- Using SDKMan for Dev Env: `sdk i java 24.0.2-graalce && sdk env init && sdk env`
-

`docker compose -f src/test/resources/dc/test-tools.yml down -v --remove-orphans && docker compose -f src/test/resources/dc/test-tools.yml up -d`

- Execute sonarqube analysis with the following command:
  `./gradlew sonar -Dsonar.projectKey=Store -Dsonar.projectName='Store' -Dsonar.host.url=http://localhost:9000 -Dsonar.token=sqp_3ffa37971fe09500f622ad4c8f388bd66946950b`
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
sdk i java 25-graalce
sdk env init && echo "Java Version: $(cat .sdkmanrc | grep java | awk -F= '{print $2}')"

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

The native image is configured with advanced optimizations:

### Virtual Threads & Performance Features

- **Java 25 Virtual Threads**: Full support with `--enable-preview` flag
- **HTTP/HTTPS Protocol Support**: Built-in networking capabilities
- **Native Architecture Optimization**: `-march=native` for hardware-specific optimizations
- **Epsilon GC**: Zero-allocation garbage collector for optimal performance

### Build Arguments

```gradle
graalvmNative {
    binaries {
        main {
            imageName = "store"
            buildArgs.addAll([
                "--enable-preview",                    // Virtual threads support
                "--enable-url-protocols=http,https",   // Network protocols
                "--initialize-at-build-time=org.slf4j,ch.qos.logback", // Logging optimization
                "-H:+ReportExceptionStackTraces",      // Better debugging
                "-H:+AddAllCharsets",                  // Character set support
                "--gc=epsilon",                        // Zero-allocation GC
                "-march=native",                       // Hardware optimization
                "--no-fallback",                       // Pure native image
                "-Ob",                                 // Optimized build
                "-J-Xmx12g",                          // Build memory (12GB)
                "-J-XX:MaxMetaspaceSize=2g",          // Metaspace memory
                "-H:+UnlockExperimentalVMOptions"      // Experimental features
            ])
        }
    }
}
```

### Memory & Resource Management

- **Build Memory**: 12GB heap allocation during native image compilation
- **Metaspace**: 2GB dedicated metaspace memory
- **Resource Inclusion**: Automatic inclusion of configuration files
- **Reflection Configuration**: Pre-configured for Spring Boot components

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

# SQL Query Optimisations (The N+1 Phenomena)

## What is the N+1 Problem?

The N+1 problem is a common performance issue in ORM frameworks where:

1. **Initial Query (1)**: One query fetches a list of parent entities (e.g., customers)
2. **Additional Queries (N)**: For each parent entity, a separate query fetches its related entities (e.g., orders)

**Example of N+1 Problem:**

```sql
-- Initial query (1)
SELECT *
FROM customer LIMIT 10;

-- N additional queries (one for each customer)
SELECT *
FROM "order"
WHERE customer_id = 1;
SELECT *
FROM "order"
WHERE customer_id = 2;
-- ... 8 more queries for each customer
```

This results in **11 queries** instead of the optimal **2 queries**.

## How This Project Prevents N+1 Queries

### 1. Default Lazy Loading Strategy

For all entities annotated with `FetchType.LAZY` operations, the project enforces lazy loading by default:

```java
// Customer entity
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private final Set<Order> orders = new HashSet<>();

// Order entity
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;

@OneToMany(cascade = CascadeType.ALL, mappedBy = "order", fetch = FetchType.LAZY)
private final List<ProductOrder> products = new ArrayList<>();
```

### 2. Selective Eager Loading with @EntityGraph

When related data is needed, we use `@EntityGraph` to fetch it efficiently in a single query:

```java

@Query(value = "from Customer c where c.id = :id")
@EntityGraph(attributePaths = {"orders"})
Optional<Customer> findCustomerById(@Param("id") Long id);
```

**Generated SQL:**

```sql
SELECT c.*, o.*
FROM customer c
         LEFT JOIN "order" o ON c.id = o.customer_id
WHERE c.id = ?
```

### 3. Application Configuration Enforcement

The `application.yml` enforces strict lazy loading policies:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Enforce lazy loading globally
        enable_lazy_load_no_trans: false
        # Batch fetching optimization
        default_batch_fetch_size: 10
        # Enforce strict lazy loading
        bytecode:
          use_reflection_optimizer: false
    # Ensure lazy loading is default
    open-in-view: false
```

### 4. Key Configuration Settings

- **`enable_lazy_load_no_trans: false`**: Prevents lazy loading outside transactions, forcing proper data access
  patterns
- **`open-in-view: false`**: Disables the anti-pattern that can hide N+1 problems
- **`default_batch_fetch_size: 10`**: When lazy loading is necessary, fetch in batches rather than individually

## Best Practices Implemented

### ✅ DO: Use @EntityGraph for Known Related Data

```java
// When you know you need orders, fetch them eagerly
@EntityGraph(attributePaths = {"orders"})
Optional<Customer> findCustomerById(@Param("id") Long id);
```

### ✅ DO: Keep FetchType.LAZY as Default

```java
// All relationships default to lazy loading
@OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
private final Set<Order> orders = new HashSet<>();
```

### ❌ DON'T: Use FetchType.EAGER Globally

```java
// This would cause all queries to fetch orders, even when not needed
@OneToMany(mappedBy = "customer", fetch = FetchType.EAGER) // ❌ Bad
private final Set<Order> orders = new HashSet<>();
```

### ✅ DO: Use Batch Fetching for Unavoidable Lazy Loading

The `default_batch_fetch_size: 10` setting ensures that if lazy loading occurs, it fetches 10 entities at once instead
of one by one.

## Performance Monitoring

The project includes tools to detect N+1 problems:

1. **P6Spy**: Logs all SQL queries with execution time
2. **Flexy Pool**: Monitors connection pool usage
3. **QuickPerf**: Test annotations to detect N+1 queries in tests

```java

@Test
@ExpectSelect(1)
    // Ensures only 1 query is executed
void shouldFetchCustomerWithOrdersInOneQuery() {
    customerRepo.findCustomerById(1L);
}
```

## Verification

The `CustomerRepoTest` demonstrates N+1 prevention:

- Customer with orders is fetched in a single query using `@EntityGraph`
- Test verifies that all related orders are loaded
- No additional queries are executed for order access

# Build Packs

## With pack-cli

Configuring & Building with paketobuildpacks commands:

### JVM Image

```shell
# Set default builder
pack config default-builder paketobuildpacks/builder-jammy-base

# Build JVM image
pack build store-app \
  --builder paketobuildpacks/builder-jammy-base \
  --env BP_JVM_VERSION=25
```

### Native Image

```shell
# Set default builder for native images
pack config default-builder paketobuildpacks/builder-jammy-tiny

# Build native image
pack build store-app-native \
  --builder paketobuildpacks/builder-jammy-tiny \
  --env BP_NATIVE_IMAGE=true \
  --env BP_JVM_VERSION=25
```

## With Spring Boot (Gradlew)

### JVM Image

```shell
# Build JVM image using Spring Boot buildpacks
./gradlew bootBuildImage --imageName=store-app
```

### Native Image

```shell
# Build native image using Spring Boot buildpacks
./gradlew bootBuildImage --imageName=store-app-native \
  -Pnative
```

### Current Build Configuration

The project is currently configured for **native image builds by default**:

```gradle
// Current configuration in build.gradle
tasks.named('bootBuildImage') {
    builder = 'paketobuildpacks/builder-jammy-tiny'
    imageName = "${project.name}:${project.version}"
    environment = [
        'BP_NATIVE_IMAGE': 'true',   // Native images enabled by default
        'BP_JVM_VERSION' : '25'
    ]
}
```

### Alternative JVM Configuration

To build JVM images instead, update your `build.gradle`:

```gradle
tasks.named('bootBuildImage') {
    builder = 'paketobuildpacks/builder-jammy-base'  // Use base builder for JVM
    imageName = "${project.name}:${project.version}"
    environment = [
        'BP_JVM_VERSION': '25'
        // Remove 'BP_NATIVE_IMAGE': 'true' for JVM builds
    ]
}
```

### Reference

Paketobuildpacks Inspired by: https://www.youtube.com/watch?v=nesRmaUi4Ts

# Potential Areas of Improvements

- Use simplified Paketo Buildpacks (or paketo-buildpacks) built-in tool to build the docker-images from Spring Boot
  Projects. Status: ✅
- Complete the .k8/ yml config for both backend & database namespaces, for the k8 deployments.

## Testing

### Sonarqube

The basis for scanning developer-induced bugs, code-smells & unnecessary complexities. Sonarqube recommendations should
not be ignored, but seriously considered as part of code review.

### Jacoco

Jacoco is the basis of the code-coverage quality metric. After running unit tests with `./gradlew test jacocoTestReport`
navigate
to the `build/reports/jacoco/test/html` & open the `index.html` via the Browser and utilise the visual aid to focus on
uncovered
test-cases from the unit tests results.

### Unit Tests

As soon as you pull the code from the repo, execute: `./gradlew clean build` & you should see all the tests pass. If any
test fails, create a Jira task & assign the task to the responsible to fix the broken unit tests. Unit Tests should be
tagged with `@Tag("unit")` annotation at the top of the class.

No commenting out unit tests. Unit tests that aren't ready can be marked as `@Disbled` annotation to indicate that the
test is ignored, and this will show up as warnings during the builds.

### Integration Tests

Integration Tests are generally slower & can impact developer productivity. All tests marked with an `@SpringBootTest`
should also be marked with an `@Tag("int")` annotation & can be ignored during development, but must never
be ignored prior to creating a PR (Pull Request). All broken Integration Tests should be fixed.

Checkout the documentation for Tagging Integration Tests:

[JUnit-5 Tagging Tests](https://www.baeldung.com/junit-filtering-tests)

[JUnit-5 Tag Expression](https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions)

## Application Architecture (Monolithic Design)

This is a monolithic Spring Boot application that follows best practices for enterprise application development.
While it's built as a single deployable unit, it still benefits from following proven architectural principles
such as the [12-Factor App](https://12factor.net/) methodology, which provides excellent guidance for building
maintainable, scalable, and cloud-ready applications regardless of their architectural style.

The 12-Factor principles are particularly valuable for ensuring our monolithic application remains:

- **Portable** across different environments
- **Scalable** through stateless design and proper resource management
- **Maintainable** with clear separation of concerns and configuration management

Kindly ensure that you familiarise yourself with [The 12 Factor App](https://12factor.net/).

## Manifest

The manifest contains the Bill Of Materials (BOM) which is a comprehensive list of the required dependencies used in
the Spring Ecosystem.

| Dependency     | Version  |   AS EoL    |   SS EoL    | Last Updated |
|:---------------|:--------:|:-----------:|:-----------:|:------------:|
| Graal-CE (JRE) |    25    | 30-Sep-2026 | 30-Sep-2029 | 18-Sep-2025  |
| Spring Boot    | 4.0.0-M2 | 30-Nov-2025 | 30-Nov-2027 | 18-Sep-2025  |
| Gradle         |  9.0.0   |     LTS     |     LTS     | 18-Sep-2025  |
| Gson           | (latest) |      -      |      -      | 18-Sep-2025  |

To further add onto the SBOM, the following dependencies are also included:

- CycloneDX: `./gradlew cyclonedxBom` to generate the SBOM (Software Bill of Materials).

[OSS End of Life](https://endoflife.date)
[Java Release Roadmap](https://en.wikipedia.org/wiki/Java_version_history)

**End-of-Life** (EoL): Indication of when support for app/module/dependency ends.

**Active support** (AS): Minor versions are actively supported for some time after their initial release. During this
time, reported bugs and security issues are fixed, and regular point releases are made.

**Security Support** (SS): Only minor releases supported for critical security issues, and releases are no longer made
on a regular basis.

Ideally, we should ensure that no obsolete framework/dependency versions should be deployed into QA/STRESS/PROD.

## Git Branching Model

Normal flow of code changes between branches.
`feat-*` -> `dev` -> `int-*` -> `qa-*` -> `stress-*` -> `master`

In the event of a bug that's currently in PROD:
`hotfix-*` -> `qa-*` -> `stress-*` -> `master`

`hotfix-*` -> `dev`

Following a successful [Git Branching Model](https://nvie.com/posts/a-successful-git-branching-model/) we would be
adopting the same best practices. It's important to spend some time to understand the branching model &
avoid anti-pattern practices.

There shall be strict branch policies to prevent unauthorised git pushes into the wrong branches, which could
potentially cause unnecessary headaches.

# Gradle & Spring Boot Version Upgrades

This project has been upgraded to use the latest versions of Java, Spring Boot, and Gradle. Below are the upgrade steps
and considerations for each component.

## Current Versions

- **Java**: 25 (LTS support and latest features)
- **Spring Boot**: 4.0.0-M3 (Milestone release with Spring Framework 7.x)
- **Gradle**: 9.0.0 (Latest stable release with improved performance)
- **JUnit**: 6.0.0 (Latest testing framework with enhanced features)

## Java 25 Upgrade

### Prerequisites

- Install Java 25 SDK (OpenJDK or Oracle JDK)
- Update `JAVA_HOME` environment variable
- Verify installation: `java -version`

### Configuration Changes

1. Update `build.gradle`:
   ```gradle
   ext {
       javaVersion = 25
   }
   
   java {
       toolchain {
           languageVersion = JavaLanguageVersion.of(javaVersion)
       }
   }
   ```

2. Update `.sdkmanrc` (if using SDKMAN):
   ```
   java=25-open
   ```

### Benefits

- Enhanced performance with latest JVM optimizations
- New language features and API improvements
- Better compatibility with modern Spring Boot versions

### ZGC Preference

**ZGC (Z Garbage Collector)** is a low-latency garbage collector designed for applications requiring consistent response
times. It's particularly beneficial for our SQL performance monitoring setup as it minimizes GC pauses that could
interfere with precise timing measurements.

#### Key Benefits

- **Ultra-low latency**: Sub-millisecond pause times regardless of heap size
- **Scalable**: Works efficiently from 8MB to 16TB heaps
- **Concurrent**: Most GC work happens concurrently with application threads
- **Predictable performance**: Ideal for monitoring applications with strict timing requirements

#### Recommended Configuration

```bash
# Essential ZGC flags
-XX:+UseZGC
-XX:+UnlockExperimentalVMOptions    # Required for Java 11-16, optional for Java 17+
-Xmx4g                             # Maximum heap size
-Xms4g                             # Initial heap size (same as max for predictable performance)

# Optional tuning parameters
-XX:+UnlockDiagnosticVMOptions     # Enable diagnostic options
-XX:ZCollectionInterval=5          # Force GC every 5s if allocation rate is low
-XX:ZUncommitDelay=300            # Return unused memory to OS after 5 minutes
-XX:ZPath=/tmp                    # Specify backing file path (Linux/macOS only)

# Monitoring and logging (useful for performance analysis)
-XX:+LogVMOutput
-Xlog:gc*:gc.log:time
```

#### Memory Sizing Guidelines

| Application Size | Recommended Heap    | Use Case                            |
|:-----------------|:--------------------|:------------------------------------|
| **Small/Dev**    | `-Xmx1g -Xms1g`     | Local development, testing          |
| **Medium**       | `-Xmx4g -Xms4g`     | Production apps, moderate load      |
| **Large**        | `-Xmx8g -Xms8g`     | High-load production systems        |
| **Enterprise**   | `-Xmx16g+ -Xms16g+` | Large-scale enterprise applications |

#### Production Example

For our SQL monitoring application in production:

```bash
java -XX:+UseZGC \
     -Xmx4g \
     -Xms4g \
     -XX:+UnlockDiagnosticVMOptions \
     -XX:ZUncommitDelay=300 \
     -jar store.jar
```

#### Performance Impact on SQL Monitoring

ZGC's consistent low-latency characteristics make it ideal for our comprehensive SQL performance monitoring stack:

- **@TrackSqlPerf annotations**: No GC interference with nanosecond-precision timing
- **Hypersistence Optimizer**: Stable performance during entity analysis
- **datasource-proxy**: Consistent query logging without GC-induced delays
- **Prometheus metrics**: Reliable metric collection timing

## ZGC for Cloud Kubernetes Deployments

### Cloud-Native Benefits

**ZGC with Java 25** provides exceptional advantages for containerized applications running in Kubernetes environments:

#### 🚀 **Container Resource Efficiency**

- **Memory elasticity**: ZGC automatically adapts to container memory limits without manual tuning
- **CPU awareness**: Works seamlessly with K8s CPU quotas and cgroup limits
- **Resource predictability**: Consistent memory usage patterns for better pod scheduling

#### ⚡ **Kubernetes-Optimized Performance**

- **Sub-millisecond pause times**: Critical for maintaining SLA during pod scaling events
- **Rolling update compatibility**: Minimal disruption during K8s rolling deployments
- **Horizontal scaling**: Consistent performance across pod replicas regardless of heap size

#### 🔧 **Cloud Infrastructure Integration**

- **Memory pressure handling**: Graceful behavior under K8s memory constraints
- **OOM prevention**: Better memory management reduces pod restarts
- **Observability**: Enhanced metrics integration with Prometheus/Grafana monitoring

### Container-Optimized Configuration

Our cloud-native Dockerfile (`.docker/Dockerfile`) implements ZGC optimizations specifically for Kubernetes:

```bash
# ZGC Configuration optimized for cloud/K8s
-XX:+UseZGC
-XX:+UnlockExperimentalVMOptions
# Memory management with percentage-based allocation for containers
-XX:MaxRAMPercentage=70.0      # Use 70% of container memory limit
-XX:InitialRAMPercentage=40.0   # Start with 40% for faster startup
-XX:MinRAMPercentage=20.0       # Minimum 20% for small containers
# ZGC-specific tuning for containerized workloads
-XX:ZCollectionInterval=5       # Frequent collections for container efficiency
-XX:ZUncommitDelay=300          # Return unused memory to K8s after 5 minutes
```

#### Memory Sizing for Kubernetes Pods

| Pod Memory Limit | ZGC Heap (70%) | Use Case                              | Recommended CPU |
|:-----------------|:---------------|:--------------------------------------|:----------------|
| **512Mi**        | ~358Mi         | Microservices, lightweight workloads  | 0.5-1.0 CPU     |
| **1Gi**          | ~716Mi         | Standard Spring Boot applications     | 1.0-2.0 CPU     |
| **2Gi**          | ~1.4Gi         | Medium-load applications with caching | 2.0-4.0 CPU     |
| **4Gi**          | ~2.8Gi         | High-throughput SQL monitoring apps   | 4.0-8.0 CPU     |
| **8Gi**          | ~5.6Gi         | Enterprise applications, heavy load   | 8.0-16.0 CPU    |

### Kubernetes Deployment Example

#### 1. **Pod Resource Configuration**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: store-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: store-app
  template:
    metadata:
      labels:
        app: store-app
    spec:
      containers:
        - name: store-app
          image: store:2.0.0
          resources:
            requests:
              memory: "1Gi"
              cpu: "1000m"
            limits:
              memory: "2Gi"      # ZGC will use ~1.4Gi (70%)
              cpu: "2000m"
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "cloud"
          # Health checks leverage our built-in endpoints
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
```

#### 2. **Horizontal Pod Autoscaler (HPA)**

ZGC's consistent performance makes it ideal for auto-scaling:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: store-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: store-app
  minReplicas: 3
  maxReplicas: 50
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

#### 3. **Pod Disruption Budget**

Ensure availability during rolling updates:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: store-app-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: store-app
```

### Cloud Provider Optimizations

#### **AWS EKS**

```bash
# EKS-optimized JVM flags (added to our Dockerfile)
-XX:+UseTransparentHugePages          # Leverage AWS Nitro system
-XX:+UseLargePages                    # Better memory performance on EC2
```

#### **Google GKE**

```bash
# GKE-optimized flags for Google Cloud infrastructure
-XX:+UseContainerSupport              # Already included in our config
-XX:+PreferContainerQuotaForCPUCount  # Already included in our config
```

#### **Azure AKS**

```bash
# AKS-optimized configuration
-XX:+ExitOnOutOfMemoryError           # Fast fail for Azure Load Balancer health
-XX:+HeapDumpOnOutOfMemoryError       # Debug support with Azure Storage
```

### Production Monitoring Integration

Our ZGC configuration integrates seamlessly with cloud monitoring:

#### **Prometheus Metrics**

```yaml
# Automatically exposed via /actuator/prometheus
jvm_gc_pause_seconds{gc="ZGC"}        # GC pause times (sub-millisecond)
jvm_memory_used_bytes{area="heap"}    # Heap usage patterns
jvm_gc_memory_allocated_bytes_total   # Allocation rate monitoring
```

#### **Grafana Dashboard Queries**

```promql
# ZGC Performance Dashboard
rate(jvm_gc_pause_seconds_count{gc="ZGC"}[5m])         # GC frequency
histogram_quantile(0.99, jvm_gc_pause_seconds{gc="ZGC"}) # 99th percentile pause time
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100  # Heap utilization %
```

### Troubleshooting Cloud Deployments

#### **Common Issues & Solutions**

| Issue                   | Symptom                             | Solution                                                   |
|:------------------------|:------------------------------------|:-----------------------------------------------------------|
| **OOM in small pods**   | Pod restarts, memory limit exceeded | Increase pod memory limit or reduce `-XX:MaxRAMPercentage` |
| **Slow startup**        | ReadinessProbe timeouts             | Increase `-XX:InitialRAMPercentage` or readiness delay     |
| **Memory not returned** | High memory usage after load        | Tune `-XX:ZUncommitDelay` to return memory faster          |
| **Poor autoscaling**    | Inconsistent resource metrics       | Ensure proper resource requests/limits configuration       |

#### **Debug Commands for K8s**

```bash
# Check ZGC status in running pod
kubectl exec -it <pod-name> -- jcmd 1 GC.run_finalization
kubectl exec -it <pod-name> -- jcmd 1 VM.info | grep -i zgc

# Monitor GC logs
kubectl logs <pod-name> | grep -i "zgc\|gc"

# Resource usage monitoring
kubectl top pods -l app=store-app
```

### Security Considerations

Our cloud-native configuration includes security hardening:

- **Non-root user**: Runs as UID 1000 for K8s security policies
- **Read-only filesystem**: Application files are immutable
- **Security policies**: Integration with Pod Security Standards
- **Secret management**: Environment-based configuration for sensitive data

This comprehensive ZGC setup ensures optimal performance, reliability, and security for cloud Kubernetes deployments
while maintaining the precise SQL performance monitoring capabilities required by our application.

## Deployment Architecture Comparison: Native GraalVM vs Vanilla JVM

This project supports **two distinct deployment architectures**, each optimized for different use cases and cloud
deployment scenarios. Both configurations have been enhanced with multi-stage builds and cloud-native optimizations.

### 🚀 **Vanilla JVM Deployment** (Traditional Java Runtime)

#### **Docker Configuration**: `.docker/Dockerfile`

```bash
# Build for traditional JVM deployment
docker build -f .docker/Dockerfile -t store-jvm:2.0.0 .

# Or use Cloud Native Buildpacks
./gradlew bootBuildImageJvm
```

#### **Key Characteristics**

| Aspect                  | Vanilla JVM            | Benefits                             |
|:------------------------|:-----------------------|:-------------------------------------|
| **Startup Time**        | 15-45 seconds          | Acceptable for long-running services |
| **Memory Usage**        | 300-600MB+             | Full JVM feature set available       |
| **Image Size**          | ~152MB (optimized)     | Multi-stage build with custom JRE    |
| **Runtime Performance** | Excellent after warmup | JIT optimizations, profiling         |
| **Debugging**           | Full JVM tooling       | Complete observability stack         |
| **Compatibility**       | 100% Java ecosystem    | All libraries and frameworks         |

#### **Optimizations Implemented**

- **ZGC Garbage Collection**: Sub-millisecond pause times
- **Virtual Threads**: Enhanced concurrency (Java 21+)
- **Custom JRE**: jlink-optimized runtime (50% size reduction)
- **Container Awareness**: Memory and CPU quota integration
- **Multi-stage Build**: Dependency caching and layer optimization

#### **Cloud Buildpack Environment (Enhanced)**

```gradle
// Vanilla JVM with ZGC optimizations
environment = [
    'BPL_JVM_JGCOPTIONS': '-XX:+UseZGC -XX:MaxRAMPercentage=75.0',
    'BPL_JVM_OPTIONS': '--enable-preview -Djdk.virtualThreadScheduler.parallelism=16',
    'BP_IMAGE_LABELS': 'java.version=25,gc.collector=ZGC,architecture=vanilla-jvm'
]
```

### ⚡ **Native GraalVM Deployment** (Ahead-of-Time Compilation)

#### **Docker Configuration**: `.docker/Dockerfile.native`

```bash
# Build for GraalVM native deployment
docker build -f .docker/Dockerfile.native -t store-native:2.0.0 .

# Or use Cloud Native Buildpacks
./gradlew bootBuildImage  # Configured for native by default
```

#### **Key Characteristics**

| Aspect                  | Native GraalVM        | Benefits                          |
|:------------------------|:----------------------|:----------------------------------|
| **Startup Time**        | <100ms                | Instant startup for serverless    |
| **Memory Usage**        | 50-150MB              | Minimal memory footprint          |
| **Image Size**          | ~80MB (estimated)     | Ultra-lightweight containers      |
| **Runtime Performance** | Consistent, no warmup | Predictable performance           |
| **Cold Start**          | Excellent             | Ideal for autoscaling, serverless |
| **Resource Efficiency** | Superior              | Better pod density in K8s         |

#### **Optimizations Implemented**

- **Serial GC**: Optimal for native executables
- **AOT Compilation**: No JIT overhead, predictable performance
- **Static Linking**: Single executable with minimal dependencies
- **Container Optimizations**: Multi-stage build with Alpine base
- **Build-time Optimizations**: Enhanced compilation settings

#### **Cloud Buildpack Environment (Enhanced)**

```gradle
// Native GraalVM with enhanced build arguments
environment = [
    'BP_NATIVE_IMAGE_BUILD_ARGUMENTS': '--gc=serial -march=native -Ob --no-fallback',
    'BPL_JVM_JGCOPTIONS': '-XX:+UseZGC -Xmx14g -XX:MaxMetaspaceSize=3g', // Build JVM
    'BP_IMAGE_LABELS': 'java.version=25-native,gc.collector=Serial,architecture=native-graalvm'
]
```

### 📊 **Performance Comparison Matrix**

| Metric                    | Vanilla JVM (ZGC) | Native GraalVM | Winner    |
|:--------------------------|:------------------|:---------------|:----------|
| **Cold Start**            | 15-45s            | <100ms         | 🥇 Native |
| **Warm Performance**      | Excellent         | Good           | 🥇 JVM    |
| **Memory Footprint**      | 300-600MB         | 50-150MB       | 🥇 Native |
| **Image Size**            | ~152MB            | ~80MB          | 🥇 Native |
| **Build Time**            | 2-5 minutes       | 5-15 minutes   | 🥇 JVM    |
| **Debugging**             | Full tooling      | Limited        | 🥇 JVM    |
| **Library Compatibility** | 100%              | 95%+           | 🥇 JVM    |
| **Resource Cost**         | Higher            | Lower          | 🥇 Native |

### 🎯 **Use Case Recommendations**

#### **Choose Vanilla JVM When:**

- **Development & Testing**: Full debugging capabilities needed
- **Complex Applications**: Heavy use of reflection, dynamic proxies
- **Long-running Services**: Performance matters more than startup time
- **Full Java Ecosystem**: Need maximum library compatibility
- **Observability**: Require complete JVM monitoring and profiling

#### **Choose Native GraalVM When:**

- **Serverless Functions**: Cold start performance critical
- **Microservices**: Resource efficiency and cost optimization
- **Container Density**: Running many instances in K8s
- **Edge Computing**: Minimal resource environments
- **Fast Scaling**: Rapid horizontal scaling requirements

### 🔧 **Build Commands Summary**

#### **Development (Quick Iterations)**

```bash
# Traditional JVM - Fast builds, full debugging
./gradlew bootBuildImageJvm
docker run -p 8080:8080 store-jvm:2.0.0
```

#### **Production (Resource Efficiency)**

```bash
# Native GraalVM - Optimized for cloud deployment
./gradlew bootBuildImage
docker run -p 8080:8080 store-native:2.0.0
```

#### **Manual Docker Builds**

```bash
# Multi-stage JVM build with custom JRE
docker build -f .docker/Dockerfile -t store-jvm:latest .

# Multi-stage Native build with GraalVM optimization
docker build -f .docker/Dockerfile.native -t store-native:latest .
```

### 🚦 **Migration Strategy**

1. **Start with Vanilla JVM** for development and testing
2. **Profile and optimize** SQL performance monitoring
3. **Test Native GraalVM** compatibility with your workload
4. **Deploy Native** for production cost optimization
5. **Monitor and compare** real-world performance metrics

### ⚠️ **Important Considerations**

#### **Native GraalVM Limitations**

- Limited reflection support (mostly resolved with proper configuration)
- No dynamic class loading at runtime
- Longer build times (5-15 minutes vs 2-5 minutes)
- Some libraries may require additional native-image configuration

#### **SQL Performance Monitoring Compatibility**

Both deployment architectures fully support our comprehensive SQL performance monitoring stack:

- ✅ **@TrackSqlPerf annotations** work identically
- ✅ **Hypersistence Optimizer** (build-time analysis for native)
- ✅ **datasource-proxy** logging and monitoring
- ✅ **Prometheus metrics** collection
- ✅ **Grafana dashboards** and alerting

This dual-architecture approach provides maximum flexibility for different deployment scenarios while maintaining
consistent SQL performance monitoring capabilities across both deployment types.

## Spring Boot 4.0.0-M3 Upgrade

### Major Changes

- Requires Java 21+ (we're using Java 25)
- Built on Spring Framework 7.x
- Updated dependency management
- Enhanced native image support

### Configuration Updates

1. Update Spring Boot version in `build.gradle`:
   ```gradle
   id 'org.springframework.boot' version '4.0.0-M3'
   ```

2. Update dependency versions:
   ```gradle
   ext {
       springBootVersion = '4.0.0-M3'
       springDependencyManagementVersion = '1.1.7'
   }
   ```

### Plugin Status & Configuration

- All plugins are now enabled and compatible with the current setup:
  ```gradle
  id 'org.hibernate.orm' version '7.1.3.Final'  // Enabled
  id 'org.cyclonedx.bom' version '2.3.1'  // Enabled
  id 'com.diffplug.spotless' version '8.0.0'  // Enabled (with Java 25 compatibility adjustments)
  ```

### Migration Considerations

- Review deprecated APIs and update code accordingly
- Test thoroughly as this is a milestone release
- Update security configurations for Spring Security 7.x changes

## JUnit 6 Upgrade

### Major Changes from JUnit 5

- Enhanced test execution engine with better performance
- Improved parameterized testing capabilities
- Better integration with modern IDEs and build tools
- Support for Java 25 features

### Configuration

The project enforces JUnit 6 globally through dependency management:

```gradle
ext {
    junitVersion = '6.0.0'
}

configurations {
    all {
        resolutionStrategy.eachDependency { details ->
            // Block any JUnit Jupiter 5.x versions globally, force upgrade to JUnit 6
            if (details.requested.group == 'org.junit.jupiter' &&
                    details.requested.version.startsWith('5.')) {
                details.useVersion "${junitVersion}"
                details.because "Globally exclude JUnit Jupiter 5.x, force JUnit 6"
            }
        }
    }
}
```

## Testcontainers Integration

### Overview

The project includes Testcontainers for integration testing with real database instances:

- **PostgreSQL Testcontainers**: Automatic provisioning of PostgreSQL instances for tests
- **Docker Compose Integration**: Seamless integration with existing Docker setup
- **H2 In-Memory Database**: Fallback for lightweight unit tests

### Dependencies

```gradle
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
```

### Usage

Integration tests automatically spin up PostgreSQL containers as needed, ensuring test isolation and consistency across
environments.

## Gradle 9.0.0 Upgrade

### Prerequisites

- Java 11+ required (we're using Java 25)
- Review plugin compatibility

### Upgrade Steps

1. Update `gradle/wrapper/gradle-wrapper.properties`:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-9.0.0-bin.zip
   ```

2. Execute wrapper update command:
   ```bash
   ./gradlew wrapper --gradle-version 9.0.0 --distribution-type bin
   ```

### Performance Improvements

- Faster build times with improved incremental compilation
- Enhanced dependency resolution
- Improved configuration cache
- Slow SQL-Query Logging/Alerts

#### Updated Complete Stack Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│ COMPLETE JPA/SQL OPTIMIZATION TOOL STACK ACROSS ENVIRONMENTS        │
├─────────────────┬───────────────────────────────────────────────────┤
│ LOCAL DEV       │ • Hypersistence Optimizer (startup)               │
│                 │ • datasource-proxy (detailed logging)             │
│                 │ • @TrackSqlPerf (all annotated, verbose)          │
│                 │ • flexy-pool (pool metrics)                       │
│                 │ Overhead: ~8-10% (acceptable for dev)             │
├─────────────────┼───────────────────────────────────────────────────┤
│ TESTS           │ • QuickPerf (fail tests on violations)            │
│                 │ • Hypersistence Optimizer (entity validation)     │
│                 │ • datasource-proxy (query tracking)               │
│                 │ • @TrackSqlPerf (optional, can validate)          │
│                 │ • Testcontainers (real DB)                        │
│                 │ Overhead: N/A (test-only)                         │
├─────────────────┼───────────────────────────────────────────────────┤
│ DEV             │ • Hypersistence Optimizer (startup)               │
│                 │ • datasource-proxy (moderate logging)             │
│                 │ • @TrackSqlPerf (all annotated, warnings)         │
│                 │ • flexy-pool (pool metrics)                       │
│                 │ Overhead: ~6-8%                                   │
├─────────────────┼───────────────────────────────────────────────────┤
│ QA              │ • datasource-proxy (slow queries only)            │
│                 │ • @TrackSqlPerf (soft warnings)                   │
│                 │ • flexy-pool (pool metrics)                       │
│                 │ • QuickPerf (automated test suite)                │
│                 │ Overhead: ~4-6%                                   │
├─────────────────┼───────────────────────────────────────────────────┤
│ STAGE           │ • datasource-proxy (slow queries)                 │
│                 │ • @TrackSqlPerf (STRICT, fail-fast=true)          │
│                 │ • flexy-pool (pool metrics)                       │
│                 │ • QuickPerf (CI tests block deployment)           │
│                 │ Overhead: ~4-5%                                   │
│                 │ ⚠️ BLOCKS DEPLOYMENT ON VIOLATIONS                │
├─────────────────┼───────────────────────────────────────────────────┤
│ PROD            │ • datasource-proxy (critical slow queries only)   │
│                 │ • @TrackSqlPerf (5-10% critical ops, metrics)     │
│                 │ • flexy-pool (pool metrics)                       │
│                 │ Overhead: ~3-4%                                   │
│                 │ ✅ NEVER blocks, only monitors & alerts           │
└─────────────────┴───────────────────────────────────────────────────┘
```

## SQL Performance Monitoring - Complete Setup Guide

### Environment Configuration Overview

This project implements a comprehensive, environment-aware SQL performance monitoring stack that automatically adapts
based on your deployment environment. Each environment has been carefully tuned to balance monitoring visibility with
performance overhead.

### Manual Setup Instructions

#### 1. Local Development Environment

**Profile**: `application-local.yml`

To run with full monitoring (recommended for development):

```bash
# Start with local profile for maximum debugging visibility
./gradlew bootRun --args='--spring.profiles.active=local'

# Or set environment variable
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun
```

**What you get:**

- ✅ Hypersistence Optimizer startup validation
- ✅ Detailed SQL logging with 100ms slow query threshold
- ✅ @TrackSqlPerf enabled for all annotated methods
- ✅ Flexy-pool with JMX + log reporters
- ✅ Full debug logging for troubleshooting

#### 2. QA Environment

**Profile**: `application-qa.yml`

```bash
# QA environment with performance testing boundaries
./gradlew bootRun --args='--spring.profiles.active=qa'
```

**What you get:**

- ⚠️ Moderate monitoring with 300ms slow query threshold
- ✅ @TrackSqlPerf with soft warnings (no fail-fast)
- ✅ Flexy-pool connection monitoring
- ⚠️ QuickPerf available for automated test suite
- ❌ Hypersistence Optimizer disabled for performance

#### 3. Staging Environment (HARD GATE)

**Profile**: `application-stage.yml`

```bash
# Staging with strict performance gates
./gradlew bootRun --args='--spring.profiles.active=stage'
```

**What you get:**

- 🚫 STRICT mode with 250ms error threshold
- 🚫 @TrackSqlPerf with FAIL-FAST enabled (blocks deployment)
- 🚫 QuickPerf in CI tests (blocks deployment on SQL issues)
- ✅ Flexy-pool monitoring
- ❌ Hypersistence Optimizer disabled

#### 4. Production Environment

**Profile**: `application-live.yml` (default)

```bash
# Production with minimal overhead
./gradlew bootRun --args='--spring.profiles.active=live'
```

**What you get:**

- ✅ Critical operations monitoring only (500ms threshold)
- ✅ @TrackSqlPerf for critical=true operations only
- ✅ Flexy-pool connection monitoring
- ❌ All other tools disabled for minimal overhead

### Usage Examples and Best Practices

#### How to Mark Operations as Critical

```java

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    // ✅ GOOD: Critical operation monitored in all environments
    @TrackSqlPerf(value = "findCustomerByIdWithOrders",
            timeUnit = TimeUnit.MILLISECONDS,
            warnThreshold = 100,
            errorThreshold = 250,
            maxExpectedQueries = 1,
            critical = true,  // 🔥 This ensures monitoring in production
            metricTags = {"service=store", "operation=fetch-with-orders"}
    )
    @Query("from Customer c where c.id =:id")
    @EntityGraph(attributePaths = {"orders"})
    Optional<Customer> findCustomerByIdWithOrders(@Param("id") Long id);

    // ❌ BAD: Non-critical operation, won't be monitored in production
    @TrackSqlPerf(value = "findAllCustomers",
            critical = false  // Skip in production
    )
    List<Customer> findAll();
}
```

#### Environment-Specific Property Overrides

**Local Development (application-local.yml)**:

```yaml
sql-performance:
  tracking:
    enabled: true
    fail-fast-on-error: false
    detailed-logging: true
    warn-threshold-multiplier: 1.0
    error-threshold-multiplier: 1.0
```

**Staging Environment (application-stage.yml)**:

```yaml
sql-performance:
  tracking:
    enabled: true
    fail-fast-on-error: true  # BLOCK deployment on issues
    strict-mode: true
    error-threshold-multiplier: 0.8  # 20% stricter thresholds
```

**Production Environment (application-live.yml)**:

```yaml
sql-performance:
  tracking:
    enabled: true
    critical-operations-only: true  # Monitor only critical=true
    fail-fast-on-error: false  # Log errors, don't crash
```

### Potential Pitfalls and Troubleshooting

#### ⚠️ Common Issues and Solutions

##### 1. "No SQL monitoring in production"

**Problem**: Operations not being tracked in production environment.

**Solution**: Ensure operations are marked with `critical = true`:

```java
// ❌ Won't be monitored in production
@TrackSqlPerf(value = "someOperation")
public Optional<Customer> someOperation(Long id) { /* ... */ }

// ✅ Will be monitored in production  
@TrackSqlPerf(value = "someOperation", critical = true)
public Optional<Customer> someOperation(Long id) { /* ... */ }
```

##### 2. "Tests failing in staging due to strict thresholds"

**Problem**: Staging environment fails deployment due to performance gates.

**Solution**: This is **intentional**. Staging acts as a HARD GATE. Options:

- Optimize the slow query
- Adjust thresholds if legitimately needed
- Use `@ExpectSlowQuery` annotation for acceptable slow operations

##### 3. "Too much logging in development"

**Problem**: Development logs are overwhelming.

**Solution**: Adjust logging levels in `application-local.yml`:

```yaml
logging:
  level:
    com.example.store.aop.performance: INFO  # Reduce to INFO
    net.ttddyy.dsproxy.listener: WARN        # Reduce SQL proxy logging
```

##### 4. "HypersistenceOptimizer warnings on startup"

**Problem**: Seeing entity relationship warnings.

**Solution**: This is **intentional** in dev/test environments:

```java
// Fix the actual relationship issue
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)  // ✅ Use LAZY
    private List<Order> orders;

    // ✅ Add helper methods for bidirectional relationships
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }
}
```

##### 5. "Connection pool exhaustion"

**Problem**: Application running out of database connections.

**Solution**: Check flexy-pool metrics and tune accordingly:

```yaml
# Increase pool size if needed
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Increase from default
      leak-detection-threshold: 60000  # Detect connection leaks
```

### Performance Impact Analysis

#### Overhead by Environment

| Environment   | CPU Overhead | Memory Overhead | I/O Overhead                | Recommended Usage       |
|:--------------|:-------------|:----------------|:----------------------------|:------------------------|
| **LOCAL DEV** | ~8-12%       | ~15-20MB        | High (detailed logging)     | ✅ All features enabled  |
| **QA**        | ~4-6%        | ~8-10MB         | Medium (warn-level logging) | ⚠️ Selective monitoring |
| **STAGE**     | ~3-4%        | ~5-8MB          | Low (error-level only)      | 🚫 Strict gates only    |
| **PROD**      | ~1-2%        | ~2-3MB          | Minimal (critical errors)   | ✅ Critical path only    |

#### Tool-Specific Impact

```
📊 Performance Impact Breakdown (Production):

datasource-proxy (enabled):     ~1-2% CPU, ~1-2MB RAM
flexy-pool (enabled):          ~0.5% CPU, ~1MB RAM  
@TrackSqlPerf (critical only): ~0.5% CPU, ~0.5MB RAM
─────────────────────────────────────────────────────
Total Production Overhead:      ~2-3% CPU, ~2.5-3.5MB RAM

🚫 Disabled in Production:
HypersistenceOptimizer:         ~2-3% CPU, ~5-10MB RAM (saved)
QuickPerf:                      Test-only, 0% runtime impact
Full SQL logging:               ~3-5% CPU, ~10MB RAM (saved)
```

### Plugin Compatibility

- Some plugins may need updates for Gradle 9.0 compatibility
- Review and update plugin versions as needed
- Test all build tasks after upgrade

## Post-Upgrade Verification

### Build Verification

```bash
# Clean and build the project
./gradlew clean build

# Run tests
./gradlew test

# Build native image (if applicable)
./gradlew nativeCompile
```

### Docker Image Building

```bash
# Build JVM image with Java 25
./gradlew bootBuildImage --imageName=store-app

# Build native image
./gradlew bootBuildImage --imageName=store-app-native -Pnative
```

### Health Checks

1. Verify application startup
2. Check actuator endpoints
3. Test critical functionality
4. Monitor memory usage and performance

## Troubleshooting

### Common Issues

1. **Plugin Compatibility**: Some plugins may not support Gradle 9.0 yet
    - **Solution**: Use compatible versions or temporarily disable non-critical plugins

2. **Dependency Conflicts**: Spring Boot 4.x may have different dependency versions
    - **Solution**: Use `gradle dependencies` to identify conflicts and update accordingly

3. **Native Image Issues**: GraalVM compatibility with Spring Boot 4.x
    - **Solution**: Update GraalVM version and review native hints

### Rollback Strategy

If issues arise, you can rollback by reverting:

1. `gradle/wrapper/gradle-wrapper.properties`
2. Spring Boot version in `build.gradle`
3. Java version configuration

## Future Considerations

- Monitor Spring Boot 4.x stable release
- Re-enable disabled plugins when compatible versions are available
- Consider upgrading to newer Gradle versions as they become available
- Keep Java version updated with LTS releases
- Spotless apply for the auto-code formatting.

## Resources

- [Spring Boot 4.0 Migration Guide](https://docs.spring.io/spring-boot/docs/4.0.0-M2/reference/html/)
- [Gradle 9.0 Release Notes](https://docs.gradle.org/9.0/release-notes.html)
- [Java 25 Features](https://openjdk.org/projects/jdk/25/)