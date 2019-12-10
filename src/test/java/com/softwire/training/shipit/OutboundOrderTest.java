package com.softwire.training.shipit;

import com.softwire.training.shipit.builder.CompanyBuilder;
import com.softwire.training.shipit.builder.EmployeeBuilder;
import com.softwire.training.shipit.builder.ProductBuilder;
import com.softwire.training.shipit.controller.OutboundOrderController;
import com.softwire.training.shipit.dao.StockAlteration;
import com.softwire.training.shipit.exception.InsufficientStockException;
import com.softwire.training.shipit.exception.NoSuchEntityException;
import com.softwire.training.shipit.exception.ValidationException;
import com.softwire.training.shipit.model.Employee;
import com.softwire.training.shipit.model.OrderLine;
import com.softwire.training.shipit.model.OutboundOrder;
import com.softwire.training.shipit.model.Product;
import com.softwire.training.shipit.model.truck.TruckManifest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Collections;

public class OutboundOrderTest extends AbstractBaseTest {
    private static final Employee EMPLOYEE = new EmployeeBuilder().createEmployee();
    private static final int WAREHOUSE_ID = EMPLOYEE.getWarehouseId();

    private Product product;
    private String gtin;

    private OutboundOrderController outboundOrderController;

    public void setOutboundOrderController(OutboundOrderController outboundOrderController) {
        this.outboundOrderController = outboundOrderController;
    }

    public void onSetUp() throws Exception {
        super.onSetUp();
        employeeDAO.addEmployees(Collections.singletonList(EMPLOYEE));
        product = new ProductBuilder().createProduct();
        gtin = product.getGtin();
        productDAO.addProducts(Collections.singletonList(product));
        companyDAO.addCompanies(Collections.singletonList(new CompanyBuilder().createCompany()));
    }

    public void testParseOutboundOrder() throws Exception {
        String xml = "<outboundOrder>" +
                "<warehouseId>2</warehouseId>" +
                "<orderLines>" +
                "<outboundOrderLine><gtin>10879210</gtin><quantity>4</quantity></outboundOrderLine>" +
                "<outboundOrderLine><gtin>43294842</gtin><quantity>723412</quantity></outboundOrderLine>" +
                "</orderLines>" +
                "</outboundOrder>";
        assertEquals(OutboundOrder.parseXML(buildXMLFragment(xml)), new OutboundOrder(2, Arrays.asList(
                new OrderLine("10879210", 4),
                new OrderLine("43294842", 723412))));
    }

    public void testOutboundOrder() throws Exception {
        System.err.println(product.getGtin());
        System.err.println(product.getWeight());
        stockDAO.addStock(WAREHOUSE_ID,
                Collections.singletonList(new StockAlteration(product.getId(), 10000)));
        MockHttpServletRequest request = createPostRequest(
                "<outboundOrder>" +
                            "<warehouseId>1</warehouseId>" +
                            "<orderLines>" +
                                "<orderLine>" +
                                    "<gtin>" + gtin + "</gtin>" +
                                    "<quantity>10000</quantity>" +
                                "</orderLine>" +
                            "</orderLines>" +
                        "</outboundOrder>");

        String output = "<TruckManifest><warehouseId>1</warehouseId><NumberOfTrucks>2</NumberOfTrucks><Trucks><Truck><TotalWeight>1999.8 kg</TotalWeight><OutboundOrderLines><OrderLine><gtin>0000346374230</gtin><name>2 Count 1 T30 Torx Bit Tips TX</name><quantity>6666</quantity><totalWeightOfOrder>1999.8 kg</totalWeightOfOrder></OrderLine></OutboundOrderLine></Truck><Truck><TotalWeight>1000.2 kg</TotalWeight><OutboundOrderLines><OrderLine><gtin>0000346374230</gtin><name>2 Count 1 T30 Torx Bit Tips TX</name><quantity>3334</quantity><totalWeightOfOrder>1000.2 kg</totalWeightOfOrder></OrderLine></OutboundOrderLine></Truck></Trucks></TruckManifest>";


        TruckManifest truckManifest = assertSuccessResponseAndReturnContent(outboundOrderController.handleRequest(request, new MockHttpServletResponse()), TruckManifest.class);
        String truckManifestString = truckManifest.renderXML();
        assertEquals(truckManifestString, output);

        assertEquals(stockDAO.getStock(1, product.getId()).getHeld(), 0);
    }

    public void testOutboundOrderInsufficientStock() throws Exception {
        stockDAO.addStock(EMPLOYEE.getWarehouseId(),
                Collections.singletonList(new StockAlteration(product.getId(), 10)));
        MockHttpServletRequest request = createPostRequest("<outboundOrder>" +
                "<warehouseId>" + WAREHOUSE_ID + "</warehouseId>" +
                "<orderLines>" +
                "<orderLine><gtin>0000346374230</gtin><quantity>11</quantity></orderLine>" +
                "</orderLines>" +
                "</outboundOrder>");

        try {
            outboundOrderController.handleRequest(request, new MockHttpServletResponse());
            fail("Expected exception to be thrown");
        } catch (InsufficientStockException e) {
            // Do nothing
        }

        assertEquals(stockDAO.getStock(WAREHOUSE_ID, product.getId()).getHeld(), 10);
    }

    public void testOutboundOrderStockNotHeld() throws Exception {
        MockHttpServletRequest request = createPostRequest("<outboundOrder>" +
                "<warehouseId>1</warehouseId>" +
                "<orderLines>" +
                "<orderLine><gtin>0000346374230</gtin><quantity>10000</quantity></orderLine>" +
                "</orderLines>" +
                "</outboundOrder>");

        try {
            outboundOrderController.handleRequest(request, new MockHttpServletResponse());
            fail("Expected exception to be thrown");
        } catch (InsufficientStockException e) {
            assertTrue(e.getMessage().contains("no stock held"));
        }

        assertNull(stockDAO.getStock(WAREHOUSE_ID, product.getId()));
    }

    public void testOutboundOrderBadGtin() throws Exception {
        MockHttpServletRequest request = createPostRequest("<outboundOrder>" +
                "<warehouseId>1</warehouseId>" +
                "<orderLines>" +
                "<orderLine><gtin>987654321</gtin><quantity>10000</quantity></orderLine>" +
                "</orderLines>" +
                "</outboundOrder>");

        try {
            outboundOrderController.handleRequest(request, new MockHttpServletResponse());
            fail("Expected exception to be thrown");
        } catch (NoSuchEntityException e) {
            assertTrue(e.getMessage().contains("987654321"));
        }
    }

    public void testOutboundOrderDuplicateGtins() throws Exception {
        stockDAO.addStock(EMPLOYEE.getWarehouseId(),
                Collections.singletonList(new StockAlteration(product.getId(), 10)));
        MockHttpServletRequest request = createPostRequest("<outboundOrder>" +
                "<warehouseId>1</warehouseId>" +
                "<orderLines>" +
                "<orderLine><gtin>" + gtin + "</gtin><quantity>1</quantity></orderLine>" +
                "<orderLine><gtin>" + gtin + "</gtin><quantity>1</quantity></orderLine>" +
                "</orderLines>" +
                "</outboundOrder>");

        try {
            outboundOrderController.handleRequest(request, new MockHttpServletResponse());
            fail("Expected exception to be thrown");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains(gtin));
        }
    }
}
