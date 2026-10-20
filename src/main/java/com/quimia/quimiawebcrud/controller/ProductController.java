package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController extends GenericController<Product, Integer, ProductResponseDTO, ProductRequestDTO> {

    private final ProductService productService;
    private final CompanyService companyService;

    @Override
    protected CrudService<Product, Integer, ProductRequestDTO, ProductResponseDTO> getService() {
        return productService;
    }

    @Override
    protected String getBasePath() {
        return "/products";
    }

    @Override
    protected String getViewFolder() {
        return "product";
    }

    @Override
    protected String getSingularName() {
        return "product";
    }

    @Override
    protected String getPluralName() {
        return "products";
    }

    @Override
    protected ProductResponseDTO newResponse() {
        return new ProductResponseDTO();
    }

    @Override
    protected void addFormAttributes(Model model) {
        model.addAttribute("companies", companyService.readAll());
    }
}
