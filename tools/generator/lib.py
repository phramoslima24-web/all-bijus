import json, math, uuid, base64, io
import numpy as np
from PIL import Image

NS = uuid.UUID('6f1b2a10-0a11-4c1e-9a10-0000000000aa')
def U(*s): return str(uuid.uuid5(NS, '/'.join(str(x) for x in s)))

class Model:
    def __init__(s, id, mats):
        s.id = id; s.mats = mats; s.groups = {}; s.order = []; s.cubes = []; s.anims = []
    def g(s, name, origin, parent=None):
        assert name not in s.groups, name
        s.groups[name] = dict(name=name, origin=tuple(float(v) for v in origin), parent=parent, cubes=[], children=[])
        if parent: s.groups[parent]['children'].append(name)
        s.order.append(name); return name
    def c(s, group, name, frm, size, mat, glow=None, detail=None):
        w, h, d = [max(1, int(round(v))) for v in size]
        cube = dict(name=name, group=group, frm=tuple(float(v) for v in frm), size=(w, h, d), mat=mat, glow=glow, detail=detail)
        s.cubes.append(cube); s.groups[group]['cubes'].append(cube)
    def has(s, n): return n in s.groups

def pack(cubes):
    items = sorted(cubes, key=lambda c: -(c['size'][2] + c['size'][1]))
    for res in (128, 256, 512, 1024):
        x = y = rowh = 0; ok = True
        for c in items:
            w, h, d = c['size']; bw, bh = 2 * (d + w), d + h
            if bw > res: ok = False; break
            if x + bw > res: x = 0; y += rowh; rowh = 0
            if y + bh > res: ok = False; break
            c['uv'] = (x, y); x += bw; rowh = max(rowh, bh)
        if ok: return res
    raise RuntimeError('texture too large')

