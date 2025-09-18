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

### With Custom Configuration

Add the following configuration to your `build.gradle` file:

```gradle
tasks.named('bootBuildImage') {
    builder = 'paketobuildpacks/builder-jammy-tiny'
    imageName = "${project.name}:${project.version}"
    environment = [
        'BP_JVM_VERSION': '25'
    ]
    
    // For native images, uncomment the following:
    // builder = 'paketobuildpacks/builder-jammy-tiny'
    // environment = [
    //     'BP_NATIVE_IMAGE': 'true',
    //     'BP_JVM_VERSION': '25'
    // ]
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
- **Spring Boot**: 4.0.0-M2 (Milestone release with Spring Framework 7.x)
- **Gradle**: 9.0.0 (Latest stable release with improved performance)

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

## Spring Boot 4.0.0-M2 Upgrade

### Major Changes

- Requires Java 21+ (we're using Java 25)
- Built on Spring Framework 7.x
- Updated dependency management
- Enhanced native image support

### Configuration Updates

1. Update Spring Boot version in `build.gradle`:
   ```gradle
   id 'org.springframework.boot' version '4.0.0-M2'
   ```

2. Update dependency versions:
   ```gradle
   ext {
       springBootVersion = '4.0.0-M2'
       springDependencyManagementVersion = '1.1.7'
   }
   ```

### Known Issues & Workarounds

- Some plugins temporarily disabled for compatibility:
  ```gradle
  // id 'org.hibernate.orm' version '7.1.0.Final'  // Temporarily disabled
  // id 'org.cyclonedx.bom' version '1.11.0'  // Temporarily disabled
  ```

### Migration Considerations

- Review deprecated APIs and update code accordingly
- Test thoroughly as this is a milestone release
- Update security configurations for Spring Security 7.x changes

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
- Better Kotlin DSL support
- Improved configuration cache

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

## Resources

- [Spring Boot 4.0 Migration Guide](https://docs.spring.io/spring-boot/docs/4.0.0-M2/reference/html/)
- [Gradle 9.0 Release Notes](https://docs.gradle.org/9.0/release-notes.html)
- [Java 25 Features](https://openjdk.org/projects/jdk/25/)