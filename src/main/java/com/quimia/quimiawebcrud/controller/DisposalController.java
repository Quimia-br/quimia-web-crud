package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.DisposalService;
import com.quimia.quimiawebcrud.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Telas de descartes em {@code /disposals}: lista, detalhe, cadastro, edição e remoção.
 */
@Controller
@RequestMapping("/disposals")
@RequiredArgsConstructor
public class DisposalController extends GenericController<Disposal, Integer, DisposalRequestDTO, DisposalResponseDTO> {

    private final DisposalService disposalService;
    private final ProductService productService;
    private final CompanyService companyService;

    @Override
    protected CrudService<Disposal, Integer, DisposalRequestDTO, DisposalResponseDTO> getService() {
        return disposalService;
    }

    @Override
    protected String getBasePath() {
        return "/disposals";
    }

    @Override
    protected String getViewFolder() {
        return "disposal";
    }

    @Override
    protected String getPluralName() {
        return "disposals";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("productsById", Lookups.byId(productService.readAll()));
        model.addAttribute("companiesById", Lookups.byId(companyService.readAll()));
    }

    @Override
    protected List<ColumnField> getColumnFields() {
        return List.of(
                ColumnField.filter("id_produto", "Produto (ID)"),
                ColumnField.editable("metodo_descarte_produto", "Descarte do produto"),
                ColumnField.editable("metodo_descarte_embalagem", "Descarte da embalagem"),
                ColumnField.editable("restricao_descarte", "Restrições de descarte"),
                ColumnField.editable("precaucao_ambiental", "Precauções ambientais"));
    }

    @Override
    protected Predicate<DisposalResponseDTO> filterBy(Map<String, String> params, Model model) {
        Map<Integer, ProductResponseDTO> productsById = Lookups.fromModel(model, "productsById");
        String method = params.get("metodo");
        return disposal -> {
            ProductResponseDTO product = productsById.get(disposal.getProductId());
            boolean hasMethod = disposal.getProductDisposalMethod() != null && !disposal.getProductDisposalMethod().isBlank();
            return Filters.matchesText(params.get("busca"),
                            product == null ? null : product.getName(), product == null ? null : product.getCasNumber())
                    && Filters.sameId(product == null ? null : product.getCompanyId(), params.get("empresa"))
                    && (method == null || method.isBlank() || ("PREENCHIDO".equals(method) == hasMethod));
        };
    }
}
