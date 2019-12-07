package com.softwire.training.shipit.dao;

public class StockAlteration {

    private int productId;
    private int quantity;

    public StockAlteration(int productId, int quantity)
    {
        this.productId = productId;
        this.quantity = quantity;

        if (quantity < 0)
        {
            throw new IllegalArgumentException("Alteration must be positive");
        }
    }

    public int getProductId()
    {
        return productId;
    }

    public int getQuantity()
    {
        return quantity;
    }
}
