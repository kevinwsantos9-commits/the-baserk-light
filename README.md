# Mod Transformação (NeoForge 1.21.1)

Mate monstros → a armadura ganha XP e sobe de nível → gaste pontos na tela de status →
aperte **R**: o jogador solta um grunhido, um raio cai nele (**sem dano**) e a armadura aparece.

## Controles (mudam em Opções > Controles > Transformação)
| Tecla | Ação |
|-------|------|
| **R** | Transformar (rugido → raio → armadura). Apertar de novo destransforma. |
| **G** | Abrir a tela de status (nível, barra de XP, pontos, botões +). |

## Como funciona
- **XP**: todo monstro (`Enemy`) morto por você dá XP = metade da vida máxima dele (zumbi = 10). **Dobra enquanto transformado.**
- **Nível**: precisa de `50 + 25 × (nível − 1)` XP para subir; cada nível dá 1 ponto. Máximo 30.
- **Status** (máx. 20 pontos cada): Força (+0,5 dano), Vitalidade (+1 coração), Defesa (+1 armadura), Agilidade (+3 % velocidade).
- **Ao transformar**: bônus base de +2 dano, +2 corações, +4 armadura, +5 % velocidade, mais o que você distribuiu. Cura total.
- **Morreu?** Nível/XP/pontos ficam; a armadura volta a "destransformada".
- **Multiplayer**: os outros jogadores veem a animação de rugido e a armadura.
- A armadura (`armadura.obj`) segue o corpo: anda, agacha, nada e balança os braços junto com o jogador.

## Rodar
1. JDK 21 + IntelliJ IDEA. Abra a pasta como projeto Gradle.
2. O wrapper não vem incluso: copie `gradlew`, `gradlew.bat` e a pasta `gradle/` do MDK oficial
   (https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) para cá. Se a build reclamar de versão
   do NeoForge ou do plugin `net.neoforged.moddev`, use os valores do `build.gradle`/`gradle.properties` desse MDK.
3. `./gradlew runClient` para testar, `./gradlew build` para gerar o `.jar` em `build/libs/`.

**Já tem um projeto de mod?** Copie `src/main/java/com/exemplo/transformacao`, `src/main/resources/assets/transformacao`,
`transformacao.mixins.json` e adicione o bloco `[[mixins]]` do `neoforge.mods.toml`. Se o seu mod_id for outro,
troque `MODID` em `TransformacaoMod.java` e renomeie a pasta `assets/transformacao`.

## Textura
Sua `.obj` veio sem textura, então gerei uma **provisória** (`textures/entity/armadura.png`) seguindo os UVs do modelo.
Para usar a sua: pinte no Blockbench, exporte o PNG e substitua esse arquivo (mesmo nome). Os UVs do OBJ são
normalizados, então a resolução pode ser qualquer uma. Para regenerar a provisória: `python3 tools/gerar_textura.py`.

## Trocar/editar o modelo
Exporte de novo do Blockbench e substitua `assets/transformacao/models/armor/armadura.obj`. Aperte **F3+T** no jogo
para recarregar sem reiniciar. Cada peça é ligada a uma parte do corpo pelo **nome** (capacete/chifre/olho/dente → cabeça;
peitoral/cinto/saiote → corpo; ombreira/braço/garra → braços; coxa/joelheira/canela/bota → pernas; lado pela posição X).
Se criar peças com outros nomes, adicione a palavra nas listas `*_KEYS` em `ArmorObjModel.java`.

## Ajustes rápidos
Tudo de balanceamento está no topo de `data/ArmorData.java` (XP, nível máximo, bônus, duração do rugido).
Sons, partículas e a sequência do raio estão em `server/TransformationService.java`; a pose do rugido em `client/RoarPose.java`.

## Limitações conhecidas
- Em **primeira pessoa** a armadura não aparece nos braços (só em terceira pessoa e nos outros jogadores).
- Armadura vanilla por cima pode atravessar o modelo (a sua é maior que a do Minecraft).
- O mixin da animação usa `require = 0`: se algum dia a assinatura do método mudar, o mod não crasha, só perde o rugido
  (o log mostra um aviso do Mixin).
