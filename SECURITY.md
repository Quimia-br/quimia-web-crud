# Política de segurança

## Versões suportadas

Damos suporte de segurança apenas à branch `main` e à última release publicada. Versões e branches antigas não recebem correções.

## Como reportar uma vulnerabilidade

Não abra uma issue pública para falhas de segurança.

Use o reporte privado do GitHub:

1. Abra a aba **Security** do repositório [Quimia-br/quimia-web-crud](https://github.com/Quimia-br/quimia-web-crud).
2. Clique em **Report a vulnerability**.
3. Descreva o problema: o que foi encontrado, onde, como reproduzir e qual o impacto que você imagina.

Não inclua senhas, chaves ou dados pessoais reais no relato. Se for preciso demonstrar o problema, use dados fictícios.

Faremos o possível para responder em um prazo razoável.

## Escopo

Exemplos do que conta como vulnerabilidade:

- Acesso indevido ao painel, como entrar sem login, contornar a autenticação ou acessar telas e ações de administrador sem permissão.
- Exposição de dados, como senhas, chaves, variáveis de ambiente ou dados de usuários, empresas e produtos visíveis para quem não deveria.
- Falhas que permitem alterar ou apagar registros sem autorização, ou injetar código nas páginas ou no banco (injeção de SQL, XSS, CSRF e semelhantes).
- Envio de arquivos (fotos) que permita executar código ou ler arquivos do servidor.

Exemplos do que não conta:

- Problemas visuais, de layout ou de texto sem efeito na segurança.
- Bugs comuns de funcionamento, que devem ser reportados como [issues](https://github.com/Quimia-br/quimia-web-crud/issues).
- Ataques que dependem de acesso físico ao computador de quem usa o painel ou de uma conta de administrador já comprometida por culpa do próprio usuário.
- Testes automatizados de carga ou de negação de serviço contra o ambiente em produção.
- Falhas em serviços de terceiros usados pelo projeto, que devem ser reportadas aos responsáveis por eles.
