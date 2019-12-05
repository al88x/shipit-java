package com.softwire.training.shipit.controller;

import com.softwire.training.shipit.dao.EmployeeDAO;
import com.softwire.training.shipit.exception.ClientVisibleException;
import com.softwire.training.shipit.exception.MalformedRequestException;
import com.softwire.training.shipit.exception.NoSuchEntityException;
import com.softwire.training.shipit.model.Employee;
import com.softwire.training.shipit.model.Employees;
import com.softwire.training.shipit.model.RenderableAsXML;
import com.softwire.training.shipit.utils.TransactionManagerUtils;
import com.softwire.training.shipit.utils.XMLParsingUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class EmployeeController extends BaseController {
    private static Logger sLog = Logger.getLogger(EmployeeController.class);

    private EmployeeDAO employeeDAO;

    public void setEmployeeDAO(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    protected RenderableAsXML handlePostMethod(
            Element documentElement,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Node employees = XMLParsingUtils.getSingleElementByTagName(documentElement, "employees");

        List<Employee> employeesToAdd = new ArrayList<Employee>();
        NodeList employeeNodes = employees.getChildNodes();
        for (int i = 0; i < employeeNodes.getLength(); i++) {
            Node node = employeeNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            employeesToAdd.add(Employee.parseXML((Element) node));
        }

        if (employeesToAdd.size() == 0) {
            throw new MalformedRequestException("Expected at least one <employee> tag");
        }

        sLog.info("Adding employees: " + employeesToAdd);

        TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            employeeDAO.addEmployees(employeesToAdd);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            TransactionManagerUtils.rollbackIgnoringErrors(transactionManager, txStatus, sLog);
            throw e;
        }

        sLog.debug("Employees added successfully");

        return null;
    }

    protected RenderableAsXML handleGetMethod(
            HttpServletRequest request,
            HttpServletResponse response) throws ClientVisibleException, ServletRequestBindingException {
        String action = ServletRequestUtils.getStringParameter(request, "action");

        if ("get".equals(action)) {
            return handleGetAction(request);
        } else if ("delete".equals(action)) {
            return handleDeleteAction(request);
        } else {
            throw new MalformedRequestException("Invalid or missing action: " + action);
        }
    }

    private RenderableAsXML handleGetAction(HttpServletRequest request)
            throws ServletRequestBindingException, ClientVisibleException {
        String name = ServletRequestUtils.getStringParameter(request, "name");
        Integer warehouseId = ServletRequestUtils.getIntParameter(request, "warehouseId");

        if (name == null && warehouseId == null) {
            throw new MalformedRequestException("Unable to parse name or warehouse from request parameters");
        }
        if (name != null && warehouseId != null) {
            throw new MalformedRequestException("Only one of name and warehouse id is valid on this request");
        }

        sLog.info(String.format("Looking up employee by name: %s or id: %d", name, warehouseId));

        List<Employee> employees;
        if (name != null) {

            Employee employee = employeeDAO.getEmployee(name);
            if (employee == null) {
                throw new NoSuchEntityException("No employee exists with name: " + name);
            }
            sLog.info("Found employee: " + employee);
            return employee;
        } else {
            employees = employeeDAO.getEmployees(warehouseId);
            if (employees.isEmpty()) {
                throw new NoSuchEntityException("No employee exists with warehouseId: " + warehouseId);
            }
            sLog.info("Found employees: " + employees);
            return new Employees(employees);
        }
    }

    private RenderableAsXML handleDeleteAction(HttpServletRequest request)
            throws ServletRequestBindingException, ClientVisibleException {
        String name = ServletRequestUtils.getStringParameter(request, "name");
        if (name == null) {
            throw new MalformedRequestException("Unable to parse name from request parameters");
        }

        try {
            employeeDAO.removeEmployee(name);
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchEntityException("No employee exists with name: " + name, e);
        }

        return null;
    }

}