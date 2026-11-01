package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.controller.support.Filters;
import com.quimia.quimiawebcrud.controller.support.ListController;
import com.quimia.quimiawebcrud.controller.support.Lookups;
import com.quimia.quimiawebcrud.dto.response.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.dto.response.ShelfResponseDTO;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.ShelfProductService;
import com.quimia.quimiawebcrud.service.crud.ShelfService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Consulta somente leitura das estantes cadastradas pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/shelves")
@RequiredArgsConstructor
public class ShelfController extends ListController<ShelfResponseDTO> {

    private final ShelfService shelfService;
    private final UserService userService;
    private final ProductService productService;
    private final ShelfProductService shelfProductService;

    @Override
    protected List<ShelfResponseDTO> readAll() {
        return shelfService.readAll();
    }

    @Override
    protected String getBasePath() {
        return "/shelves";
    }

    @Override
    protected String getViewFolder() {
        return "shelf";
    }

    @Override
    protected String getPluralName() {
        return "shelves";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
        model.addAttribute("productsById", Lookups.byId(productService.readAll()));
    }

    @Override
    protected Predicate<ShelfResponseDTO> filterBy(Map<String, String> params, Model model) {
        Set<Integer> shelvesWithProduct = new HashSet<>();
        String productId = params.get("produto");
        if (productId != null && !productId.isBlank()) {
            for (ShelfProductResponseDTO link : shelfProductService.readAll()) {
                if (Filters.sameId(link.getProductId(), productId)) {
                    shelvesWithProduct.add(link.getShelfId());
                }
            }
        }
        return shelf -> Filters.matchesText(params.get("busca"), shelf.getRoom())
                && Filters.sameId(shelf.getUserId(), params.get("usuario"))
                && (productId == null || productId.isBlank() || shelvesWithProduct.contains(shelf.getId()));
    }
}
