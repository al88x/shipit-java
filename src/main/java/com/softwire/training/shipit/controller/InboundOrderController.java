package com.softwire.training.shipit.controller;

import com.softwire.training.shipit.dao.*;
import com.softwire.training.shipit.exception.ClientVisibleException;
import com.softwire.training.shipit.exception.InvalidStateException;
import com.softwire.training.shipit.exception.MalformedRequestException;
import com.softwire.training.shipit.exception.ValidationException;
import com.softwire.training.shipit.model.*;
import com.softwire.training.shipit.utils.XMLParsingUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class InboundOrderController extends BaseController {
    private static Logger sLog = Logger.getLogger(InboundOrderController.class);

    private StockDAO stockDAO;
    private ProductDAO productDAO;
    private EmployeeDAO employeeDAO;
    private CompanyDAO companyDAO;
    private InboundOrderDataDAO inboundOrderDataDAO;

    public void setStockDAO(StockDAO stockDAO) {
        this.stockDAO = stockDAO;
    }

    public void setProductDAO(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public void setEmployeeDAO(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public void setCompanyDAO(CompanyDAO companyDAO) {
        this.companyDAO = companyDAO;
    }

    public void setInboundOrderDataDAO(InboundOrderDataDAO inboundOrderDataDAO) {
        this.inboundOrderDataDAO = inboundOrderDataDAO;
    }

    protected RenderableAsXML handleGetMethod(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String action = ServletRequestUtils.getStringParameter(request, "action");
        sLog.debug(String.format("Handling action: %s", action));

        if ("create".equals(action)) {
            return handleCreateAction(request);
        } else {
            throw new MalformedRequestException("Invalid or missing action: " + action);
        }
    }

    private RenderableAsXML handleCreateAction(HttpServletRequest request)
            throws ServletRequestBindingException, ClientVisibleException {
        long startTime = System.currentTimeMillis();
        Integer warehouseId = ServletRequestUtils.getIntParameter(request, "warehouseId");
        if (warehouseId == null) {
            throw new MalformedRequestException("Missing warehouseId");
        }

        sLog.info("orderIn for warehouse ID: " + warehouseId);

        Employee operationsManager = getEmployeeFromDatabase(warehouseId);

//        List<OrderSegment> segments = getOrderSegmentsFromDatabaseSlow(warehouseId);
        List<OrderSegment> segments = getOrderSegmentsFromDatabaseFaster(warehouseId);


        long endTime = System.currentTimeMillis();
        System.err.println("Time taken: " + (endTime - startTime));

        return new InboundOrder(warehouseId, segments, operationsManager);
    }

    private List<OrderSegment> getOrderSegmentsFromDatabaseFaster(Integer warehouseId) {
        List<InboundOrderData> inboundOrderDataList = inboundOrderDataDAO.getInboundOrderByWarehouseId(warehouseId);

        Map<Company, List<InboundOrderLine>> orderlinesByCompany = new HashMap<Company, List<InboundOrderLine>>();
        for (InboundOrderData order : inboundOrderDataList) {
            if (!orderlinesByCompany.containsKey(order.getCompany())) {
                orderlinesByCompany.put(order.getCompany(), new ArrayList<InboundOrderLine>());
            }

            orderlinesByCompany.get(order.getCompany()).add(
                    new InboundOrderLine(order.getGtinCode(), order.getGtinName(), order.getInboundOrderLineQuantity()));
        }

        sLog.debug(String.format("Constructed order lines: %s", orderlinesByCompany));

        Set<Map.Entry<Company, List<InboundOrderLine>>> entrySet = orderlinesByCompany.entrySet();
        List<OrderSegment> segments = new ArrayList<OrderSegment>(entrySet.size());
        for (Map.Entry<Company, List<InboundOrderLine>> entry : entrySet) {
            segments.add(new OrderSegment(entry.getKey(), entry.getValue()));
        }
        sLog.info("Constructed inbound order");
        return segments;
    }

    private Employee getEmployeeFromDatabase(Integer warehouseId) throws InvalidStateException {
        List<Employee> operationsManagers = employeeDAO.getEmployees(warehouseId, EmployeeRole.OPERATIONS_MANAGER);
        if (operationsManagers.size() != 1) {
            throw new InvalidStateException(
                    "There should be exactly one operations manager per warehouse, but found: " + operationsManagers);
        }
        Employee operationsManager = operationsManagers.get(0);
        sLog.debug(String.format("Found operations manager: %s", operationsManager));
        return operationsManager;
    }

    private List<OrderSegment> getOrderSegmentsFromDatabaseSlow(Integer warehouseId) {
        List<Stock> allStock = stockDAO.getStock(warehouseId);

        Map<Company, List<InboundOrderLine>> orderlinesByCompany = new HashMap<Company, List<InboundOrderLine>>();

        for (Stock item : allStock) {
            Product product = productDAO.getProduct(item.getProductId());
            if (item.getHeld() < product.getLowerThreshold() && !product.isDiscontinued()) {
                Company company = companyDAO.getCompany(product.getGcp());
                int orderQuantity = NumberUtils.max(
                        product.getLowerThreshold() * 3 - item.getHeld(), product.getMinimumOrderQuantity());

                if (!orderlinesByCompany.containsKey(company)) {
                    orderlinesByCompany.put(company, new ArrayList<InboundOrderLine>());
                }

                orderlinesByCompany.get(company).add(
                        new InboundOrderLine(product.getGtin(), product.getName(), orderQuantity));
            }
        }

        sLog.debug(String.format("Constructed order lines: %s", orderlinesByCompany));

        Set<Map.Entry<Company, List<InboundOrderLine>>> entrySet = orderlinesByCompany.entrySet();
        List<OrderSegment> segments = new ArrayList<OrderSegment>(entrySet.size());
        for (Map.Entry<Company, List<InboundOrderLine>> entry : entrySet) {
            segments.add(new OrderSegment(entry.getKey(), entry.getValue()));
        }
        sLog.info("Constructed inbound order");
        return segments;
    }

    protected RenderableAsXML handlePostMethod(
            Element documentElement,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        InboundManifest inboundManifest = InboundManifest.parseXML((Element)
                XMLParsingUtils.getSingleElementByTagName(documentElement, "inboundManifest"));
        sLog.info(String.format("Processing manifest: %s", inboundManifest));

        List<String> gtins = new ArrayList<String>(inboundManifest.getOrderLines().size());
        for (OrderLine orderLine : inboundManifest.getOrderLines()) {
            gtins.add(orderLine.getGtin());
        }

        Map<String, Product> products = productDAO.getProductsByGtin(gtins);
        sLog.debug(String.format("Retrieved products to verify manifest: %s", products));

        List<StockAlteration> lineItems =
                new ArrayList<StockAlteration>(inboundManifest.getOrderLines().size());
        List<String> errors = new ArrayList<String>();
        for (OrderLine orderLine : inboundManifest.getOrderLines()) {
            Product product = products.get(orderLine.getGtin());
            if (product == null) {
                errors.add(String.format("Unknown product gtin: %s", orderLine.getGtin()));
            } else if (!product.getGcp().equals(inboundManifest.getGcp())) {
                errors.add(String.format("Manifest GCP (%s) doesn't match Product GCP (%s)",
                        inboundManifest.getGcp(), product));
            } else {
                lineItems.add(new StockAlteration(product.getId(), orderLine.getQuantity()));
            }
        }
        if (errors.size() > 0) {
            sLog.debug(String.format("Found errors with inbound manifest: %s", errors));
            throw new ValidationException(String.format("Found inconsistencies in the inbound manifest: %s", errors));
        }

        sLog.debug(String.format("Increasing stock levels with manifest: %s", inboundManifest));
        stockDAO.addStock(inboundManifest.getWarehouseId(), lineItems);
        sLog.info("Stock levels increased");

        return null;
    }
}
