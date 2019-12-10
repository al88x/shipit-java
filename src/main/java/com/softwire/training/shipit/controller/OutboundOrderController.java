package com.softwire.training.shipit.controller;

import com.softwire.training.shipit.dao.ProductDAO;
import com.softwire.training.shipit.dao.StockAlteration;
import com.softwire.training.shipit.dao.StockDAO;
import com.softwire.training.shipit.exception.InsufficientStockException;
import com.softwire.training.shipit.exception.NoSuchEntityException;
import com.softwire.training.shipit.model.*;
import com.softwire.training.shipit.model.truck.OutboundOrderLine;
import com.softwire.training.shipit.model.truck.TruckManifest;
import com.softwire.training.shipit.utils.TransactionManagerUtils;
import com.softwire.training.shipit.utils.XMLParsingUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OutboundOrderController extends BaseController {
    private static Logger sLog = Logger.getLogger(OutboundOrderController.class);

    private StockDAO stockDAO;
    private ProductDAO productDAO;
    private TruckManifest truckManifest;

    public void setStockDAO(StockDAO stockDAO) {
        this.stockDAO = stockDAO;
    }
    public void setProductDAO(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }


    protected RenderableAsXML handlePostMethod(Element documentElement, HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

        OutboundOrder outboundOrder = OutboundOrder.parseXML((Element)
                XMLParsingUtils.getSingleElementByTagName(documentElement, "outboundOrder"));

        sLog.info(String.format("Processing outbound order: %s", outboundOrder));
        Map<String, Product> products = getProductsFromOutboundOrder(outboundOrder);

        OrderBasket orderBasket = buildOrderBasket(outboundOrder, products);

        TransactionStatus txStatus = transactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.ISOLATION_SERIALIZABLE));
        try {
            if (isStockAvailable(orderBasket)) {
                stockDAO.removeStock(orderBasket.getWarehouseId(), orderBasket.getListOfLineItems());
                truckManifest = buildTruckManifest(orderBasket, products);
                transactionManager.commit(txStatus);
            }
        } catch (Exception e) {
            TransactionManagerUtils.rollbackIgnoringErrors(transactionManager, txStatus, sLog);
            throw e;
        }
        return truckManifest;
    }

    private boolean isStockAvailable(OrderBasket orderBasket) throws InsufficientStockException {
        List<String> errors;
        Map<Integer, Stock> stock = stockDAO.getStock(orderBasket.getWarehouseId(), orderBasket.getProductIds());

        errors = new ArrayList<String>();
        for(String productCode : orderBasket.getLineItems().keySet()){
            StockAlteration lineItem = orderBasket.getLineItems().get(productCode);

            Stock item = stock.get(lineItem.getProductId());
            if (item == null) {
                errors.add(String.format("Product: %s, no stock held", productCode));
            } else if (lineItem.getQuantity() > item.getHeld()) {
                errors.add(String.format("Product: %s, stock held: %s, stock to remove: %s",
                        productCode, item.getHeld(), lineItem.getQuantity()));
            }
        }

        if (errors.size() > 0) {
            throw new InsufficientStockException(StringUtils.join(errors, "; "));
        }
        return true;
    }

    private OrderBasket buildOrderBasket(OutboundOrder outboundOrder, Map<String, Product> products) throws NoSuchEntityException {
        OrderBasket orderBasket = new OrderBasket();
        orderBasket.setWarehouseId(outboundOrder.getWarehouseId());
        List<String> errors = new ArrayList<String>();
        for (OrderLine orderLine : outboundOrder.getOrderLines()) {
            Product product = products.get(orderLine.getGtin());

            if (product == null) {
                errors.add(String.format("Unknown product: %s", orderLine.getGtin()));
            } else {
                Integer productId = product.getId();
                orderBasket.addLineItem(orderLine.getGtin(), new StockAlteration(productId, orderLine.getQuantity()));
                orderBasket.addProductId(productId);
            }
        }

        if (errors.size() > 0) {
            throw new NoSuchEntityException(StringUtils.join(errors, "; "));
        }
        return orderBasket;
    }

    private Map<String, Product> getProductsFromOutboundOrder(OutboundOrder outboundOrder) {
        List<String> gtins = new ArrayList<String>();
        for (OrderLine orderLine : outboundOrder.getOrderLines()) {
            gtins.add(orderLine.getGtin());
        }
        return productDAO.getProductsByGtin(gtins);
    }

    private TruckManifest buildTruckManifest(OrderBasket orderBasket, Map<String, Product> products) {
        TruckManifest truckManifest = new TruckManifest();
        for (StockAlteration lineItem : orderBasket.getLineItems().values()) {
            Product product = getProductFromMap(products, lineItem.getProductId());
            if (product == null) {
                product = productDAO.getProduct(lineItem.getProductId());
            }
            truckManifest.addOrder(new OutboundOrderLine(product.getGtin(), product.getName(),
                    lineItem.getQuantity(), product.getWeight()));

        }
        truckManifest.setWarehouseId(orderBasket.getWarehouseId());
        truckManifest.buildManifest();
        return truckManifest;
    }

    private Product getProductFromMap(Map<String, Product> products, int productId) {
        for (Product product : products.values()) {
            if (product.getId() == productId) {
                return product;
            }
        }
        return null;
    }
}
