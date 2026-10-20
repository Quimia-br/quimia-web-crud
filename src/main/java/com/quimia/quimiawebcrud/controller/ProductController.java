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

import java.util.Map;
import java.util.function.Predicate;

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
    protected String getPluralName() {
        return "products";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("companiesById", Lookups.byId(companyService.readAll()));
    }

    @Override
    protected Predicate<ProductResponseDTO> filterBy(Map<String, String> params, Model model) {
        return product -> Filters.matchesText(params.get("busca"), product.getName(), product.getCasNumber())
                && Filters.sameId(product.getCompanyId(), params.get("empresa"))
                && Filters.matchesText(params.get("marca"), product.getBrand());
    }
}
