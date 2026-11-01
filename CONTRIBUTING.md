# Como contribuir

Obrigado por querer ajudar no painel administrativo do Quimia. Este guia resume o que é preciso para rodar o projeto, como organizar as branches e como abrir uma PR. O detalhamento da aplicação está no [README](README.md).

## Pré-requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 25 |
| Maven | não precisa instalar: o projeto usa o Maven Wrapper (`mvnw`) |
| Banco | PostgreSQL com o schema do [`quimia-bd-primeiro`](https://github.com/Quimia-br/quimia-bd-primeiro) e a view `mv_dashboard` |

O painel não cria nem altera tabelas (`spring.jpa.hibernate.ddl-auto=validate`). Mudanças no banco são feitas no repositório do schema e combinadas com quem cuida dele.

## Rodar localmente

1. Clone o repositório e entre na `develop`:

   ```bash
   git clone https://github.com/Quimia-br/quimia-web-crud.git
   cd quimia-web-crud
   git switch develop
   ```

2. Crie o seu `.env` a partir do modelo `.env.example`:

   ```bash
   cp .env.example .env
   ```

   Preencha as variáveis do banco (`DATABASE_URL`, `DATABASE_USER` e `DATABASE_PASSWORD`). As demais são opcionais para rodar no computador e estão explicadas no próprio `.env.example` e no [README](README.md#2-configurar-o-env).

3. Suba a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows (PowerShell), use `.\mvnw.cmd spring-boot:run`. Depois abra <http://localhost:8080>.

> [!CAUTION]
> O `.env` fica fora do Git (está no `.gitignore`) e nunca deve ser versionado. Não coloque senha, chave ou dado pessoal real no `.env.example`, no código nem na PR. Se o `.env` aparecer no `git status`, pare e confira antes de continuar.

## Rodar os testes

```bash
./mvnw test
```

Rode antes de abrir a PR.

## Fluxo de branches

- `main`: recebe só o que já foi integrado na `develop`. Nada vai direto para a `main`. Quando algo entra nela, o workflow de release cria a tag e o release no GitHub.
- `develop`: branch de integração. Toda funcionalidade, correção e ajuste entra por aqui.

Passo a passo:

1. Parta sempre da `develop` atualizada.
2. Crie uma branch com prefixo do tipo: `feat/`, `fix/`, `refactor/`, `style/`, `test/` ou `chore/` (por exemplo, `feat/nova-tela`).
3. Faça os commits e rode `./mvnw test`.
4. Abra a PR **para a `develop`** e peça a revisão de alguém da equipe.

## Commits e título de PR

Os commits são escritos em inglês, no imperativo, no padrão [Conventional Commits](https://www.conventionalcommits.org/):

```text
tipo(escopo opcional): descrição
```

O título da PR segue o mesmo padrão, e o workflow `pr-title.yml` reprova a PR se ele não bater. Tipos aceitos:

`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

O escopo é opcional (letras minúsculas, números, `.`, `_`, `/` e `-`), e `!` depois do tipo ou do escopo marca uma mudança que quebra compatibilidade. Exemplos:

```text
feat(sidebar): add a pin button
fix(lists): rename the Filtrar button to Buscar
docs: add README
refactor: split controller, dto, mapper and repository by role
```

O título da PR aparece nas notas do release, e a versão é calculada pelos commits.

## Checklist da PR

Ao abrir a PR, o GitHub preenche a descrição com o modelo em [`.github/pull_request_template.md`](.github/pull_request_template.md). Preencha todos os campos e confira a checklist que ele traz antes de pedir a revisão.

## Bugs e sugestões

Use a aba [Issues](https://github.com/Quimia-br/quimia-web-crud/issues) do repositório e escolha o modelo **Bug** ou **Melhoria**:

- **Bug:** descreva o que aconteceu, o que era esperado e como reproduzir (tela, passos e, se houver, a mensagem de erro). Não cole senhas, chaves nem dados pessoais.
- **Melhoria:** explique o problema ou a necessidade e o que você imagina como solução.

Antes de abrir uma nova, procure se já não existe uma issue sobre o mesmo assunto.

Falhas de segurança **não** devem virar issue pública. Siga o [SECURITY.md](SECURITY.md).
