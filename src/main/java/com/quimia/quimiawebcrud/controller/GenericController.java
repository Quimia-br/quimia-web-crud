package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.CrudService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Telas de cadastro: além da lista, têm as rotas de salvar e excluir usadas pelos popups.
 */
public abstract class GenericController<T, ID, Res, Req> extends ListController<Res> {

    protected abstract CrudService<T, ID, Req, Res> getService();

    @Override
    protected List<Res> readAll() {
        return getService().readAll();
    }

    @PostMapping("/save")
    public String save(@RequestParam(name = "id", required = false) ID id,
                       @ModelAttribute Req request,
                       RedirectAttributes redirect) {
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

    protected String redirectToList() {
        return "redirect:" + getBasePath();
    }
}
