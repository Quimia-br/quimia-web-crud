package com.quimia.quimiawebcrud.service.photo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Onde as fotos já validadas são guardadas: numa pasta do servidor ({@link DiskPhotoStore}) ou num bucket da
 * Cloudflare R2 ({@link R2PhotoStore}). O {@link PhotoStorageService} confere e regrava a imagem; esta interface só
 * cuida de gravar, apagar e reconhecer as fotos que ela mesma guardou.
 */
public interface PhotoStore {

    /** Nome de arquivo que o sistema gera (UUID + extensão); qualquer outro nome nunca é aceito. */
    Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)$");

    /** Guarda a foto com este nome e devolve o endereço público dela (o que vai para o banco). */
    String save(String fileName, byte[] content, String contentType);

    /** Apaga a foto pelo nome; se ela não existir, não é erro. */
    void delete(String fileName);

    /** Se o endereço é de uma foto guardada por esta implementação, devolve o nome do arquivo; senão, vazio. */
    Optional<String> ownFileName(String photoUrl);

    /** Arquivo no disco, para a rota que entrega a foto. Só o disco serve fotos; o R2 as entrega por conta própria. */
    default Optional<Path> find(String fileName) {
        return Optional.empty();
    }
}
