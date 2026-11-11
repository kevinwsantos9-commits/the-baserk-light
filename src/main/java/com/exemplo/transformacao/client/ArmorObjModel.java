package com.exemplo.transformacao.client;

import com.exemplo.transformacao.TransformacaoMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Lê o armadura.obj (exportado do Blockbench) e desenha cada peça presa a uma parte do corpo do jogador,
 * assim a armadura acompanha andar, agachar, nadar, balançar os braços e a animação de rugido.
 *
 * Convenção do seu modelo (conferida no arquivo): frente = -Z, "direita" = -X, pés em y = 0, y para cima,
 * unidades em blocos. O espaço de modelo do Minecraft tem y para baixo com a origem 1,5 bloco acima dos pés,
 * por isso: (x, y, z) -> (x, 1.5 - y, z).
 */
public final class ArmorObjModel {
    public static final ResourceLocation OBJ = TransformacaoMod.id("models/armor/armadura.obj");
    public static final ResourceLocation TEXTURE = TransformacaoMod.id("textures/entity/armadura.png");

    public enum Part { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    /** Pivôs (em blocos) das partes do PlayerModel vanilla. */
    private static final float[][] PIVOT = {
            {0F, 0F, 0F},                       // cabeça
            {0F, 0F, 0F},                       // corpo
            {-5F / 16F, 2F / 16F, 0F},          // braço direito
            {5F / 16F, 2F / 16F, 0F},           // braço esquerdo
            {-1.9F / 16F, 12F / 16F, 0F},       // perna direita
            {1.9F / 16F, 12F / 16F, 0F}         // perna esquerda
    };

    private static final String[] HEAD_KEYS = {"capacete", "mandibula", "chifre", "bochecha", "crista",
            "espinho_traseiro", "orbita", "olho", "sobrancelha", "nariz", "dente", "presa", "helmet", "head", "horn"};
    private static final String[] BODY_KEYS = {"peitoral", "costela", "espinho_peito", "espinho_costas",
            "cinto", "fivela", "saiote", "chest", "belt", "torso"};
    private static final String[] ARM_KEYS = {"ombreira", "braco", "cotovelo", "antebraco", "garra",
            "shoulder", "elbow", "claw", "arm"};
    private static final String[] LEG_KEYS = {"coxa", "joelheira", "canela", "bota", "knee", "boot", "shin", "leg"};

    private static volatile ArmorObjModel current;

    private final EnumMap<Part, List<float[]>> quads = new EnumMap<>(Part.class);
    private int quadCount;

    private ArmorObjModel() {
        for (Part p : Part.values()) quads.put(p, new ArrayList<>());
    }

    public static ArmorObjModel get() {
        return current;
    }

    /** Chamado quando os recursos recarregam (F3+T) e na primeira carga do jogo. */
    public static void reload(ResourceManager manager) {
        try (InputStream in = manager.open(OBJ)) {
            ArmorObjModel model = parse(in);
            current = model;
            com.mojang.logging.LogUtils.getLogger()
                    .info("[transformacao] armadura.obj carregado: {} faces", model.quadCount);
        } catch (Exception e) {
            current = null;
            com.mojang.logging.LogUtils.getLogger().error("[transformacao] falha ao carregar {}", OBJ, e);
        }
    }

    // ------------------------------------------------------------------ parser

