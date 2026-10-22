package com.warehouse.management.service;

import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service

public class WarehouseService {

    private final JdbcTemplate db;

    public WarehouseService(JdbcTemplate db) {

        this.db = db;

    }

    private int i(Object v) {

        if (v == null || String.valueOf(v).isBlank()) return 0;

        return Integer.parseInt(String.valueOf(v));

    }

    private long l(Object v) {

        if (v == null || String.valueOf(v).isBlank()) return 0L;

        return Long.parseLong(String.valueOf(v));

    }

    private double d(Object v) {

        if (v == null || String.valueOf(v).isBlank()) return 0D;

        return Double.parseDouble(String.valueOf(v));

    }

    private String s(Object v) {

        return v == null ? "" : String.valueOf(v);

    }

    // =========================================================

    // LOGIN - UPDATED

    // =========================================================

    public Map<String, Object> login(Map<String, Object> body) {

        String username = s(body.get("username")).trim();

        String password = s(body.get("password")).trim();

        String role = s(body.get("role")).trim().toUpperCase();

        if (username.isBlank() || password.isBlank() || role.isBlank()) {

            throw new IllegalArgumentException(

                    "Username, password and role are required"

            );

        }

        try {

            Map<String, Object> user = db.queryForMap(

                    """

                    SELECT USER_ID, USERNAME, ROLE

                    FROM USERS

                    WHERE UPPER(TRIM(USERNAME)) = UPPER(TRIM(?))

                      AND TRIM(PASSWORD) = TRIM(?)

                      AND UPPER(TRIM(ROLE)) = UPPER(TRIM(?))

                    """,

                    username,

                    password,

                    role

            );

            Map<String, Object> out = new LinkedHashMap<>();

            out.put("userId", l(user.get("USER_ID")));

            out.put("username", s(user.get("USERNAME")));

            out.put("role", s(user.get("ROLE")).trim().toUpperCase());

            if ("ADMIN".equalsIgnoreCase(s(user.get("ROLE")))) {

                out.put("displayRole", "Warehouse Manager");

            } else {

                out.put("displayRole", "Staff");

            }

            out.put("status", "ACTIVE");

            return out;

        } catch (EmptyResultDataAccessException e) {

            throw new IllegalArgumentException(

                    "Invalid username, password or role"

            );

        }

    }

    // =========================================================

    // PRODUCTS

    // =========================================================

    public List<Map<String, Object>> products() {

        return db.queryForList(

                """

                SELECT

                    p.PRODUCT_ID,

                    p.PRODUCT_NAME,

                    p.CATEGORY,

                    p.PRICE,

                    NVL(p.QUANTITY,0) STOCK,

                    p.SUPPLIER_ID,

                    NVL(i.AVAILABLE_STOCK,NVL(p.QUANTITY,0)) AVAILABLE_STOCK,

                    NVL(i.REORDER_LEVEL,0) REORDER_LEVEL,

                    NVL(i.REORDER_QUANTITY,0) REORDER_QUANTITY,

                    CASE

                        WHEN NVL(i.AVAILABLE_STOCK,NVL(p.QUANTITY,0))

                             <= NVL(i.REORDER_LEVEL,0)

                        THEN 'LOW STOCK'

                        ELSE 'IN STOCK'

                    END STATUS

                FROM PRODUCTS p

                LEFT JOIN INVENTORY i

                    ON i.PRODUCT_ID = p.PRODUCT_ID

                ORDER BY p.PRODUCT_ID

                """

        );

    }

    @Transactional

    public Map<String, Object> createProduct(Map<String, Object> x) {

        String id = s(x.get("productId"));

        String name = s(x.get("productName"));

        String supplier = s(x.get("supplierId"));

        if (id.isBlank() || name.isBlank()) {

            throw new IllegalArgumentException(

                    "Product ID and product name are required"

            );

        }

        int stock = i(x.get("stock"));

        int level = i(x.get("reorderLevel"));

        int reorder = i(x.get("reorderQuantity"));

        db.update(

                """

                INSERT INTO PRODUCTS

                (

                    PRODUCT_ID,

                    PRODUCT_NAME,

                    CATEGORY,

                    PRICE,

                    QUANTITY,

                    SUPPLIER_ID

                )

                VALUES (?,?,?,?,?,?)

                """,

                id,

                name,

                s(x.get("category")),

                d(x.get("price")),

                stock,

                supplier.isBlank() ? null : supplier

        );

        int invId = db.queryForObject(

                "SELECT NVL(MAX(INVENTORY_ID),0)+1 FROM INVENTORY",

                Integer.class

        );

        db.update(

                """

                INSERT INTO INVENTORY

                (

                    INVENTORY_ID,

                    PRODUCT_ID,

                    STOCK_IN,

                    STOCK_OUT,

                    AVAILABLE_STOCK,

                    LAST_UPDATED,

                    REORDER_LEVEL,

                    REORDER_QUANTITY,

                    STATUS

                )

                VALUES (?,?,?,?,?,SYSDATE,?,?,?)

                """,

                invId,

                id,

                stock,

                0,

                stock,

                level,

                reorder,

                stock <= level ? "LOW STOCK" : "IN STOCK"

        );

        return productById(id);

    }

