# 🌳 Árvore + Cipó — Roblox (vibecoding)

Recomeço do zero, focado no essencial: uma **árvore caprichada** e um **cipó
em que você pode se pendurar e balançar**. Escrito em **Luau**.

## O que tem

| Coisa | O que faz |
|---|---|
| 🌳 Árvore | Tronco que afina, raízes, galhos e copa de folhagem em vários tons |
| 🌿 Cipó | Pendura num galho e balança feito pêndulo — dá pra se segurar e voar |

**Controles do cipó:**
- **Encostar** no cipó → o personagem se pendura e começa a balançar
- **Espaço** (pulo) → solta, aproveitando o embalo pra ser lançado 🚀

## Estrutura

```
roblox-obby/
├── default.project.json      <- config do Rojo
├── Obby.rbxlx                <- arquivo pronto: dê 2 cliques pra abrir no Studio
└── src/
    ├── server/
    │   ├── World.server.luau        <- monta o chão, a árvore e o cipó
    │   ├── VineGrab.server.luau      <- lógica de agarrar/soltar o cipó
    │   └── LightingSetup.server.luau <- iluminação de floresta
    └── client/
        └── VineClient.client.luau    <- "espaço = soltar" + dica na tela
```

## Como abrir (o jeito fácil)

1. Baixe o arquivo **`Obby.rbxlx`**.
2. Dê **dois cliques** nele → abre no Roblox Studio com tudo montado.
3. Aperte **Play** ▶️, ande até o cipó, encoste pra se pendurar e aperte
   **espaço** pra soltar.

## Como mexer

- **Tamanho/formato da árvore:** `World.server.luau` (seções do tronco,
  galhos e as "bolotas" da copa).
- **Posição e comprimento do cipó:** `World.server.luau`, procure por
  `vineTopPos` e `vineLength`.
- **Força do impulso ao soltar:** `VineGrab.server.luau`, no valor
  `Vector3.new(0, 22, 0)` (aumente o 22 pra voar mais alto).

Quer mais cipós? É só duplicar a Part do cipó no Studio e marcar com a tag
`Grabbable` — o script cuida do resto. 🌿
