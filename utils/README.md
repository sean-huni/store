# Data Generation Utility

This utility generates sample data for the store application. It creates SQL INSERT statements for customers, products,
orders, and product-order relationships. The script requires Node.js and npm to run.

## What It Generates

The script generates:

- Customers with random names
- Products with random names and UUIDs as SKUs
- Orders with random descriptions and customer assignments
- Product-order relationships with quantities and prices

## Configuration

You can adjust the following constants in the script:

- `N`: Number of customers (default: 100)
- `M`: Number of orders (default: 10,000)
- `P`: Number of products (default: 500)
- `PO_PER_ORDER`: Average number of products per order (default: 3)

## Installation

```shell
npm install
```

If you encounter issues with the npm registry, you may need to configure npm to use the public registry:

```shell
npm config set registry https://registry.npmjs.org/
npm install
```

If you're still experiencing connection issues, you can install the required package directly with the --registry flag:

```shell
npm install @faker-js/faker@^7.0.0 --registry=https://registry.npmjs.org/
```

## Execution

To generate the data and save it to a SQL file:

```shell
node ./generateData.js > ../src/main/resources/db/changelog/data.sql
```

To preview the generated data without saving:

```shell
node ./generateData.js | head -n 20
```

## Using the Generated Data

The generated SQL file can be used in several ways:

1. Directly execute it against your database
2. Include it in your Liquibase changelog
3. Use it for testing or development purposes

## Sample Output

The script generates SQL INSERT statements like these:

```sql
-- Customer data
INSERT INTO customer (id, name) VALUES (1, 'John Doe');
INSERT INTO customer (id, name) VALUES (2, 'Jane Smith');

-- Product data
INSERT INTO product (id, description, sku) VALUES (1, 'Ergonomic Concrete Shirt', '123e4567-e89b-12d3-a456-426614174000');
INSERT INTO product (id, description, sku) VALUES (2, 'Intelligent Cotton Gloves', '123e4567-e89b-12d3-a456-426614174001');

-- Order data
INSERT INTO "order" (id, description, customer_id) VALUES (1, 'The automobile layout consists of a front-engine design.', 42);
INSERT INTO "order" (id, description, customer_id) VALUES (2, 'The slim & simple design creates a comfortable driving experience.', 17);

-- Product-Order relationships
INSERT INTO product_order (id, order_id, product_id, quantity, price) VALUES (1, 1, 42, 3, 59.99);
INSERT INTO product_order (id, order_id, product_id, quantity, price) VALUES (2, 1, 128, 1, 24.50);
```

## Notes

- If you change the database schema, you'll need to update the script to match the new schema
- If you change the Liquibase migration, you'll need to update the Liquibase changelog or drop and recreate your
  database