    @Transactional

    public Map<String, Object> updateProduct(

            String id,

            Map<String, Object> x

    ) {

        if (db.queryForObject(

                "SELECT COUNT(*) FROM PRODUCTS WHERE PRODUCT_ID=?",

                Integer.class,

                id

        ) == 0) {

            throw new NoSuchElementException("Product not found");

        }

        int stock = i(x.get("stock"));

        int level = i(x.get("reorderLevel"));

        int reorder = i(x.get("reorderQuantity"));

        db.update(

                """

                UPDATE PRODUCTS

                SET

                    PRODUCT_NAME=?,

                    CATEGORY=?,

                    PRICE=?,

                    QUANTITY=?,

                    SUPPLIER_ID=?

                WHERE PRODUCT_ID=?

                """,

                s(x.get("productName")),

                s(x.get("category")),

                d(x.get("price")),

                stock,

                s(x.get("supplierId")).isBlank()

                        ? null

                        : s(x.get("supplierId")),

                id

        );

        int count = db.queryForObject(

                "SELECT COUNT(*) FROM INVENTORY WHERE PRODUCT_ID=?",

                Integer.class,

                id

        );

        if (count == 0) {

            int invId = db.queryForObject(

                    "SELECT NVL(MAX(INVENTORY_ID),0)+1 FROM INVENTORY",

                    Integer.class

            );

            db.update(

                    """

                    INSERT INTO INVENTORY

                    (

                        INVENTORY_ID,

                        PRODUCT_ID,

                        STOCK_IN,

                        STOCK_OUT,

                        AVAILABLE_STOCK,

                        LAST_UPDATED,

                        REORDER_LEVEL,

                        REORDER_QUANTITY,

                        STATUS

                    )

                    VALUES (?,?,?,?,?,SYSDATE,?,?,?)

                    """,

                    invId,

                    id,

                    stock,

                    0,

                    stock,

                    level,

                    reorder,

                    stock <= level ? "LOW STOCK" : "IN STOCK"

            );

        } else {

            db.update(

                    """

                    UPDATE INVENTORY

                    SET

                        AVAILABLE_STOCK=?,

                        REORDER_LEVEL=?,

                        REORDER_QUANTITY=?,

                        STATUS=?,

                        LAST_UPDATED=SYSDATE

                    WHERE PRODUCT_ID=?

                    """,

                    stock,

                    level,

                    reorder,

                    stock <= level ? "LOW STOCK" : "IN STOCK",

                    id

            );

        }

        return productById(id);

    }

    @Transactional

    public void deleteProduct(String id) {

        db.update(

                "DELETE FROM ORDER_ITEMS WHERE PRODUCT_ID=?",

                id

        );

        db.update(

                "DELETE FROM INVENTORY WHERE PRODUCT_ID=?",

                id

        );

        db.update(

                "DELETE FROM PRODUCTS WHERE PRODUCT_ID=?",

                id

        );

    }

    private Map<String, Object> productById(String id) {

        return db.queryForMap(

                """

                SELECT

                    p.PRODUCT_ID,

                    p.PRODUCT_NAME,

                    p.CATEGORY,

                    p.PRICE,

                    NVL(p.QUANTITY,0) STOCK,

                    p.SUPPLIER_ID,

                    NVL(i.AVAILABLE_STOCK,NVL(p.QUANTITY,0)) AVAILABLE_STOCK,

                    NVL(i.REORDER_LEVEL,0) REORDER_LEVEL,

                    NVL(i.REORDER_QUANTITY,0) REORDER_QUANTITY,

                    CASE

                        WHEN NVL(i.AVAILABLE_STOCK,NVL(p.QUANTITY,0))

                             <= NVL(i.REORDER_LEVEL,0)

                        THEN 'LOW STOCK'

                        ELSE 'IN STOCK'

                    END STATUS

                FROM PRODUCTS p

                LEFT JOIN INVENTORY i

                    ON i.PRODUCT_ID=p.PRODUCT_ID

                WHERE p.PRODUCT_ID=?

                """,

                id

        );

    }

    // =========================================================

    // SUPPLIERS

    // =========================================================

    public List<Map<String, Object>> suppliers() {

        return db.queryForList(

                """

                SELECT

                    SUPPLIER_ID,

                    SUPPLIER_NAME,

                    PHONE,

                    EMAIL,

                    ADDRESS,

                    'ACTIVE' STATUS

                FROM SUPPLIERS

                ORDER BY SUPPLIER_NAME

                """

        );

    }

    @Transactional

