# Narutoto: All Bijuu (addon)

Addon para o mod **Narutoto 0.4.1** (Forge 1.20.1 (testado para compilar com Forge 47.4.10), Java 17). Adiciona os 8 Bijuu (Shukaku a Gyuki), sistema de
Jinchuuriki, transformações em 6 estágios, Bijuu Dama, extração/selamento, covis com guardiões e rastreamento.

## Compilar pelo GitHub (navegador)
1. Crie um repositório e envie **todo o conteúdo** desta pasta (arraste os arquivos para "Add file > Upload files").
2. Pastas ocultas (`.github`) às vezes não sobem pelo upload: crie `.github/workflows/build.yml` em "Add file > Create new file"
   (digite o caminho com as barras) e cole o conteúdo do `build.yml` fornecido.
3. Aba **Actions** > "Build narutoto-allbijuu" > execute. Ao terminar, baixe o artefato `narutoto-allbijuu-2.1.0`
   (o `.jar` fica dentro). Se der erro, copie o log da etapa **Build** e me envie.
4. Coloque o `.jar` em `mods/` junto com o `narutoto-0_4_1-forge.jar`.

## Como jogar
- **Diário dos Bijuu** (aba Ferramentas do criativo): clique direito inicia a missão e mostra o covil; Shift+clique troca o Bijuu rastreado.
  Segurando o diário, a barra de ação mostra seta e distância.
- Chegue perto do covil (~64 blocos): ele aparece (plataforma + altar + guardiões). Mate os guardiões e clique no **Altar**.
- O Bijuu abaixo de 25% de vida fica **enfraquecido**: use o **Pergaminho de Selamento** nele (segure perto até o ritual acabar).
- **Extração**: pergaminho em outro jogador hospedeiro -> você recebe o **Receptáculo**; clique direito nele para se selar (expira em 2 dias de jogo).
- Teclas (mude em Controles): G transforma (Shift+G desfaz), H/J/K/L habilidades, N Bijuu Dama (segure), Y meditar, O status.
- Controle: sem controle suficiente, subir de estágio causa descontrole. Medite e use o estágio 1 para treinar.
- Comandos de teste (op): `/allbijuu seal <bijuu>`, `release`, `reset <bijuu>`, `locate <bijuu>`, `mastery <n>`, `status`.

## Limites conhecidos (ainda não testado em jogo)
- Código escrito sem compilador no ambiente: pode haver erros de compilação na primeira tentativa.
- Integração com o Narutoto por reflexão (chakra, nível ninja, afinidades); se algo mudar, ela se desativa sozinha.
- Sem camada de aura/skin no jogador: a transformação usa partículas e atributos.
- 2.0.0: modelos individuais (ver abaixo). Os oito arquivos `.bbmodel` enviados separadamente já estão copiados, sem alteração, para `src/main/resources/assets/narutoto_allbijuu/models/entity/`. O renderizador do cliente carrega geometria, UV e animações diretamente desses arquivos e usa as texturas PNG correspondentes. A compilação ainda precisa ser executada no GitHub e o resultado precisa ser testado dentro do Minecraft; não é possível garantir a aparência final sem esse teste.
- Não analisei os outros nove addons (não foram enviados), então não há integração específica com eles.

## Versão 2.1.0 – interface e HUD revisados
- Tela de status reorganizada com painel, hierarquia visual e barra de chakra.
- HUD com barra de chakra limitada corretamente entre 0% e 100%, fundo e contorno para melhor leitura.
- Layout da tela de status adaptado à largura e altura disponíveis.

## Versão 2.0.0 – modelos individuais dos oito Bijuu
Cada Bijuu tem seu próprio arquivo Blockbench, textura e textura emissiva (brilho), todos em
`src/main/resources/assets/narutoto_allbijuu/`:

| Bijuu | Modelo | Textura | Brilho |
|---|---|---|---|
| Shukaku (tanuki de areia, 1 cauda) | `models/entity/shukaku.bbmodel` | `textures/entity/bijuu_shukaku.png` | `bijuu_shukaku_glow.png` |
| Matatabi (gato azul, 2 caudas em chamas) | `matatabi.bbmodel` | `bijuu_matatabi.png` | `bijuu_matatabi_glow.png` |
| Isobu (tartaruga com carapaça espinhosa, 3 caudas) | `isobu.bbmodel` | `bijuu_isobu.png` | `bijuu_isobu_glow.png` |
| Son Goku (gorila vermelho, 4 caudas) | `songoku.bbmodel` | `bijuu_songoku.png` | `bijuu_songoku_glow.png` |
| Kokuo (cavalo/golfinho, 5 caudas) | `kokuo.bbmodel` | `bijuu_kokuo.png` | `bijuu_kokuo_glow.png` |
| Saiken (lesma segmentada, 6 caudas) | `saiken.bbmodel` | `bijuu_saiken.png` | `bijuu_saiken_glow.png` |
| Chomei (inseto alado, 7 caudas) | `chomei.bbmodel` | `bijuu_chomei.png` | `bijuu_chomei_glow.png` |
| Gyuki (touro + tentáculos, 8 caudas) | `gyuki.bbmodel` | `bijuu_gyuki.png` | `bijuu_gyuki_glow.png` |

- **O `.bbmodel` é o arquivo que o jogo carrega** (`client/bb/BbModelData`): geometria, ossos, UV e animações vêm dele.
  Edite no Blockbench (formato Free, Box UV), salve no mesmo caminho e recompile.
- Animações em cada modelo: `idle`, `walk`, `run`, `attack`, `hurt`, `death`, `dama`, `skill1`..`skill4`, `weak`
  (e `fly` na Chomei). O jogo escolhe: parado/andando/correndo pela velocidade, `attack` ao acertar, `hurt` ao levar dano,
  `skillN` ao usar a habilidade N, `dama` na carga da Bijuu Dama, `weak` quando enfraquecido (selável), `death` ao morrer.
- Nomes de ossos importam: `head`, `jaw`, `body`, `tailN_M`/`tentN_M` (caudas e tentáculos em segmentos independentes).
- Chomei paira ~1,5 bloco acima do solo (enquanto não está enfraquecida) com asas batendo; Matatabi emite partículas de chama azul.
- Os sistemas de Jinchuuriki, transformação, selamento, extração, Bijuu Dama, missões, guardiões e interface não foram alterados.
- `tools/generator/` contém os scripts Python que geraram os modelos/texturas/animações (`python3 export.py`).
