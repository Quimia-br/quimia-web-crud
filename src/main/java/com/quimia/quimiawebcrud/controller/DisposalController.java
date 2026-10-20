package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.DisposalService;
import com.quimia.quimiawebcrud.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/disposals")
@RequiredArgsConstructor
public class DisposalController extends GenericController<Disposal, Integer, DisposalResponseDTO, DisposalRequestDTO> {

    private final DisposalService disposalService;
    private final ProductService productService;

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
    protected String getSingularName() {
        return "disposal";
    }

    @Override
    protected String getPluralName() {
        return "disposals";
    }

    @Override
    protected DisposalResponseDTO newResponse() {
        return new DisposalResponseDTO();
    }

    @Override
    protected void addFormAttributes(Model model) {
        model.addAttribute("products", productService.readAll());
    }
}
