# Filtro de Chamadas

App Android que recusa automaticamente ligações de DDDs fora da sua região.

## Regras

- **DDDs permitidos** (padrão: 99) → tocam normalmente. Dá para adicionar ou remover DDDs no app.
- **Contatos salvos** → sempre tocam, de qualquer DDD.
- **Outros DDDs** → recusados sem tocar e registrados no histórico do app.
- **0303** (telemarketing) e **internacionais** → recusados.
- **0800, 4004, números sem DDD** → tocam normalmente.
- **Números ocultos** → opção no app (desligada por padrão).

## Baixar

O GitHub compila o app automaticamente a cada alteração. O arquivo mais recente fica em **Releases → FiltroChamadas.apk**.

## Primeiro uso

1. Instale o .apk (permita "instalar apps desconhecidos" quando o Android pedir).
2. Abra o app e toque em **Fazer agora** nos dois passos: definir como app de filtro de chamadas e permitir acesso aos contatos.

Requer Android 10 ou mais novo.