    public Map<String, Object> createSupplier(

            Map<String, Object> x

    ) {

        String id = s(x.get("supplierId"));

        if (id.isBlank()) {

            id = "SUP-" + String.format(

                    "%03d",

                    db.queryForObject(

                            """

                            SELECT NVL(

                                MAX(

                                    TO_NUMBER(

                                        REGEXP_SUBSTR(

                                            SUPPLIER_ID,

                                            '[0-9]+$'

                                        )

                                    )

                                ),0)+1

                            FROM SUPPLIERS

                            """,

                            Integer.class

                    )

            );

        }

        db.update(

                """

                INSERT INTO SUPPLIERS

                (

                    SUPPLIER_ID,

                    SUPPLIER_NAME,

                    PHONE,

                    EMAIL,

                    ADDRESS

                )

                VALUES (?,?,?,?,?)

                """,

                id,

                s(x.get("supplierName")),

                s(x.get("phone")),

                s(x.get("email")),

                s(x.get("address"))

        );

        return db.queryForMap(

                """

                SELECT

                    SUPPLIER_ID,

                    SUPPLIER_NAME,

                    PHONE,

                    EMAIL,

                    ADDRESS,

                    'ACTIVE' STATUS

                FROM SUPPLIERS

                WHERE SUPPLIER_ID=?

                """,

                id

        );

    }

    @Transactional

    public Map<String, Object> updateSupplier(

            String id,

            Map<String, Object> x

    ) {

        db.update(

                """

                UPDATE SUPPLIERS

                SET

                    SUPPLIER_NAME=?,

                    PHONE=?,

                    EMAIL=?,

                    ADDRESS=?

                WHERE SUPPLIER_ID=?

                """,

                s(x.get("supplierName")),

                s(x.get("phone")),

                s(x.get("email")),

                s(x.get("address")),

                id

        );

        return db.queryForMap(

                """

                SELECT

                    SUPPLIER_ID,

                    SUPPLIER_NAME,

                    PHONE,

                    EMAIL,

                    ADDRESS,

                    'ACTIVE' STATUS

                FROM SUPPLIERS

                WHERE SUPPLIER_ID=?

                """,

                id

        );

    }

    @Transactional

    public void deleteSupplier(String id) {

        db.update(

                "UPDATE PRODUCTS SET SUPPLIER_ID=NULL WHERE SUPPLIER_ID=?",

                id

        );

        db.update(

                "DELETE FROM SUPPLIERS WHERE SUPPLIER_ID=?",

                id

        );

    }

    // =========================================================

    // INVENTORY

    // =========================================================

    public List<Map<String, Object>> inventory() {

        return db.queryForList(

                """

                SELECT

                    i.PRODUCT_ID,

                    p.PRODUCT_NAME,

                    i.STOCK_IN,

                    i.STOCK_OUT,

                    i.AVAILABLE_STOCK,

                    i.REORDER_LEVEL,

                    i.REORDER_QUANTITY,

                    i.STATUS,

                    TO_CHAR(

                        i.LAST_UPDATED,

                        'DD Mon YYYY HH24:MI'

                    ) LAST_UPDATED

                FROM INVENTORY i

                JOIN PRODUCTS p

                    ON p.PRODUCT_ID=i.PRODUCT_ID

                ORDER BY p.PRODUCT_NAME

                """

        );

    }

    @Transactional

    public Map<String, Object> updateInventory(

            String id,

            Map<String, Object> x

    ) {

        int stock = i(x.get("availableStock"));

        int level = i(x.get("reorderLevel"));

        int reorder = i(x.get("reorderQuantity"));

        db.update(

                """

                UPDATE INVENTORY

                SET

                    AVAILABLE_STOCK=?,

                    REORDER_LEVEL=?,

                    REORDER_QUANTITY=?,

                    STATUS=?,

                    LAST_UPDATED=SYSDATE

                WHERE PRODUCT_ID=?

                """,

                stock,

                level,

                reorder,

                stock <= level ? "LOW STOCK" : "IN STOCK",

                id

        );

        db.update(

                "UPDATE PRODUCTS SET QUANTITY=? WHERE PRODUCT_ID=?",

                stock,

                id

        );

        return db.queryForMap(

                """

                SELECT

                    PRODUCT_ID,

                    AVAILABLE_STOCK,

                    REORDER_LEVEL,

                    REORDER_QUANTITY,

                    STATUS

                FROM INVENTORY

                WHERE PRODUCT_ID=?

                """,

                id

        );

    }

    // =========================================================

    // RECEIVING

    // =========================================================

    public List<Map<String, Object>> receiving() {
        return db.queryForList(
            """
            SELECT
                r.RECEIVING_ID,
                r.PRODUCT_ID,
                p.PRODUCT_NAME,
                r.WAREHOUSE_ID,
                r.LOCATION_ID,
                p.SUPPLIER_ID,
                s.SUPPLIER_NAME,
                r.RECEIVED_QUANTITY,
                TO_CHAR(r.RECEIVED_DATE, 'DD Mon YYYY') AS RECEIVED_DATE,
                r.PUTAWAY_STATUS AS STATUS,
                l.LOCATION_CODE
            FROM STOCK_RECEIVING r
            JOIN PRODUCTS p ON p.PRODUCT_ID = r.PRODUCT_ID
            LEFT JOIN SUPPLIERS s ON s.SUPPLIER_ID = p.SUPPLIER_ID
            LEFT JOIN WAREHOUSE_LOCATIONS l ON l.LOCATION_ID = r.LOCATION_ID
            ORDER BY r.RECEIVING_ID DESC
            """
        );
    }

