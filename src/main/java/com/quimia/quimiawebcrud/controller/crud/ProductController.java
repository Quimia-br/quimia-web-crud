package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.controller.support.ColumnField;
import com.quimia.quimiawebcrud.controller.support.Filters;
import com.quimia.quimiawebcrud.controller.support.GenericController;
import com.quimia.quimiawebcrud.controller.support.Lookups;
import com.quimia.quimiawebcrud.dto.request.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.response.ProductResponseDTO;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Telas de produtos em {@code /products}: lista, detalhe, cadastro, edição e remoção.
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController extends GenericController<Product, Integer, ProductRequestDTO, ProductResponseDTO> {

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
    protected List<ColumnField> getColumnFields() {
        return List.of(
                ColumnField.filter("nome", "Nome"),
                ColumnField.filter("cas_number", "Nº CAS"),
                ColumnField.editable("marca", "Marca"),
                ColumnField.editable("id_usuario_empresa", "Empresa (ID)"),
                ColumnField.editable("instrucao_de_uso", "Instruções de uso"),
                ColumnField.editable("dosagem_tecnica", "Dosagem técnica"));
    }

    @Override
    protected Predicate<ProductResponseDTO> filterBy(Map<String, String> params, Model model) {
        return product -> Filters.matchesText(params.get("busca"), product.getName(), product.getCasNumber())
                && Filters.sameId(product.getCompanyId(), params.get("empresa"))
                && Filters.matchesText(params.get("marca"), product.getBrand());
    }
}
