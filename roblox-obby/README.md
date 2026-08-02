# 🧗 Obby Base — Roblox (vibecoding)

Uma base **completa e pronta pra jogar** de um Obby (jogo de obstáculos) no
Roblox, escrita em **Luau**. Foi feita pra vibecoding: você marca as partes
com _tags_ no Studio e os scripts fazem toda a lógica sozinhos. Sem precisar
copiar script em cada plataforma. 🎮

## O que já vem pronto

| Sistema | O que faz | Tag pra usar |
|---|---|---|
| ✅ Checkpoints | Salva o progresso e renasce o jogador no último estágio | `Checkpoint` |
| 🔥 Lava / Killer | Mata quem encostar | `Killer` |
| ↔️ Plataforma móvel | Vai e volta sozinha (TweenService) | `MovingPlatform` |
| 👻 Plataforma que some | Some quando você pisa, volta depois | `Disappearing` |
| 🏆 Linha de chegada | Mostra a tela de "Você venceu!" | `Finish` |
| 🖥️ Interface | Contador de estágio + tela de vitória | (automático) |

Tudo o que dá pra ajustar (velocidade, distância, tempos) está num único
arquivo fácil: [`src/shared/Config.luau`](src/shared/Config.luau).

## Estrutura dos arquivos

```
roblox-obby/
├── default.project.json         <- config do Rojo (sincronizar com o Studio)
└── src/
    ├── shared/
    │   └── Config.luau           <- TODOS os ajustes do jogo ficam aqui
    ├── server/                   <- lógica do servidor (ServerScriptService)
    │   ├── CheckpointService.server.luau
    │   ├── KillPartService.server.luau
    │   ├── MovingPlatformService.server.luau
    │   ├── DisappearingPlatformService.server.luau
    │   └── FinishService.server.luau
    └── client/                   <- interface do jogador (StarterPlayerScripts)
        └── StageGui.client.luau
```

---

## Como colocar pra rodar no Roblox Studio

Tem dois caminhos. Se você está começando, use o **Jeito 1 (manual)**.

### Jeito 1 — Manual (copiar e colar) — recomendado pra iniciante

1. Abra o **Roblox Studio** e crie um lugar novo (Baseplate serve).
2. Na janela **Explorer**, crie os scripts assim:
   - Em **ReplicatedStorage** → crie uma **Folder** chamada `Shared` →
     dentro dela, um **ModuleScript** chamado `Config` → cole o conteúdo de
     `src/shared/Config.luau`.
   - Em **ServerScriptService** → crie um **Script** normal pra cada arquivo
     da pasta `src/server/` (use o mesmo nome) e cole o conteúdo.
   - Em **StarterPlayer → StarterPlayerScripts** → crie um **LocalScript**
     chamado `StageGui` e cole o conteúdo de `src/client/StageGui.client.luau`.

> Dica: o sufixo `.server.luau` = **Script** (roda no servidor);
> `.client.luau` = **LocalScript** (roda no jogador); sem sufixo = **ModuleScript**.

### Jeito 2 — Com Rojo (fluxo profissional, sincroniza automático)

1. Instale o [Rojo](https://rojo.space/) e o plugin dele no Studio.
2. No terminal, dentro da pasta `roblox-obby/`, rode:
   ```bash
   rojo serve
   ```
3. No Studio, abra o plugin do Rojo e clique em **Connect**. Pronto — seus
   arquivos aparecem no jogo e qualquer edição que você salvar aqui atualiza
   no Studio na hora.

---

## Como montar a fase (o passo mais divertido!)

Depois que os scripts estão no lugar, você **constrói a fase com blocos** e
só marca cada bloco com a tag certa:

1. No Studio, ative o editor de tags: menu **View → Tag Editor** (ou instale
   o plugin gratuito "Tag Editor").
2. Crie as tags: `Checkpoint`, `Killer`, `MovingPlatform`, `Disappearing`,
   `Finish`.
3. Selecione uma Part e marque a tag desejada:
   - **Checkpoint:** marque a tag `Checkpoint` **e** adicione um Atributo
     chamado `Stage` (number) com o número da fase: `0`, `1`, `2`, ...
     (o `0` é o começo; use números crescentes pra frente).
   - **Lava:** tag `Killer`.
   - **Plataforma móvel:** tag `MovingPlatform` (e deixe `Anchored` = ✔️).
   - **Plataforma que some:** tag `Disappearing` (e `Anchored` = ✔️).
   - **Chegada:** tag `Finish`.
4. Aperte **Play** ▶️ e teste!

> Pra adicionar um **Atributo**: selecione a Part → painel **Properties** →
> role até o fim → **Attributes** → **＋** → nome `Stage`, tipo `number`.

---

## Ideias pra continuar vibecodando 💡

- **Estilos de lava:** mude a `Color` das partes `Killer` pra criar veneno
  (verde), gelo (azul), etc.
- **Dificuldade:** no `Config.luau`, diminua o `Time` das plataformas móveis
  pra deixar tudo mais rápido e tenso.
- **Recompensa:** dê moedas quando o jogador passa de estágio (dá pra
  adicionar um `IntValue` "Coins" no `leaderstats`).
- **Salvar progresso entre sessões:** troque a memória por `DataStoreService`
  pra o jogador continuar de onde parou quando voltar ao jogo.
- **Sons:** toque um som de "morte" no `KillPartService` e um de "vitória"
  no `FinishService`.

Bom jogo e bom código! 🚀
