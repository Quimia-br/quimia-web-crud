package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Telas de cadastro: além da lista, têm as rotas de salvar e excluir usadas pelos popups.
 */
public abstract class GenericController<T, ID, Res, Req> extends ListController<Res> {

    private static final int MAX_MESSAGES = 3;

    protected abstract CrudService<T, ID, Req, Res> getService();

    @Override
    protected List<Res> readAll() {
        return getService().readAll();
    }

    @PostMapping("/save")
    public String save(@RequestParam(name = "id", required = false) ID id,
                       @Valid @ModelAttribute Req request,
                       BindingResult validation,
                       RedirectAttributes redirect) {
        if (validation.hasErrors()) {
            redirect.addFlashAttribute("error", describe(validation));
            return redirectToList();
        }
        try {
            if (id == null) {
                getService().create(request);
                redirect.addFlashAttribute("message", "Registro criado com sucesso.");
            } else {
                getService().update(id, request);
                redirect.addFlashAttribute("message", "Registro atualizado com sucesso.");
            }
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redirectToList();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") ID id, RedirectAttributes redirect) {
        try {
            getService().delete(id);
            redirect.addFlashAttribute("message", "Registro excluído com sucesso.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redirectToList();
    }

    /**
     * Junta as mensagens de validação (até {@value #MAX_MESSAGES}) em um único aviso para a tela.
     */
    private String describe(BindingResult validation) {
        Set<String> messages = new LinkedHashSet<>();
        for (ObjectError error : validation.getAllErrors()) {
            if (error instanceof FieldError field && "typeMismatch".equals(field.getCode())) {
                messages.add("O valor informado no campo \"" + field.getField() + "\" é inválido.");
            } else {
                messages.add(error.getDefaultMessage());
            }
        }
        return messages.stream().limit(MAX_MESSAGES).collect(Collectors.joining(" "));
    }

    protected String redirectToList() {
        return "redirect:" + getBasePath();
    }
}
