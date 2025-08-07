const {faker} = require('@faker-js/faker');

const N = 100; // Number of customers
const M = 10_000; // Number of orders
const P = 500; // Number of products
const PO_PER_ORDER = 3; // Average number of products per order

// Generate customers
for (let i = 1; i <= N; i++) {
    console.log(`INSERT INTO customer (id, name) VALUES (${i}, '${faker.name.fullName()}');`);
}

// Generate products
for (let i = 1; i <= P; i++) {
    const uuid = faker.datatype.uuid();
    console.log(`INSERT INTO product (id, description, sku)
                 VALUES (${i}, '${faker.commerce.productName()}', '${uuid}');`);
}

// Generate orders
for (let i = 1; i <= M; i++) {
    const customerId = Math.ceil(Math.random() * N);
    console.log(`INSERT INTO "order" (id, description, customer_id)
                 VALUES (${i}, '${faker.commerce.productDescription()}', ${customerId});`);
}

// Generate product_order entries (linking products to orders)
let poId = 1;
for (let orderId = 1; orderId <= M; orderId++) {
    // Determine how many products this order will have (1 to 5)
    const numProducts = Math.ceil(Math.random() * PO_PER_ORDER);

    // Create a set to track which products have been added to this order
    // to ensure we don't add the same product twice (due to unique constraint)
    const addedProducts = new Set();

    for (let j = 0; j < numProducts; j++) {
        // Get a random product ID that hasn't been added to this order yet
        let productId;
        do {
            productId = Math.ceil(Math.random() * P);
        } while (addedProducts.has(productId));

        addedProducts.add(productId);

        const quantity = Math.ceil(Math.random() * 5); // Random quantity between 1 and 5
        const price = (Math.random() * 100 + 10).toFixed(2); // Random price between 10 and 110

        console.log(`INSERT INTO product_order (id, order_id, product_id, quantity, price)
                     VALUES (${poId}, ${orderId}, ${productId}, ${quantity}, ${price});`);
        poId++;
    }
}
