package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.BaseEntity;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.model.User;

import java.util.List;

/**
 * Resumo legível de um registro, gravado no log de edições (antes de editar ou de excluir).
 * A senha nunca entra no resumo.
 */
public final class RecordSummary {

    private RecordSummary() {
    }

    public static String of(BaseEntity record) {
        return String.join("\n", fields(record));
    }

    public static List<String> fields(BaseEntity record) {
        return switch (record) {
            case User user -> List.of(
                    field("Nome", user.getUserName()), field("E-mail", user.getEmail()), field("Status", user.getStatus()),
                    field("Nascimento", user.getBirthDate()), field("Cadastro", user.getSignUpDate()),
                    field("Foto", user.getPhotoUrl()));
            case Company company -> List.of(
                    field("Nome", company.getName()), field("CNPJ", company.getCnpj()), field("E-mail", company.getEmail()),
                    field("Porte", company.getSize()), field("Status", company.getStatus()),
                    field("Cadastro", company.getSignUpDate()), field("Foto", company.getPhotoUrl()));
            case Product product -> List.of(
                    field("Nome", product.getName()), field("CAS", product.getCasNumber()), field("Marca", product.getBrand()),
                    field("Empresa", product.getCompany() == null ? null : product.getCompany().getName()),
                    field("Instruções", product.getInstructions()), field("Dosagem", product.getDosage()));
            case Disposal disposal -> List.of(
                    field("Produto", disposal.getProduct() == null ? null : disposal.getProduct().getName()),
                    field("Descarte do produto", disposal.getProductDisposalMethod()),
                    field("Descarte da embalagem", disposal.getPackagingDisposalMethod()),
                    field("Restrições", disposal.getDisposalRestrictions()),
                    field("Precauções ambientais", disposal.getEnvironmentalPrecautions()));
            case HistoryType historyType -> List.of(field("Nome", historyType.getName()));
            case Admin admin -> List.of(
                    field("Nome", admin.getUserName()), field("E-mail", admin.getEmail()), field("Status", admin.getStatus()),
                    field("Cadastro", admin.getSignUpDate()));
            default -> List.of(field("ID", record.getId()));
        };
    }

    private static String field(String label, Object value) {
        return label + ": " + (value == null || value.toString().isBlank() ? "-" : value);
    }
}
