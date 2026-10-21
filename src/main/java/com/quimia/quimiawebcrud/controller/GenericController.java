package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.exception.BusinessException;
import com.quimia.quimiawebcrud.service.CrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Telas de cadastro: além da lista, têm as rotas de salvar e excluir usadas pelos popups.
 * Erros de regra de negócio viram aviso na própria tela; erros do banco ganham uma mensagem amigável;
 * qualquer outro erro sobe para o tratador global (página de erro).
 */
@Slf4j
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
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao salvar em {}", getBasePath(), e);
            redirect.addFlashAttribute("error",
                    "Não foi possível salvar: já existe um registro com esses dados ou algum dado relacionado é inválido.");
        }
        return redirectToList();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") ID id, RedirectAttributes redirect) {
        try {
            getService().delete(id);
            redirect.addFlashAttribute("message", "Registro excluído com sucesso.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao excluir em {}", getBasePath(), e);
            redirect.addFlashAttribute("error",
                    "Não foi possível excluir: este registro está em uso por outros dados.");
        }
        return redirectToList();
    }

    protected String redirectToList() {
        return "redirect:" + getBasePath();
    }
}
