package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.CrudService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Base CRUD controller for the Thymeleaf views.
 * <p>
 * Routes (relative to the subclass {@code @RequestMapping}):
 * GET / | GET /new | GET /{id}/edit | POST /save | POST /{id}/delete
 * <p>
 * Views: {@code <viewFolder>/list} and {@code <viewFolder>/form}.
 */
@Controller
public abstract class GenericController<T, ID, Res, Req> {

    protected abstract CrudService<T, ID, Req, Res> getService();

    /** URL prefix used in redirects, e.g. "/locations". */
    protected abstract String getBasePath();

    /** Template folder, e.g. "location". */
    protected abstract String getViewFolder();

    /** Model attribute for the form object, e.g. "location". */
    protected abstract String getSingularName();

    /** Model attribute for the list, e.g. "locations". */
    protected abstract String getPluralName();

    /** Empty response used to render the "new" form (id == null). */
    protected abstract Res newResponse();

    /** Hook for extra form data, e.g. select options. */
    protected void addFormAttributes(Model model) {
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute(getPluralName(), getService().readAll());
        return getViewFolder() + "/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute(getSingularName(), newResponse());
        addFormAttributes(model);
        return getViewFolder() + "/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") ID id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute(getSingularName(), getService().findById(id));
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return redirectToList();
        }
        addFormAttributes(model);
        return getViewFolder() + "/form";
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
            return "redirect:" + getBasePath() + (id == null ? "/new" : "/" + id + "/edit");
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