    private static ArmorObjModel parse(InputStream in) throws IOException {
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        Map<String, List<int[][]>> objects = new LinkedHashMap<>();
        String current = "default";

        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] t = line.split("\\s+");
                switch (t[0]) {
                    case "v" -> v.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "vt" -> vt.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2])});
                    case "o", "g" -> current = t.length > 1 ? t[1] : "default";
                    case "f" -> {
                        int[][] face = new int[t.length - 1][2];
                        for (int i = 1; i < t.length; i++) {
                            String[] c = t[i].split("/");
                            face[i - 1][0] = index(c[0], v.size());
                            face[i - 1][1] = c.length > 1 && !c[1].isEmpty() ? index(c[1], vt.size()) : -1;
                        }
                        objects.computeIfAbsent(current, k -> new ArrayList<>()).add(face);
                    }
                    default -> { }
                }
            }
        }

        // Se o modelo estiver virado para +Z (nariz com z > 0), gira 180° em Y.
        boolean flip = false;
        for (Map.Entry<String, List<int[][]>> e : objects.entrySet()) {
            String n = e.getKey().toLowerCase(Locale.ROOT);
            if (n.contains("nariz") || n.contains("nose")) {
                flip = centroid(e.getValue(), v)[2] > 0.05F;
                break;
            }
        }

        ArmorObjModel model = new ArmorObjModel();
        for (Map.Entry<String, List<int[][]>> e : objects.entrySet()) {
            float[] c = centroid(e.getValue(), v);
            float cx = flip ? -c[0] : c[0];
            float cz = flip ? -c[2] : c[2];
            Part part = classify(e.getKey().toLowerCase(Locale.ROOT), cx, c[1]);
            for (int[][] face : e.getValue()) {
                model.addFace(part, face, v, vt, new float[]{cx, c[1], cz}, flip);
            }
        }
        return model;
    }

    private static int index(String s, int size) {
        int i = Integer.parseInt(s);
        return i > 0 ? i - 1 : size + i;
    }

    private static float[] centroid(List<int[][]> faces, List<float[]> v) {
        double x = 0, y = 0, z = 0;
        int n = 0;
        for (int[][] f : faces) {
            for (int[] c : f) {
                float[] p = v.get(c[0]);
                x += p[0]; y += p[1]; z += p[2];
                n++;
            }
        }
        if (n == 0) return new float[]{0, 0, 0};
        return new float[]{(float) (x / n), (float) (y / n), (float) (z / n)};
    }

    private static boolean has(String name, String[] keys) {
        for (String k : keys) if (name.contains(k)) return true;
        return false;
    }

    /** Decide a que parte do corpo a peça pertence (pelo nome; se não reconhecer, pela posição). */
    private static Part classify(String name, float cx, float cy) {
        if (has(name, HEAD_KEYS)) return Part.HEAD;
        if (has(name, BODY_KEYS)) return Part.BODY;
        boolean right = cx < 0F; // espaço do modelo: direita do personagem = -X
        if (has(name, ARM_KEYS)) return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
        if (has(name, LEG_KEYS)) return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
        // Fallback por posição
        if (cy >= 1.5F) return Part.HEAD;
        if (cy < 0.75F) return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
        if (Math.abs(cx) > 0.35F) return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
        return Part.BODY;
    }

    private void addFace(Part part, int[][] face, List<float[]> v, List<float[]> vt, float[] objCenter, boolean flip) {
        int n = face.length;
        if (n < 3) return;
        if (n == 4) {
            emit(part, new int[][]{face[0], face[1], face[2], face[3]}, v, vt, objCenter, flip);
        } else {
            for (int i = 1; i < n - 1; i++) {
                emit(part, new int[][]{face[0], face[i], face[i + 1], face[i + 1]}, v, vt, objCenter, flip);
            }
        }
    }

    private void emit(Part part, int[][] f, List<float[]> v, List<float[]> vt, float[] objCenter, boolean flip) {
        float[] pivot = PIVOT[part.ordinal()];
        float[][] p = new float[4][3];
        for (int i = 0; i < 4; i++) {
            float[] src = v.get(f[i][0]);
            p[i][0] = flip ? -src[0] : src[0];
            p[i][1] = src[1];
            p[i][2] = flip ? -src[2] : src[2];
        }

        // normal calculada no espaço do OBJ e orientada para fora da peça
        float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
        float bx = p[2][0] - p[0][0], by = p[2][1] - p[0][1], bz = p[2][2] - p[0][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float fx = (p[0][0] + p[1][0] + p[2][0] + p[3][0]) / 4F - objCenter[0];
        float fy = (p[0][1] + p[1][1] + p[2][1] + p[3][1]) / 4F - objCenter[1];
        float fz = (p[0][2] + p[1][2] + p[2][2] + p[3][2]) / 4F - objCenter[2];
        if (nx * fx + ny * fy + nz * fz < 0F) { nx = -nx; ny = -ny; nz = -nz; }
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-8F) { nx = 0F; ny = 1F; nz = 0F; len = 1F; }
        nx /= len; ny /= len; nz /= len;

        float[] q = new float[23];
        for (int i = 0; i < 4; i++) {
            q[i * 5]     = p[i][0] - pivot[0];
            q[i * 5 + 1] = (1.5F - p[i][1]) - pivot[1];
            q[i * 5 + 2] = p[i][2] - pivot[2];
            if (f[i][1] >= 0) {
                float[] uv = vt.get(f[i][1]);
                q[i * 5 + 3] = uv[0];
                q[i * 5 + 4] = 1F - uv[1];
            }
        }
        // mesma transformação linear da posição aplicada à normal (inverte só o Y)
        q[20] = nx;
        q[21] = -ny;
        q[22] = nz;
        quads.get(part).add(q);
        quadCount++;
    }

    // ------------------------------------------------------------------ desenho

    public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, PlayerModel<?> m) {
        renderPart(Part.HEAD, m.head, pose, vc, light, overlay);
        renderPart(Part.BODY, m.body, pose, vc, light, overlay);
        renderPart(Part.RIGHT_ARM, m.rightArm, pose, vc, light, overlay);
        renderPart(Part.LEFT_ARM, m.leftArm, pose, vc, light, overlay);
        renderPart(Part.RIGHT_LEG, m.rightLeg, pose, vc, light, overlay);
        renderPart(Part.LEFT_LEG, m.leftLeg, pose, vc, light, overlay);
    }

    private void renderPart(Part part, ModelPart anchor, PoseStack pose, VertexConsumer vc, int light, int overlay) {
        List<float[]> list = quads.get(part);
        if (list.isEmpty()) return;

        pose.pushPose();
        anchor.translateAndRotate(pose); // posição + rotação atuais da parte (já com agachar/rugido/etc.)
        PoseStack.Pose last = pose.last();
        for (float[] q : list) {
            for (int i = 0; i < 4; i++) {
                int o = i * 5;
                vc.addVertex(last, q[o], q[o + 1], q[o + 2])
                        .setColor(255, 255, 255, 255)
                        .setUv(q[o + 3], q[o + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(last, q[20], q[21], q[22]);
            }
        }
        pose.popPose();
    }
}