def pattern(mat, fw, fh, rng):
    yy, xx = np.mgrid[0:fh, 0:fw]; p = mat.get('p', 4); k = mat.get('pat', 'none')
    if k == 'stripe_h': return ((yy // p) % 2).astype(float)
    if k == 'stripe_v': return ((xx // p) % 2).astype(float)
    if k == 'hex':
        row = yy // p; return (((xx + (row % 2) * (p // 2 + 1)) // (p + 1) + row) % 2 == 0).astype(float) * 0.8
    if k == 'spots': return (rng.random((fh, fw)) < mat.get('dens', 0.04)).astype(float)
    if k == 'sand': return (np.sin(xx * 0.45 + np.sin(yy * 0.35) * 2.0) > 0.55).astype(float) * 0.7
    if k == 'dots': return (((xx % p) < 2) & ((yy % p) < 2)).astype(float)
    if k == 'lattice': return (((xx % p) == 0) | ((yy % p) == 0)).astype(float)
    if k == 'cracks':
        return (((xx * 2 + yy + (yy // 5) * 3) % 13 == 0) & (rng.random((fh, fw)) < 0.6)).astype(float)
    return np.zeros((fh, fw))

def region(mat, fw, fh, rng, face):
    base = np.array(mat['base'], float); alt = np.array(mat.get('alt', mat['base']), float)
    if face == 'down' and 'down' in mat: base = np.array(mat['down'], float)
    m = pattern(mat, fw, fh, rng)[..., None]
    col = np.clip(base * (1 - m) + alt * m + rng.normal(0, mat.get('noise', 6), (fh, fw, 1)), 0, 255)
    a = np.full((fh, fw, 1), 255.0)
    if mat.get('pat') == 'lattice' and mat.get('holes'):
        a = np.where(m > 0.5, 255.0, 0.0)
    return np.concatenate([col, a], axis=2).astype(np.uint8)

def paint(model):
    res = pack(model.cubes)
    img = np.zeros((res, res, 4), np.uint8); glow = np.zeros((res, res, 4), np.uint8)
    rng = np.random.default_rng(sum(map(ord, model.id)))
    for c in model.cubes:
        w, h, d = c['size']; u, v = c['uv']; mat = model.mats[c['mat']]
        faces = {'up': (d, 0, w, d), 'down': (d + w, 0, w, d), 'east': (0, d, d, h), 'north': (d, d, w, h), 'west': (d + w, d, d, h), 'south': (2 * d + w, d, w, h)}
        c['faces'] = {}
        for f, (fx, fy, fw, fh) in faces.items():
            reg = region(mat, fw, fh, rng, f)
            if fw > 2 and fh > 2:
                reg[0, :, :3] = (reg[0, :, :3] * 0.82).astype(np.uint8); reg[-1, :, :3] = (reg[-1, :, :3] * 0.82).astype(np.uint8)
                reg[:, 0, :3] = (reg[:, 0, :3] * 0.88).astype(np.uint8); reg[:, -1, :3] = (reg[:, -1, :3] * 0.88).astype(np.uint8)
            img[v + fy:v + fy + fh, u + fx:u + fx + fw] = reg
            c['faces'][f] = [u + fx, v + fy, u + fx + fw, v + fy + fh]
            gl = c['glow']
            if gl:
                gc, mode = gl
                gr = np.zeros((fh, fw, 4), np.uint8)
                if mode == 'all':
                    n = np.clip(np.array(gc, float) + rng.normal(0, 14, (fh, fw, 1)), 0, 255)
                    gr[..., :3] = n.astype(np.uint8); gr[..., 3] = 255
                    img[v + fy:v + fy + fh, u + fx:u + fx + fw, :3] = gr[..., :3]
                elif mode == 'cracks':
                    mm = pattern({'pat': 'cracks'}, fw, fh, rng) > 0
                    gr[mm, :3] = gc; gr[mm, 3] = 255
                glow[v + fy:v + fy + fh, u + fx:u + fx + fw] = gr
            if c['detail'] and f == 'north':
                kind, gc = c['detail']; draw_eyes(img, glow, u + fx, v + fy, fw, fh, kind, gc)
    return res, img, glow

def draw_eyes(img, glow, x0, y0, fw, fh, kind, gc):
    sw = max(2, fw // 6); sh = max(2, fh // 7)
    for cx in (fw * 0.27, fw * 0.73):
        ex = int(x0 + cx - sw / 2); ey = int(y0 + fh * 0.42)
        if kind == 'ring':
            img[ey - sh:ey + 2 * sh, ex - sw // 2:ex + sw + sw // 2, :3] = (35, 25, 20)
        img[ey:ey + sh, ex:ex + sw, :3] = gc; img[ey:ey + sh, ex:ex + sw, 3] = 255
        glow[ey:ey + sh, ex:ex + sw] = (*gc, 255)
        img[ey:ey + sh, ex + sw // 2:ex + sw // 2 + 1, :3] = (10, 10, 10)

def png_bytes(arr):
    b = io.BytesIO(); Image.fromarray(arr, 'RGBA').save(b, 'PNG', optimize=True); return b.getvalue()

def export_bb(model, res, tex_png, glow_png, name):
    elements = []
    def group_node(gn):
        g = model.groups[gn]; ch = []
        for c in g['cubes']:
            eu = U(model.id, 'cube', c['name']); w, h, d = c['size']; f = c['frm']
            elements.append({'name': c['name'], 'box_uv': True, 'rescale': False, 'locked': False, 'light_emission': 0, 'render_order': 'default',
                'from': [f[0], f[1], f[2]], 'to': [f[0] + w, f[1] + h, f[2] + d], 'autouv': 0, 'color': 0, 'origin': list(g['origin']),
                'uv_offset': list(c['uv']), 'faces': {k: {'uv': v, 'texture': 0} for k, v in c['faces'].items()}, 'type': 'cube', 'uuid': eu})
            ch.append(eu)
        for cn in g['children']: ch.append(group_node(cn))
        return dict(name=gn, origin=list(g['origin']), rotation=[0, 0, 0], color=0, uuid=U(model.id, 'group', gn), export=True, isOpen=False, locked=False, visibility=True, autouv=0, children=ch)
    outl = [group_node(n) for n in model.order if model.groups[n]['parent'] is None]
    anims = []
    for a in model.anims:
        animators = {}
        for gn, chans in a['tracks'].items():
            kfs = []
            for ch, pts in chans.items():
                for i, (t, v) in enumerate(pts):
                    kfs.append(dict(channel=ch, data_points=[dict(x=round(v[0], 3), y=round(v[1], 3), z=round(v[2], 3))], uuid=U(model.id, a['name'], gn, ch, i), time=round(t, 4), color=-1, interpolation='linear'))
            animators[U(model.id, 'group', gn)] = dict(name=gn, type='bone', keyframes=kfs)
        anims.append(dict(uuid=U(model.id, 'anim', a['name']), name=a['name'], loop=a['loop'], override=False, length=a['length'], snapping=24, selected=False,
                          anim_time_update='', blend_weight='', start_delay='', loop_delay='', animators=animators))
    def tex(i, nm, png):
        return dict(path='', name=nm, folder='', namespace='', id=str(i), particle=False, render_mode='default', render_sides='auto', frame_time=1,
                    frame_order_type='loop', frame_order='', frame_interpolate=False, visible=True, internal=True, saved=True, uuid=U(model.id, 'tex', i),
                    relative_path='', source='data:image/png;base64,' + base64.b64encode(png).decode())
    return dict(meta=dict(format_version='4.9', model_format='free', box_uv=True), name=name, model_identifier=model.id, visible_box=[4, 4, 2],
                variable_placeholders='', variable_placeholder_buttons=[], unhandled_root_fields={}, resolution=dict(width=res, height=res),
                elements=elements, outliner=outl, textures=[tex(0, model.id + '.png', tex_png), tex(1, model.id + '_glow.png', glow_png)], animations=anims)

# ------------------------------------------------ animation helpers
def ev(v, t): return v(t) if callable(v) else v
def R(x=0, y=0, z=0): return lambda t: (ev(x, t), ev(y, t), ev(z, t))
def S(amp, period, phase=0.0, off=0.0): return lambda t: amp * math.sin(2 * math.pi * t / period + phase) + off
def K(*pts):
    def f(t):
        if t <= pts[0][0]: return pts[0][1]
        for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
            if t <= t1: return v0 + (v1 - v0) * (t - t0) / max(1e-6, t1 - t0)
        return pts[-1][1]
    return f

class Anim:
    def __init__(s, model, name, length, loop):
        s.m = model; s.name = name; s.length = length; s.loop = loop; s.tracks = {}
    def t(s, group, chan, fn):
        if not s.m.has(group): return s
        s.tracks.setdefault(group, {}).setdefault(chan, []).append(fn); return s
    def rot(s, g, fn): return s.t(g, 'rotation', fn)
    def pos(s, g, fn): return s.t(g, 'position', fn)
    def done(s):
        n = max(8, min(60, int(round(s.length / 0.05)))); times = [s.length * i / n for i in range(n + 1)]
        out = {}
        for g, chans in s.tracks.items():
            out[g] = {}
            for ch, fns in chans.items():
                pts = []
                for t in times:
                    v = [0.0, 0.0, 0.0]
                    for fn in fns:
                        r = fn(t)
                        for i in range(3): v[i] += r[i]
                    pts.append((t, tuple(v)))
                out[g][ch] = pts
        s.m.anims.append(dict(name=s.name, length=s.length, loop=s.loop, tracks=out))
