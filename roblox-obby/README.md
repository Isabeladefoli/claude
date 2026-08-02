# 🐊 Tarzan do Lago — Roblox (vibecoding)

Um desafio estilo **Tarzan**: pule de **cipó em cipó** por cima de um **lago
cheio de jacarés** e chegue no **troféu** do outro lado — sem cair na água!
Escrito em **Luau**.

## O que tem

| Coisa | O que faz |
|---|---|
| 🐊 Lago com jacarés | Água de verdade (terrain) com 5 jacarés que nadam, abrem a boca e **te comem se você cair** |
| 🌳 Duas árvores | Cada uma com tronco que afina, raízes, galhos e copa de folhagem |
| 🌿 Dois cipós | Penduram sobre a água. Parado ele fica parado; **você balança com W/S** (pêndulo com física de rótula) |
| 🙌 Pose de braço | Ao agarrar, seus dois braços **sobem segurando o cipó** (R6 e R15) |
| 🏆 Troféu | O objetivo, brilhando na ilha de chegada |

## Como jogar

1. Você nasce na **ilha de partida**.
2. Corra e **encoste no 1º cipó** → seu personagem se pendura, os braços sobem.
   O cipó fica **parado** até você mandar.
3. Segure **W** pra balançar **pra frente** (e **S** pra voltar). Vai e volta
   pra ganhar **embalo** e subir cada vez mais alto.
4. No auge do balanço pra frente, aperte **Espaço** → você solta com o embalo
   e é **lançado pelo ar** 🚀.
5. **Agarre o 2º cipó** no meio do voo, balance de novo (W) e solte pra pousar
   na **ilha de chegada** com o troféu 🏆.
6. **Caiu na água?** Os jacarés não perdoam — você renasce na partida. 🐊

## Estrutura

```
roblox-obby/
├── default.project.json      <- config do Rojo
├── build_rbxlx.py            <- gera o Obby.rbxlx a partir de src/
├── Obby.rbxlx                <- arquivo pronto: dê 2 cliques pra abrir no Studio
└── src/
    ├── server/
    │   ├── World.server.luau        <- lago, ilhas, 2 árvores, 2 cipós, troféu
    │   ├── VineGrab.server.luau      <- lógica de agarrar/soltar o cipó
    │   ├── Crocodiles.server.luau    <- os jacarés (nadar, morder, caçar)
    │   └── LightingSetup.server.luau <- iluminação de floresta
    └── client/
        ├── VineClient.client.luau    <- "espaço = soltar" + dica na tela
        ├── VineControl.client.luau   <- W/S balançam o cipó (frente/trás)
        └── ArmPose.client.luau       <- braços erguidos segurando o cipó
```

## Como abrir (o jeito fácil)

1. Baixe o arquivo **`Obby.rbxlx`**.
2. Dê **dois cliques** nele → abre no Roblox Studio com tudo montado.
3. Aperte **Play** ▶️ e comece a se balançar!

> Se você editar algum script em `src/` e não usa o Rojo, rode
> `python3 build_rbxlx.py` pra regerar o `Obby.rbxlx` com as mudanças.

## Como mexer (afinar o jogo)

- **Distância entre os cipós / posição das árvores:** `World.server.luau`,
  procure `VINE_A_ANCHOR`, `VINE_B_ANCHOR`, `TREE_A_BASE`, `TREE_B_BASE`.
- **Comprimento do cipó:** `World.server.luau`, no `makeVine(..., 15, ...)`.
- **Força do pulo ao soltar:** `VineGrab.server.luau`, no valor `+ 26` (mais
  alto = voa mais longe) e no `* 1.15` (empurrão horizontal).
- **Força do balanço (W/S):** `VineControl.client.luau`, nos valores `ACCEL`
  (quão rápido ganha embalo) e `MAX_SWING` (quão forte balança).
- **Quantos jacarés / velocidade:** `Crocodiles.server.luau`, `NUM_CROCS` e
  `speed`.
- **Altura da pose do braço:** `ArmPose.client.luau`, no valor `RAISE`.

Quer mais cipós? Duplique a Part de um cipó no Studio e marque com a tag
`Grabbable` — o script cuida do resto. 🌿
