package com.warehouse.management.controller;

import com.warehouse.management.service.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final WarehouseService service;

    public ApiController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "warehouse-management-backend");
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return service.login(body);
    }

    @GetMapping("/products")
    public List<Map<String, Object>> products() {
        return service.products();
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> x) {
        return ResponseEntity.ok(service.createProduct(x));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Map<String, Object> x) {
        return ResponseEntity.ok(service.updateProduct(id, x));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suppliers")
    public List<Map<String, Object>> suppliers() {
        return service.suppliers();
    }

    @PostMapping("/suppliers")
    public Map<String, Object> createSupplier(@RequestBody Map<String, Object> x) {
        return service.createSupplier(x);
    }

    @PutMapping("/suppliers/{id}")
    public Map<String, Object> updateSupplier(@PathVariable String id, @RequestBody Map<String, Object> x) {
        return service.updateSupplier(id, x);
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable String id) {
        service.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> inventory() {
        return service.inventory();
    }

    @PutMapping("/inventory/{id}")
    public Map<String, Object> updateInventory(@PathVariable String id, @RequestBody Map<String, Object> x) {
        return service.updateInventory(id, x);
    }

    @GetMapping("/receiving")
    public List<Map<String, Object>> receiving() {
        return service.receiving();
    }

    @PostMapping("/receiving")
    public Map<String, Object> createReceiving(@RequestBody Map<String, Object> x) {
        return service.createReceiving(x);
    }

    /**
     * Complete putaway for a receiving record.
     * The service should validate the receiving status and update inventory/capacity
     * transactionally, so the same receiving record cannot be applied twice.
     */
    @PutMapping("/receiving/{id}/putaway/complete")
    public Map<String, Object> completePutaway(@PathVariable int id) {
        return service.completePutaway(id);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders() {
        return service.orders();
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> x) {
        return ResponseEntity.ok(service.createOrder(x));
    }

    @PutMapping("/orders/{id}")
    public Map<String, Object> updateOrder(@PathVariable long id, @RequestBody Map<String, Object> x) {
        return service.updateOrder(id, x);
    }

    @GetMapping("/warehouses/locations")
    public List<Map<String, Object>> locations() {
        return service.locations();
    }

    @PostMapping("/warehouses/locations")
    public Map<String, Object> createLocation(@RequestBody Map<String, Object> x) {
        return service.createLocation(x);
    }

    @PutMapping("/warehouses/locations/{id}")
    public Map<String, Object> updateLocation(@PathVariable long id, @RequestBody Map<String, Object> x) {
        return service.updateLocation(id, x);
    }

    @DeleteMapping("/warehouses/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable long id) {
        service.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return service.users();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> x) {
        return service.createUser(x);
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable long id, @RequestBody Map<String, Object> x) {
        return service.updateUser(id, x);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public Map<String, Object> reports() {
        return service.reports();
    }
}
