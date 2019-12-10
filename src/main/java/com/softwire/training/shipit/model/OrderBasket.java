package com.softwire.training.shipit.model;

import com.softwire.training.shipit.dao.StockAlteration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderBasket {

//    private class Order {
//        private Product product;
//        private int quantity;
//    }

    private int warehouseId;
//    private List<Order> orders;

    private Map<String, StockAlteration> lineItems;
    private List<Integer> productIds;

    public OrderBasket() {
        lineItems = new HashMap<String, StockAlteration>();
        productIds = new ArrayList<Integer>();
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void addLineItem(String gtin, StockAlteration stockAlteration) {
        lineItems.put(gtin, stockAlteration);
    }

    public void addProductId(Integer productId) {
        productIds.add(productId);
    }

    public Map<String, StockAlteration> getLineItems() {
        return lineItems;
    }

    public List<Integer> getProductIds() {
        return productIds;
    }

    public List<StockAlteration> getListOfLineItems() {
        return new ArrayList<StockAlteration>(lineItems.values());
    }
}