    @Transactional
    public Map<String, Object> createReceiving(Map<String, Object> x) {
        String productId = s(x.get("productId")).trim();
        int qty = i(x.get("receivedQuantity"));
        int warehouseId = i(x.get("warehouseId"));
        int locationId = i(x.get("locationId"));
        String receivedDate = s(x.get("receivedDate")).trim();
        if (receivedDate.isBlank()) receivedDate = java.time.LocalDate.now().toString();
        if (!receivedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Received date must use YYYY-MM-DD format");
        }

        if (productId.isBlank() || qty <= 0) {
            throw new IllegalArgumentException("Select a product and enter a positive quantity");
        }
        if (warehouseId <= 0 || locationId <= 0) {
            throw new IllegalArgumentException("A valid warehouse and putaway location are required");
        }

        Integer productCount = db.queryForObject(
            "SELECT COUNT(*) FROM PRODUCTS WHERE PRODUCT_ID = ?", Integer.class, productId
        );
        if (productCount == null || productCount == 0) {
            throw new IllegalArgumentException("Selected product does not exist");
        }

        String selectedSupplierId = s(x.get("supplierId")).trim();
        Map<String, Object> productSupplier = db.queryForMap(
            "SELECT SUPPLIER_ID FROM PRODUCTS WHERE PRODUCT_ID = ?",
            productId
        );
        String actualSupplierId = s(productSupplier.get("SUPPLIER_ID")).trim();
        if (!actualSupplierId.isBlank() && !selectedSupplierId.isBlank()
                && !actualSupplierId.equals(selectedSupplierId)) {
            throw new IllegalArgumentException(
                "The selected supplier does not match the supplier assigned to this product"
            );
        }

        Integer inventoryCount = db.queryForObject(
            "SELECT COUNT(*) FROM INVENTORY WHERE PRODUCT_ID = ?", Integer.class, productId
        );
        if (inventoryCount == null || inventoryCount == 0) {
            throw new IllegalArgumentException("Inventory row is missing for product " + productId);
        }

        Map<String, Object> location;
        try {
            location = db.queryForMap(
                """
                SELECT LOCATION_ID, WAREHOUSE_ID, NVL(CAPACITY, 0) CAPACITY,
                       NVL(USED_CAPACITY, 0) USED_CAPACITY,
                       NVL(AVAILABLE_CAPACITY, NVL(CAPACITY, 0) - NVL(USED_CAPACITY, 0)) AVAILABLE_CAPACITY
                FROM WAREHOUSE_LOCATIONS
                WHERE LOCATION_ID = ? AND WAREHOUSE_ID = ?
                """,
                locationId, warehouseId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Selected location does not belong to the selected warehouse");
        }
        int availableCapacity = i(location.get("AVAILABLE_CAPACITY"));
        if (availableCapacity < qty) {
            throw new IllegalArgumentException(
                "Insufficient available capacity at this location. Available: " + availableCapacity
            );
        }

        // Create a pending receiving record only. Do not post stock until putaway is completed.
        int receivingId = db.queryForObject(
            "SELECT NVL(MAX(RECEIVING_ID), 0) + 1 FROM STOCK_RECEIVING",
            Integer.class
        );

        db.update(
            """
            INSERT INTO STOCK_RECEIVING
                (RECEIVING_ID, PRODUCT_ID, WAREHOUSE_ID, LOCATION_ID,
                 RECEIVED_QUANTITY, RECEIVED_DATE, PUTAWAY_STATUS)
            VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), 'IN PROGRESS')
            """,
            receivingId, productId, warehouseId, locationId, qty, receivedDate
        );

        return db.queryForMap(
            """
            SELECT RECEIVING_ID, PRODUCT_ID, WAREHOUSE_ID, LOCATION_ID,
                   RECEIVED_QUANTITY, TO_CHAR(RECEIVED_DATE, 'DD Mon YYYY') AS RECEIVED_DATE,
                   PUTAWAY_STATUS AS STATUS
            FROM STOCK_RECEIVING
            WHERE RECEIVING_ID = ?
            """,
            receivingId
        );
    }

    @Transactional
    public Map<String, Object> completePutaway(int receivingId) {
        Map<String, Object> receiving;
        try {
            receiving = db.queryForMap(
                """
                SELECT RECEIVING_ID, PRODUCT_ID, WAREHOUSE_ID, LOCATION_ID,
                       RECEIVED_QUANTITY, PUTAWAY_STATUS
                FROM STOCK_RECEIVING
                WHERE RECEIVING_ID = ?
                FOR UPDATE
                """,
                receivingId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Receiving record " + receivingId + " was not found");
        }

        String status = s(receiving.get("PUTAWAY_STATUS")).trim().toUpperCase().replace('_', ' ');
        if ("COMPLETED".equals(status) || "PUTAWAY COMPLETED".equals(status)) {
            throw new IllegalArgumentException("Putaway is already completed; inventory was not changed again");
        }
        if (!"IN PROGRESS".equals(status)) {
            throw new IllegalArgumentException("Only IN PROGRESS receiving records can be completed");
        }

        String productId = s(receiving.get("PRODUCT_ID"));
        int qty = i(receiving.get("RECEIVED_QUANTITY"));
        int locationId = i(receiving.get("LOCATION_ID"));

        if (qty <= 0) {
            throw new IllegalArgumentException("Receiving quantity must be positive");
        }

        Map<String, Object> location;
        try {
            location = db.queryForMap(
                """
                SELECT LOCATION_ID,
                       NVL(CAPACITY, 0) CAPACITY,
                       NVL(USED_CAPACITY, 0) USED_CAPACITY,
                       NVL(AVAILABLE_CAPACITY, NVL(CAPACITY, 0) - NVL(USED_CAPACITY, 0)) AVAILABLE_CAPACITY
                FROM WAREHOUSE_LOCATIONS
                WHERE LOCATION_ID = ?
                FOR UPDATE
                """,
                locationId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Putaway location no longer exists");
        }
        int availableCapacity = i(location.get("AVAILABLE_CAPACITY"));
        if (availableCapacity < qty) {
            throw new IllegalArgumentException(
                "Insufficient available location capacity. Available: " + availableCapacity
            );
        }

        int inventoryUpdated = db.update(
            """
            UPDATE INVENTORY
            SET STOCK_IN = NVL(STOCK_IN, 0) + ?,
                AVAILABLE_STOCK = NVL(AVAILABLE_STOCK, 0) + ?,
                LAST_UPDATED = SYSDATE,
                STATUS = CASE
                    WHEN NVL(AVAILABLE_STOCK, 0) + ? <= NVL(REORDER_LEVEL, 0)
                    THEN 'LOW STOCK' ELSE 'IN STOCK'
                END
            WHERE PRODUCT_ID = ?
            """,
            qty, qty, qty, productId
        );
        if (inventoryUpdated != 1) {
            throw new IllegalStateException("Inventory could not be updated for product " + productId);
        }

        int productUpdated = db.update(
            "UPDATE PRODUCTS SET QUANTITY = NVL(QUANTITY, 0) + ? WHERE PRODUCT_ID = ?",
            qty, productId
        );
        if (productUpdated != 1) {
            throw new IllegalStateException("Product quantity could not be updated for " + productId);
        }

        int locationUpdated = db.update(
            """
            UPDATE WAREHOUSE_LOCATIONS
            SET USED_CAPACITY = NVL(USED_CAPACITY, 0) + ?,
                AVAILABLE_CAPACITY = GREATEST(
                    0, NVL(CAPACITY, 0) - (NVL(USED_CAPACITY, 0) + ?)
                )
            WHERE LOCATION_ID = ?
            """,
            qty, qty, locationId
        );
        if (locationUpdated != 1) {
            throw new IllegalStateException("Warehouse location capacity could not be updated");
        }

        db.update(
            "UPDATE STOCK_RECEIVING SET PUTAWAY_STATUS = 'COMPLETED' WHERE RECEIVING_ID = ?",
            receivingId
        );

        return db.queryForMap(
            """
            SELECT RECEIVING_ID, PRODUCT_ID, WAREHOUSE_ID, LOCATION_ID,
                   RECEIVED_QUANTITY, PUTAWAY_STATUS AS STATUS
            FROM STOCK_RECEIVING
            WHERE RECEIVING_ID = ?
            """,
            receivingId
        );
    }

    // =========================================================

    // ORDERS

    // =========================================================

    public List<Map<String, Object>> orders() {
        return db.queryForList(
                """
                SELECT
                    o.ORDER_ID,
                    o.CUSTOMER_NAME,
                    oi.PRODUCT_ID,
                    p.PRODUCT_NAME,
                    NVL(oi.QUANTITY, 0) AS QUANTITY,
                    o.ORDER_STATUS,
                    o.PICKING_STATUS,
                    o.PACKING_STATUS,
                    TO_CHAR(o.ORDER_DATE, 'DD Mon YYYY') AS ORDER_DATE,
                    o.TOTAL_AMOUNT
                FROM ORDERS o
                LEFT JOIN ORDER_ITEMS oi
                    ON oi.ORDER_ID = o.ORDER_ID
                LEFT JOIN PRODUCTS p
                    ON p.PRODUCT_ID = oi.PRODUCT_ID
                ORDER BY o.ORDER_ID DESC
                """
        );
    }

    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> x) {
        String customerName = s(x.get("customerName")).trim();
        String productId = s(x.get("productId")).trim();
        int quantity = i(x.get("quantity"));

        if (customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (productId.isBlank() || quantity <= 0) {
            throw new IllegalArgumentException("Select a product and enter a positive quantity");
        }

        Map<String, Object> product;
        try {
            product = db.queryForMap(
                "SELECT PRODUCT_ID, PRODUCT_NAME, PRICE FROM PRODUCTS WHERE PRODUCT_ID = ?",
                productId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        Integer availableStock;
        try {
            availableStock = db.queryForObject(
                "SELECT NVL(AVAILABLE_STOCK, 0) FROM INVENTORY WHERE PRODUCT_ID = ? FOR UPDATE",
                Integer.class,
                productId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Inventory record not found for product: " + productId);
        }
        if (availableStock == null || availableStock < quantity) {
            throw new IllegalArgumentException(
                "Insufficient stock. Available: " + (availableStock == null ? 0 : availableStock)
            );
        }

        double price = d(product.get("PRICE"));
        double totalAmount = price * quantity;

        // IDs follow the existing schema convention. Use database sequences for concurrent production traffic.
        Integer orderId = db.queryForObject(
            "SELECT NVL(MAX(ORDER_ID), 1000) + 1 FROM ORDERS",
            Integer.class
        );
        Integer itemId = db.queryForObject(
            "SELECT NVL(MAX(ORDER_ITEM_ID), 0) + 1 FROM ORDER_ITEMS",
            Integer.class
        );

        db.update(
            """
            INSERT INTO ORDERS (
                ORDER_ID, CUSTOMER_NAME, ORDER_DATE, TOTAL_AMOUNT,
                ORDER_STATUS, PICKING_STATUS, PACKING_STATUS
            ) VALUES (?, ?, SYSDATE, ?, 'PENDING', 'PENDING', 'PENDING')
            """,
            orderId, customerName, totalAmount
        );

        db.update(
            """
            INSERT INTO ORDER_ITEMS (
                ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID, QUANTITY,
                PICKED_QUANTITY, PACKED_QUANTITY
            ) VALUES (?, ?, ?, ?, 0, 0)
            """,
            itemId, orderId, productId, quantity
        );

        return db.queryForMap(
            """
            SELECT O.ORDER_ID, O.CUSTOMER_NAME, O.ORDER_DATE, O.TOTAL_AMOUNT,
                   O.ORDER_STATUS, O.PICKING_STATUS, O.PACKING_STATUS,
                   I.PRODUCT_ID, P.PRODUCT_NAME, I.QUANTITY
            FROM ORDERS O
            JOIN ORDER_ITEMS I ON I.ORDER_ID = O.ORDER_ID
            JOIN PRODUCTS P ON P.PRODUCT_ID = I.PRODUCT_ID
            WHERE O.ORDER_ID = ?
            """,
            orderId
        );
    }

    @Transactional

    public Map<String, Object> updateOrder(

            long id,

            Map<String, Object> x

    ) {

        Map<String, Object> old;

        try {

            old = db.queryForMap(

                    """

                    SELECT ORDER_STATUS, PICKING_STATUS, PACKING_STATUS

                    FROM ORDERS

                    WHERE ORDER_ID = ?

                    """,

                    id

            );

        } catch (EmptyResultDataAccessException e) {

            throw new NoSuchElementException("Order not found: " + id);

        }

        String oldStatus = s(old.get("ORDER_STATUS")).trim().toUpperCase();

        String newStatus = x.containsKey("orderStatus")

                ? s(x.get("orderStatus")).trim().toUpperCase() : oldStatus;

        String pickingStatus = x.containsKey("pickingStatus")

                ? s(x.get("pickingStatus")).trim().toUpperCase()

                : s(old.get("PICKING_STATUS")).trim().toUpperCase();

        String packingStatus = x.containsKey("packingStatus")

                ? s(x.get("packingStatus")).trim().toUpperCase()

                : s(old.get("PACKING_STATUS")).trim().toUpperCase();

        if (!List.of("PENDING", "PICKING", "PACKING", "SHIPPED", "COMPLETED", "CANCELLED").contains(newStatus)) {

            throw new IllegalArgumentException("Invalid order status: " + newStatus);

        }

        db.update(

                """

                UPDATE ORDERS

                SET ORDER_STATUS = ?, PICKING_STATUS = ?, PACKING_STATUS = ?

                WHERE ORDER_ID = ?

                """,

                newStatus, pickingStatus, packingStatus, id

        );

        // Deduct stock only on the first transition to SHIPPED.

        if ("SHIPPED".equals(newStatus) && !"SHIPPED".equals(oldStatus)) {

            List<Map<String, Object>> items = db.queryForList(

                    "SELECT PRODUCT_ID, QUANTITY FROM ORDER_ITEMS WHERE ORDER_ID = ?", id

            );

            for (Map<String, Object> item : items) {

                String productId = s(item.get("PRODUCT_ID"));

                int qty = i(item.get("QUANTITY"));

                int inventoryUpdated = db.update(

                        """

                        UPDATE INVENTORY

                        SET STOCK_OUT = NVL(STOCK_OUT, 0) + ?,

                            AVAILABLE_STOCK = GREATEST(0, NVL(AVAILABLE_STOCK, 0) - ?),

                            STATUS = CASE

                                WHEN GREATEST(0, NVL(AVAILABLE_STOCK, 0) - ?) <= NVL(REORDER_LEVEL, 0)

                                THEN 'LOW STOCK' ELSE 'IN STOCK' END,

                            LAST_UPDATED = SYSDATE

                        WHERE PRODUCT_ID = ?

                        """,

                        qty, qty, qty, productId

                );

                if (inventoryUpdated > 0) {

                    db.update(

                            "UPDATE PRODUCTS SET QUANTITY = GREATEST(0, NVL(QUANTITY, 0) - ?) WHERE PRODUCT_ID = ?",

                            qty, productId

                    );

                }

            }

        }

        return orders().stream()

                .filter(o -> l(o.get("ORDER_ID")) == id)

                .findFirst()

                .orElseThrow(() -> new NoSuchElementException("Updated order not found"));

    }

// =========================================================

    // WAREHOUSE LOCATIONS

    // =========================================================

    public List<Map<String, Object>> locations() {

        return db.queryForList(

                """

                SELECT

                    LOCATION_ID,

                    WAREHOUSE_ID,

                    LOCATION_CODE CODE,

                    REGEXP_SUBSTR(

                        LOCATION_CODE,

                        '^[A-Z]+'

                    ) ZONE_PREFIX,

                    LOCATION_TYPE TYPE,

                    NVL(CAPACITY,0) CAPACITY,

                    NVL(USED_CAPACITY,0) USED_CAPACITY,

                    NVL(

                        AVAILABLE_CAPACITY,

                        NVL(CAPACITY,0)-NVL(USED_CAPACITY,0)

                    ) AVAILABLE_CAPACITY

                FROM WAREHOUSE_LOCATIONS

                ORDER BY LOCATION_ID

                """

        );

    }

    @Transactional

    public Map<String, Object> createLocation(

            Map<String, Object> x

    ) {

        int id = db.queryForObject(

                "SELECT NVL(MAX(LOCATION_ID),0)+1 FROM WAREHOUSE_LOCATIONS",

                Integer.class

        );

        int wh = i(x.get("warehouseId"));

        if (wh == 0) wh = 1;

        int cap = i(x.get("capacity"));

        int used = i(x.get("usedCapacity"));

        db.update(

                """

                INSERT INTO WAREHOUSE_LOCATIONS

                (

                    LOCATION_ID,

                    WAREHOUSE_ID,

                    LOCATION_CODE,

                    LOCATION_TYPE,

                    CAPACITY,

                    USED_CAPACITY,

                    AVAILABLE_CAPACITY

                )

                VALUES (?,?,?,?,?,?,?)

                """,

                id,

                wh,

                s(x.get("code")),

                s(x.get("type")),

                cap,

                used,

                Math.max(0, cap - used)

        );

        return db.queryForMap(

                """

                SELECT

                    LOCATION_ID,

                    LOCATION_CODE CODE,

                    LOCATION_TYPE TYPE,

                    CAPACITY,

                    USED_CAPACITY,

                    AVAILABLE_CAPACITY

                FROM WAREHOUSE_LOCATIONS

                WHERE LOCATION_ID=?

                """,

                id

        );

    }

    @Transactional

    public Map<String, Object> updateLocation(

            long id,

            Map<String, Object> x

    ) {

        int cap = i(x.get("capacity"));

        int used = i(x.get("usedCapacity"));

        db.update(

                """

                UPDATE WAREHOUSE_LOCATIONS

                SET

                    LOCATION_CODE=?,

                    LOCATION_TYPE=?,

                    CAPACITY=?,

                    USED_CAPACITY=?,

                    AVAILABLE_CAPACITY=?

                WHERE LOCATION_ID=?

                """,

                s(x.get("code")),

                s(x.get("type")),

                cap,

                used,

                Math.max(0, cap - used),

                id

        );

        return db.queryForMap(

                """

                SELECT

                    LOCATION_ID,

                    LOCATION_CODE CODE,

                    LOCATION_TYPE TYPE,

                    CAPACITY,

                    USED_CAPACITY,

                    AVAILABLE_CAPACITY

                FROM WAREHOUSE_LOCATIONS

                WHERE LOCATION_ID=?

                """,

                id

        );

    }

    @Transactional

    public void deleteLocation(long id) {

        if (

                db.queryForObject(

                        """

                        SELECT COUNT(*)

                        FROM STOCK_RECEIVING

                        WHERE LOCATION_ID=?

                        """,

                        Integer.class,

                        id

                ) > 0

        ) {

            throw new IllegalStateException(

                    "Location is used by receiving records and cannot be deleted"

            );

        }

        db.update(

                "DELETE FROM WAREHOUSE_LOCATIONS WHERE LOCATION_ID=?",

                id

        );

    }

    // =========================================================

    // USERS

    // =========================================================

    public List<Map<String, Object>> users() {

        return db.queryForList(

                """

                SELECT

                    USER_ID,

                    USERNAME,

                    PASSWORD,

                    ROLE,

                    CASE

                        WHEN ROLE='ADMIN'

                        THEN 'Warehouse Manager'

                        ELSE 'Staff'

                    END DISPLAY_ROLE,

                    'ACTIVE' STATUS

                FROM USERS

                ORDER BY USER_ID

                """

        );

    }

    @Transactional

    public Map<String, Object> createUser(

            Map<String, Object> x

    ) {

        int id = db.queryForObject(

                "SELECT NVL(MAX(USER_ID),0)+1 FROM USERS",

                Integer.class

        );

        String role =

                s(x.get("role")).trim().toUpperCase();

        db.update(

                """

                INSERT INTO USERS

                (

                    USER_ID,

                    USERNAME,

                    PASSWORD,

                    ROLE

                )

                VALUES (?,?,?,?)

                """,

                id,

                s(x.get("username")).trim(),

                s(x.get("password")),

                role

        );

        return db.queryForMap(

                """

                SELECT

                    USER_ID,

                    USERNAME,

                    PASSWORD,

                    ROLE,

                    CASE

                        WHEN ROLE='ADMIN'

                        THEN 'Warehouse Manager'

                        ELSE 'Staff'

                    END DISPLAY_ROLE,

                    'ACTIVE' STATUS

                FROM USERS

                WHERE USER_ID=?

                """,

                id

        );

    }

    @Transactional

    public Map<String, Object> updateUser(

            long id,

            Map<String, Object> x

    ) {

        if (

                x.containsKey("password")

                &&

                !s(x.get("password")).isBlank()

        ) {

            db.update(

                    """

                    UPDATE USERS

                    SET

                        PASSWORD=?,

                        ROLE=?

                    WHERE USER_ID=?

                    """,

                    s(x.get("password")),

                    s(x.get("role")).trim().toUpperCase(),

                    id

            );

        } else {

            db.update(

                    """

                    UPDATE USERS

                    SET ROLE=?

                    WHERE USER_ID=?

                    """,

                    s(x.get("role")).trim().toUpperCase(),

                    id

            );

        }

        return db.queryForMap(

                """

                SELECT

                    USER_ID,

                    USERNAME,

                    PASSWORD,

                    ROLE,

                    CASE

                        WHEN ROLE='ADMIN'

                        THEN 'Warehouse Manager'

                        ELSE 'Staff'

                    END DISPLAY_ROLE,

                    'ACTIVE' STATUS

                FROM USERS

                WHERE USER_ID=?

                """,

                id

        );

    }

    @Transactional

    public void deleteUser(long id) {

        db.update(

                "DELETE FROM USERS WHERE USER_ID=?",

                id

        );

    }

    // =========================================================

    // REPORTS

    // =========================================================

    public Map<String, Object> reports() {

        Map<String, Object> out =

                new LinkedHashMap<>();

        out.put(

                "totalProducts",

                db.queryForObject(

                        "SELECT COUNT(*) FROM PRODUCTS",

                        Integer.class

                )

        );

        out.put(

                "availableStock",

                db.queryForObject(

                        """

                        SELECT NVL(SUM(AVAILABLE_STOCK),0)

                        FROM INVENTORY

                        """,

                        Integer.class

                )

        );

        out.put(

                "lowStock",

                db.queryForObject(

                        """

                        SELECT COUNT(*)

                        FROM INVENTORY

                        WHERE AVAILABLE_STOCK<=REORDER_LEVEL

                        """,

                        Integer.class

                )

        );

        out.put(

                "pendingOrders",

                db.queryForObject(

                        """

                        SELECT COUNT(*)

                        FROM ORDERS

                        WHERE ORDER_STATUS

                        NOT IN ('SHIPPED','CANCELLED')

                        """,

                        Integer.class

                )

        );

        out.put(

                "shippedOrders",

                db.queryForObject(

                        """

                        SELECT COUNT(*)

                        FROM ORDERS

                        WHERE ORDER_STATUS='SHIPPED'

                        """,

                        Integer.class

                )

        );

        out.put(

                "warehouseUtilization",

                db.queryForObject(

                        """

                        SELECT

                            CASE

                                WHEN NVL(SUM(CAPACITY),0)=0

                                THEN 0

                                ELSE ROUND(

                                    SUM(USED_CAPACITY)

                                    /

                                    SUM(CAPACITY)

                                    * 100,

                                    2

                                )

                            END

                        FROM WAREHOUSE_LOCATIONS

                        """,

                        Double.class

                )

        );

        out.put(

                "lowStockProducts",

                db.queryForList(

                        """

                        SELECT

                            p.PRODUCT_ID,

                            p.PRODUCT_NAME,

                            i.AVAILABLE_STOCK,

                            i.REORDER_LEVEL,

                            i.REORDER_QUANTITY

                        FROM INVENTORY i

                        JOIN PRODUCTS p

                            ON p.PRODUCT_ID=i.PRODUCT_ID

                        WHERE i.AVAILABLE_STOCK<=i.REORDER_LEVEL

                        ORDER BY i.AVAILABLE_STOCK

                        """

                )

        );

        return out;

    }

}
