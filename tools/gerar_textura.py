#!/usr/bin/env python3
"""
Gera uma textura PROVISORIA (armadura.png) a partir do mapeamento UV do armadura.obj.
Serve so para o mod funcionar de primeira. Depois pinte a sua no Blockbench
(Texture > Export) e substitua o PNG em:
  src/main/resources/assets/transformacao/textures/entity/armadura.png
O OBJ usa UV normalizado (0..1), entao a textura pode ter QUALQUER resolucao.

Uso: python3 tools/gerar_textura.py [caminho.obj] [saida.png] [tamanho]
"""
import sys, random, collections
from PIL import Image, ImageDraw

obj_path = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/transformacao/models/armor/armadura.obj"
out_path = sys.argv[2] if len(sys.argv) > 2 else "src/main/resources/assets/transformacao/textures/entity/armadura.png"
SIZE = int(sys.argv[3]) if len(sys.argv) > 3 else 128

V, VT, objs, cur = [], [], collections.OrderedDict(), None
for line in open(obj_path, encoding="utf-8", errors="ignore"):
    p = line.split()
    if not p: continue
    if p[0] == "v": V.append(tuple(map(float, p[1:4])))
    elif p[0] == "vt": VT.append(tuple(map(float, p[1:3])))
    elif p[0] in ("o", "g"): cur = p[1]; objs.setdefault(cur, [])
    elif p[0] == "f":
        objs[cur].append([tuple(int(x) if x else 0 for x in t.split("/")) for t in p[1:]])

def palette(name):
    n = name.lower()
    if "olho" in n:                       return (255, 190, 30)    # olhos brilhantes
    if any(k in n for k in ("dente", "presa", "garra")): return (232, 224, 200)  # marfim
    if "chifre" in n or "espinho" in n or "crista" in n: return (205, 190, 160)  # osso
    if "fivela" in n:                     return (212, 160, 40)    # dourado
    if "cinto" in n or "saiote" in n:     return (92, 52, 36)      # couro
    if "sobrancelha" in n or "orbita" in n: return (60, 14, 14)
    if "bota" in n:                       return (34, 30, 38)
    if any(k in n for k in ("ombreira", "joelheira", "cotovelo")): return (120, 24, 24)  # vermelho escuro
    return (58, 54, 66)                                            # placas escuras

img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
rnd = random.Random(7)
for name, faces in objs.items():
    base = palette(name)
    for f in faces:
        uv = [VT[t[1] - 1] for t in f]
        pts = [(u * SIZE, (1 - v) * SIZE) for u, v in uv]
        x0 = int(min(p[0] for p in pts)); x1 = int(max(p[0] for p in pts + [(0, 0)]) if False else max(p[0] for p in pts))
        y0 = int(min(p[1] for p in pts)); y1 = int(max(p[1] for p in pts))
        # sombreamento simples: normal no espaco do OBJ
        a, b, c = (V[t[0] - 1] for t in f[:3])
        ux, uy, uz = (b[i] - a[i] for i in range(3)); vx, vy, vz = (c[i] - a[i] for i in range(3))
        ny = uz * vx - ux * vz
        shade = 1.12 if ny > 1e-9 else (0.78 if ny < -1e-9 else 0.95)
        for y in range(max(y0, 0), min(y1 + 1, SIZE)):
            for x in range(max(x0, 0), min(x1 + 1, SIZE)):
                k = shade * (0.88 + rnd.random() * 0.24)
                col = tuple(max(0, min(255, int(ch * k))) for ch in base)
                d.point((x, y), fill=col + (255,))
img.save(out_path)
print("textura gerada:", out_path, img.size)
