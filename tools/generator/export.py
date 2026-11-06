import json, os, sys
sys.path.insert(0, '.')
from models import *; from anims import *
PROJ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src/main/resources/assets/narutoto_allbijuu')
os.makedirs(f'{PROJ}/models/entity', exist_ok=True); os.makedirs(f'{PROJ}/textures/entity', exist_ok=True)
NAMES = dict(shukaku='Shukaku (Ichibi)', matatabi='Matatabi (Nibi)', isobu='Isobu (Sanbi)', songoku='Son Goku (Yonbi)', kokuo='Kokuo (Gobi)', saiken='Saiken (Rokubi)', chomei='Chomei (Nanabi)', gyuki='Gyuki (Hachibi)')
MODELS = {}
for k, f in BUILD.items():
    m = f(); build_all(m); res, img, glow = paint(m)
    tp = png_bytes(img); gp = png_bytes(glow)
    open(f'{PROJ}/textures/entity/bijuu_{k}.png', 'wb').write(tp); open(f'{PROJ}/textures/entity/bijuu_{k}_glow.png', 'wb').write(gp)
    bb = export_bb(m, res, tp, gp, NAMES[k])
    json.dump(bb, open(f'{PROJ}/models/entity/{k}.bbmodel', 'w'), separators=(',', ':'))
    MODELS[k] = (m, img)
    print(k, 'cubes', len(m.cubes), 'bones', len(m.groups), 'anims', len(m.anims), 'tex', res, os.path.getsize(f'{PROJ}/models/entity/{k}.bbmodel') // 1024, 'KB')
