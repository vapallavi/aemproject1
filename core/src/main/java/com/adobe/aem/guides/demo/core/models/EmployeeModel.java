package com.adobe.aem.guides.demo.core.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class EmployeeModel {

    @ChildResource(name = "employees")
    private Resource employeesResource;

    @ChildResource(name = "salaryDetails")
    private Resource salaryDetailsResource;

    public List<EmployeeItem> getEmployees() {

        List<EmployeeItem> employees = new ArrayList<>();

        if (employeesResource != null) {
            for (Resource resource : employeesResource.getChildren()) {

                EmployeeItem employee = resource.adaptTo(EmployeeItem.class);

                if (employee != null) {
                    employees.add(employee);
                }
            }
        }

        return employees;
    }

    public List<SalaryItem> getSalaryDetails() {

        List<SalaryItem> salaryDetails = new ArrayList<>();

        if (salaryDetailsResource != null) {
            for (Resource resource : salaryDetailsResource.getChildren()) {

                SalaryItem salary = resource.adaptTo(SalaryItem.class);

                if (salary != null) {
                    salaryDetails.add(salary);
                }
            }
        }

        return salaryDetails;
    }

    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class EmployeeItem {

        @ValueMapValue
        private String employeeName;

        @ValueMapValue
        private String employeeId;

        @ValueMapValue
        private String designation;

        @ValueMapValue
        private String department;

        @ValueMapValue
        private String email;

        public String getEmployeeName() {
            return employeeName;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public String getDesignation() {
            return designation;
        }

        public String getDepartment() {
            return department;
        }

        public String getEmail() {
            return email;
        }
    }

    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class SalaryItem {

        @ValueMapValue
        private String salaryAmount;

        @ValueMapValue
        private String currency;

        @ValueMapValue
        private String effectiveDate;

        public String getSalaryAmount() {
            return salaryAmount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getEffectiveDate() {
            return effectiveDate;
        }
    }
}