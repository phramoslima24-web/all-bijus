import math, re
from lib import *

TAU = 2 * math.pi
def P(*pts):
    fx = K(*[(p[0], p[1]) for p in pts]); fy = K(*[(p[0], p[2]) for p in pts]); fz = K(*[(p[0], p[3]) for p in pts])
    return lambda t: (fx(t), fy(t), fz(t))
def sn(amp, cycles, L, phase=0.0, off=0.0): return S(amp, L / cycles, phase, off)
def pos_y(f): return lambda t: (0, f(t), 0)

FAM = dict(shukaku='biped', songoku='biped', gyuki='tent', matatabi='quad', kokuo='quad', isobu='quad', saiken='slug', chomei='insect')
SC = dict(shukaku=1.4, songoku=1.2, gyuki=1.3, matatabi=0.9, kokuo=0.95, isobu=1.0, saiken=1.0, chomei=1.0)
SKILLS = dict(shukaku=['SHIELD', 'ZONE', 'ZONE', 'CONE'], matatabi=['CONE', 'DASH', 'ZONE', 'AOE'], isobu=['DASH', 'PROJ', 'ZONE', 'CONE'],
              songoku=['PROJ', 'CONE', 'ZONE', 'AOE'], kokuo=['CONE', 'DASH', 'ZONE', 'AOE'], saiken=['PROJ', 'ZONE', 'SHIELD', 'BARRAGE'],
              chomei=['CONE', 'FLIGHT', 'BARRAGE', 'ZONE'], gyuki=['CONE', 'DASH', 'ZONE', 'AOE'])

def chains(m):
    out = []
    for pre in ('tail', 'tent'):
        ks = sorted({int(n.split('_')[0][len(pre):]) for n in m.groups if re.fullmatch(pre + r'\d+_\d+', n)})
        for k in ks:
            seg = sorted([n for n in m.groups if re.fullmatch(f'{pre}{k}_\\d+', n)], key=lambda n: int(n.split('_')[1]))
            out.append(seg)
    return out

class Rig:
    def __init__(s, m):
        s.m = m; s.fam = FAM[m.id]; s.sc = SC[m.id]; s.ch = chains(m)
        h = m.has
        if s.fam == 'quad':
            s.up = ['leg_fl', 'leg_fr']; s.lo = [('shin_fl' if h('shin_fl') else 'paw_fl'), ('shin_fr' if h('shin_fr') else 'paw_fr')]
            s.hind = ['leg_bl', 'leg_br']; s.hlo = [('shin_bl' if h('shin_bl') else 'paw_bl'), ('shin_br' if h('shin_br') else 'paw_br')]
        elif s.fam == 'insect':
            s.up = ['leg0_l', 'leg0_r']; s.lo = ['shin0_l', 'shin0_r']; s.hind = ['leg2_l', 'leg2_r']; s.hlo = ['shin2_l', 'shin2_r']
        elif s.fam == 'slug':
            s.up = ['arm_l', 'arm_r']; s.lo = ['hand_l', 'hand_r']; s.hind = []; s.hlo = []
        elif s.fam == 'tent':
            s.up = ['arm_l', 'arm_r']; s.lo = ['forearm_l', 'forearm_r']; s.hind = []; s.hlo = []
        else:
            s.up = ['arm_l', 'arm_r']; s.lo = ['forearm_l', 'forearm_r']; s.hind = ['leg_l', 'leg_r']; s.hlo = ['foot_l', 'foot_r']
        s.neck = [n for n in ('neck', 'neck2') if h(n)]
        s.segs = [f'seg{i}' for i in range(6) if h(f'seg{i}')]
        s.abd = [f'abd{i}' for i in range(3) if h(f'abd{i}')]
        s.wings = [n for n in ('wing_fl', 'wing_fr', 'wing_hl', 'wing_hr') if h(n)]

def tails(a, r, L, cyc, ay, ax=0.0, lead=0.7, base_x=0.0, spread=0.0, fn=None):
    n = len(r.ch)
    for k, seg in enumerate(r.ch):
        for j, g in enumerate(seg):
            ph = -j * lead + k * 0.9
            a.rot(g, R(x=sn(ax, cyc, L, ph + 1.3, base_x), y=lambda t, ph=ph, j=j, k=k: (spread * (k - (n - 1) / 2) if j == 0 else 0) + ay * math.sin(TAU * cyc * t / L + ph)))
            if fn: fn(a, g, j, k)

def head_idle(a, r, L, amp=2.0):
    a.rot('head', R(x=sn(amp, 1, L, 1.0), y=sn(amp * 0.8, 1, L, 0.2)))
    for n in r.neck: a.rot(n, R(x=sn(amp * 0.5, 1, L, 0.6)))
    a.rot('jaw', R(x=sn(2.0, 1, L, 0.0, 2.0)))

def idle(m, r):
    L = 3.0; a = Anim(m, 'idle', L, True); sc = r.sc
    a.pos('body', pos_y(sn(0.7 * sc, 1, L, 0)))
    a.rot('body', R(x=sn(1.2, 1, L, 0.4)))
    head_idle(a, r, L)
    for i, g in enumerate(r.up):
        side = 1 if i == 0 else -1
        a.rot(g, R(x=sn(2.5, 1, L, 0.8 + i * 0.4), z=sn(1.5 * side, 1, L, 0.2)))
    for g in r.lo: a.rot(g, R(x=sn(2.0, 1, L, 1.4)))
    for i, g in enumerate(r.segs): a.rot(g, R(x=sn(1.8, 1, L, -i * 0.7)))
    for i, g in enumerate(r.abd): a.rot(g, R(x=sn(2.5, 1, L, -i * 0.8)))
    flick = r.m.id == 'matatabi'
    tails(a, r, L, 3 if flick else 1, 9 if flick else 7, ax=4 if flick else 3, base_x=14 if r.fam != 'tent' else 0)
    if r.fam == 'tent':
        for k, seg in enumerate(r.ch):
            for j, g in enumerate(seg): a.rot(g, R(x=sn(3, 1, L, k * 0.8 - j * 0.5)))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1
        a.rot(g, R(y=sd * 55, z=lambda t, sd=sd: sd * 2 * math.sin(TAU * 2 * t / L)))
    if r.m.id == 'chomei':
        for i in range(3):
            for sd, n in ((1, 'l'), (-1, 'r')):
                a.rot(f'leg{i}_{n}', R(y=sd * (i - 1) * 8))
        a.rot('horn', R(x=sn(2, 1, L)))
    a.done()

def walk(m, r, run=False):
    L = 0.6 if run else 1.0; a = Anim(m, 'run' if run else 'walk', L, True); sc = r.sc
    la = 46 if run else 26; aa = 40 if run else 18
    lean = 9 if run else 3
    a.rot('body', R(x=lean, z=sn(3.0 if run else 2.0, 1, L)))
    a.pos('body', pos_y(sn((2.8 if run else 1.4) * sc, 2, L, 1.57)))
    a.rot('head', R(x=sn(-3 if not run else -5, 2, L, 0.5, -lean * 0.8), y=sn(-3, 1, L)))
    for n in r.neck: a.rot(n, R(x=sn(2.5, 2, L, 0.8)))
    if r.fam == 'biped':
        for i, (u, lo, ul, ll) in enumerate(zip(r.hind, r.hlo, r.up, r.lo)):
            ph = i * math.pi
            a.rot(u, R(x=sn(la, 1, L, ph)))
            a.rot(lo, R(x=lambda t, ph=ph: 0.5 * la * (math.sin(TAU * t / L + ph + 1.0) + 1.0) * 0.5 - 0.0))
            a.rot(ul, R(x=sn(aa, 1, L, ph + math.pi), z=0))
            a.rot(ll, R(x=sn(-aa * 0.6 if run else -aa * 0.3, 1, L, ph + math.pi, -aa * (1.1 if run else 0.2))))
    elif r.fam == 'quad':
        for i, (u, lo) in enumerate(zip(r.up, r.lo)):
            ph = i * math.pi
            a.rot(u, R(x=sn(la, 1, L, ph)))
            a.rot(lo, R(x=lambda t, ph=ph: -0.6 * la * max(0.0, math.sin(TAU * t / L + ph + 1.6))))
        for i, (u, lo) in enumerate(zip(r.hind, r.hlo)):
            ph = (1 - i) * math.pi
            a.rot(u, R(x=sn(la, 1, L, ph)))
            a.rot(lo, R(x=lambda t, ph=ph: 0.7 * la * max(0.0, math.sin(TAU * t / L + ph + 1.6))))
        for g in ('paw_fl', 'paw_fr', 'paw_bl', 'paw_br'):
            if m.has('shin' + g[3:]): a.rot(g, R(x=sn(la * 0.25, 1, L, 0.6)))
    elif r.fam == 'tent':
        for i, (ul, ll) in enumerate(zip(r.up, r.lo)):
            ph = i * math.pi
            a.rot(ul, R(x=sn(aa, 1, L, ph)))
            a.rot(ll, R(x=sn(-aa * 0.5, 1, L, ph, -aa * 0.8)))
        for k, seg in enumerate(r.ch):
            for j, g in enumerate(seg):
                a.rot(g, R(x=sn(la * (0.9 if j == 0 else 0.5), 1, L, k * math.pi / 4 * 2 - j * 0.9), z=sn(6, 1, L, k * 1.2 - j * 0.7)))
        a.pos('body', pos_y(sn(2.2 * sc, 1, L, 0.4)))
    elif r.fam == 'slug':
        a.pos('body', pos_y(sn(0.4 * sc, 1, L)))
        for i, g in enumerate(r.segs): a.rot(g, R(x=sn(9 if run else 6, 1, L, -i * 0.95), y=sn(5 if run else 3, 1, L, -i * 0.95 + 1.5)))
        for i, (u, lo) in enumerate(zip(r.up, r.lo)):
            a.rot(u, R(x=sn(22 if run else 12, 1, L, i * math.pi)))
        for sd in ('l', 'r'):
            a.rot(f'stalk_{sd}', R(x=sn(6, 1, L, 0.4)))
            a.rot(f'stalk2_{sd}', R(x=sn(8, 1, L, 1.0)))
    elif r.fam == 'insect':
        for i in range(3):
            for s_, n in ((1, 'l'), (-1, 'r')):
                ph = ((i + (0 if n == 'l' else 1)) % 2) * math.pi
                amp = 24 if not run else 34
                a.rot(f'leg{i}_{n}', R(y=lambda t, ph=ph, s_=s_, i=i: s_ * (amp * math.sin(TAU * t / L + ph) + (i - 1) * 8),
                                      z=lambda t, ph=ph, s_=s_: s_ * 14 * max(0.0, math.sin(TAU * t / L + ph + 1.57))))
                a.rot(f'shin{i}_{n}', R(z=lambda t, ph=ph, s_=s_: s_ * 8 * max(0.0, math.sin(TAU * t / L + ph + 1.57))))
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1
            a.rot(g, R(y=sd * 50, z=lambda t, sd=sd: sd * 4 * math.sin(TAU * 3 * t / L)))
        for i, g in enumerate(r.abd): a.rot(g, R(x=sn(5, 1, L, -i * 0.8, 6)))
    tails(a, r, L, 1, 14 if run else 8, ax=5, base_x=(2 if run else 10) if r.fam != 'tent' else 0, lead=0.6)
    if r.fam != 'tent' and r.fam != 'slug':
        pass
    a.done()

def fly(m, r):
    L = 0.5; a = Anim(m, 'fly', L, True)
    a.pos('body', pos_y(sn(2.5, 1, L, 1.57)))
    a.rot('body', R(x=sn(2, 1, L, 0.0, -6)))
    a.rot('head', R(x=sn(2, 1, L, 1.0, 6)))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1; hind = g.startswith('wing_h')
        ph = 0.9 if hind else 0.0
        a.rot(g, R(y=sd * 6, z=sn(sd * 62, 1, L, ph)))
    for i, g in enumerate(r.abd): a.rot(g, R(x=sn(5, 1, L, -i * 0.8 + 1.0, 14 + i * 5)))
    for i in range(3):
        for sd, n in ((1, 'l'), (-1, 'r')):
            a.rot(f'leg{i}_{n}', R(y=sd * ((i - 1) * 6 + sn(3, 1, L, i)(0) * 0), z=sd * -10))
            a.rot(f'shin{i}_{n}', R(z=sd * -14))
    tails(a, r, L, 2, 6, ax=3, base_x=12)
    for g in ('mand_l', 'mand_r'): a.rot(g, R(y=sn(6, 2, L)))
    a.done()

def weak(m, r):
    L = 3.5; a = Anim(m, 'weak', L, True); sc = r.sc
    a.pos('body', R(y=lambda t: (-9 * sc + 0.6 * math.sin(TAU * t / L * 1)) , z=0))
    a.rot('body', R(x=sn(1.5, 1, L, 0, 8)))
    a.rot('head', R(x=sn(3, 1, L, 0.5, 38), y=sn(5, 1, L)))
    for n in r.neck: a.rot(n, R(x=12))
    a.rot('jaw', R(x=sn(3, 1, L, 0, 12)))
    for i, g in enumerate(r.up): a.rot(g, R(x=sn(2, 1, L, i, 14)))
    for g in r.lo: a.rot(g, R(x=-18))
    for g in r.hind: a.rot(g, R(x=-24))
    for g in r.hlo: a.rot(g, R(x=36))
    for i, g in enumerate(r.segs): a.rot(g, R(x=sn(1.5, 1, L, -i * 0.5, -3)))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1; a.rot(g, R(y=sd * 70, z=sd * -20))
    tails(a, r, L, 1, 3, ax=0, base_x=-14 if r.fam != 'tent' else 0)
    if r.fam == 'tent':
        for k, seg in enumerate(r.ch):
            for j, g in enumerate(seg): a.rot(g, R(x=sn(2, 1, L, k + j * 0.4, 10)))
    a.done()

def hurt(m, r):
    L = 0.4; a = Anim(m, 'hurt', L, False); sc = r.sc
    a.rot('body', lambda t: (K((0, 0), (0.08, -9), (0.4, 0))(t), 0, K((0, 0), (0.08, 4), (0.2, -3), (0.4, 0))(t)))
    a.pos('body', lambda t: (0, 0, K((0, 0), (0.08, 5 * sc), (0.4, 0))(t)))
    a.rot('head', lambda t: (K((0, 0), (0.08, -22), (0.4, 0))(t), K((0, 0), (0.1, 8), (0.25, -5), (0.4, 0))(t), 0))
    a.rot('jaw', lambda t: (K((0, 0), (0.08, 30), (0.4, 0))(t), 0, 0))
    for i, g in enumerate(r.up): a.rot(g, lambda t, i=i: (K((0, 0), (0.08, 20), (0.4, 0))(t), 0, K((0, 0), (0.1, 10 * (1 if i == 0 else -1)), (0.4, 0))(t)))
    for n in r.neck: a.rot(n, lambda t: (K((0, 0), (0.08, -10), (0.4, 0))(t), 0, 0))
    for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (K((0, 0), (0.1, -5 + i), (0.4, 0))(t), 0, 0))
    if r.m.id == 'isobu':
        a.pos('neck', lambda t: (0, 0, K((0, 0), (0.1, 6), (0.4, 0))(t)))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * K((0, 0), (0.1, 25), (0.4, 0))(t), sd * K((0, 0), (0.1, 30), (0.4, 0))(t)))
    tails(a, r, L, 1, 14, ax=10, base_x=0, lead=0.5)
    a.done()

def death(m, r):
    L = 1.0; a = Anim(m, 'death', L, False); sc = r.sc
    f = lambda *p: K(*p)
    a.pos('body', lambda t: (0, f((0, 0), (0.3, -4 * sc), (0.7, -14 * sc), (1.0, -17 * sc))(t), f((0, 0), (0.3, 4 * sc), (1.0, 6 * sc))(t)))
    a.rot('body', lambda t: (f((0, 0), (0.25, -14), (0.7, 6), (1.0, 8))(t), 0, f((0, 0), (0.5, 12), (1.0, 78))(t)))
    a.rot('head', lambda t: (f((0, 0), (0.3, -28), (0.7, 20), (1.0, 34))(t), f((0, 0), (1.0, 18))(t), f((0, 0), (1.0, -14))(t)))
    a.rot('jaw', lambda t: (f((0, 0), (0.3, 36), (1.0, 20))(t), 0, 0))
    for n in r.neck: a.rot(n, lambda t: (f((0, 0), (0.6, 10), (1.0, 18))(t), 0, 0))
    for i, g in enumerate(r.up): a.rot(g, lambda t, i=i: (f((0, 0), (0.3, -40), (1.0, 10))(t), 0, f((0, 0), (1.0, 22 * (1 if i == 0 else -1)))(t)))
    for g in r.lo: a.rot(g, lambda t: (f((0, 0), (1.0, -30))(t), 0, 0))
    for i, g in enumerate(r.hind): a.rot(g, lambda t, i=i: (f((0, 0), (1.0, -34 if i == 0 else -10))(t), 0, 0))
    for g in r.hlo: a.rot(g, lambda t: (f((0, 0), (1.0, 40))(t), 0, 0))
    for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (0.5, -4), (1.0, 6))(t), 0, f((0, 0), (1.0, 5))(t)))
    for g in r.abd: a.rot(g, lambda t: (f((0, 0), (1.0, -18))(t), 0, 0))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * f((0, 0), (1.0, 80))(t), sd * f((0, 0), (0.5, 30), (1.0, -34))(t)))
    for i in range(3):
        for sd, n in ((1, 'l'), (-1, 'r')):
            a.rot(f'leg{i}_{n}', lambda t, sd=sd: (0, 0, sd * f((0, 0), (1.0, -40))(t)))
    for k, seg in enumerate(r.ch):
        for j, g in enumerate(seg):
            a.rot(g, lambda t, j=j, k=k: (f((0, 0), (0.5, -10), (1.0, -16 if r.fam != 'tent' else 6))(t), f((0, 0), (1.0, (k - len(r.ch) / 2) * 3))(t), 0))
    a.done()

# ---------------- generic action helpers
def tail_flare(a, r, pts_x, pts_y=None, spread=0.0, lead=0.3):
    n = len(r.ch)
    for k, seg in enumerate(r.ch):
        for j, g in enumerate(seg):
            fx = K(*pts_x); fy = K(*(pts_y or [(0, 0), (99, 0)]))
            a.rot(g, lambda t, fx=fx, fy=fy, j=j, k=k: (fx(t), fy(t) + (spread * (k - (n - 1) / 2) if j == 0 else 0) * min(1, t * 2), 0))

def attack(m, r):
    L = 0.8; a = Anim(m, 'attack', L, False); sc = r.sc; f = K
    id = m.id
    jaw = lambda t: (f((0, 0), (0.28, 8), (0.42, 42), (0.55, 4), (0.8, 0))(t), 0, 0)
    a.rot('jaw', jaw)
    if r.fam in ('biped', 'tent'):
        # soco/garra em arco: braco direito levanta e desce, esquerdo apoia
        a.rot(r.up[0], lambda t: (f((0, 0), (0.3, 55), (0.45, -105), (0.6, -95), (0.8, 0))(t), 0, f((0, 0), (0.3, -12), (0.45, 6), (0.8, 0))(t)))
        a.rot(r.lo[0], lambda t: (f((0, 0), (0.3, -35), (0.45, -15), (0.8, 0))(t), 0, 0))
        a.rot(r.up[1], lambda t: (f((0, 0), (0.3, 20), (0.45, -35), (0.8, 0))(t), 0, 0))
        a.rot('body', lambda t: (f((0, 0), (0.3, -8), (0.45, 16), (0.65, 10), (0.8, 0))(t), f((0, 0), (0.3, 10), (0.45, -12), (0.8, 0))(t), 0))
        a.pos('body', lambda t: (0, f((0, 0), (0.45, -4 * sc), (0.8, 0))(t), f((0, 0), (0.3, 3 * sc), (0.45, -8 * sc), (0.8, 0))(t)))
        a.rot('head', lambda t: (f((0, 0), (0.3, -12), (0.45, 18), (0.8, 0))(t), 0, 0))
        if r.fam == 'biped': a.rot('leg_l', lambda t: (f((0, 0), (0.45, -22), (0.8, 0))(t), 0, 0)); a.rot('leg_r', lambda t: (f((0, 0), (0.45, 16), (0.8, 0))(t), 0, 0))
        if r.fam == 'tent':
            for k, seg in enumerate(r.ch):
                for j, g in enumerate(seg):
                    a.rot(g, lambda t, k=k, j=j: (f((0, 0), (0.3, 10), (0.5, -26 + j * 6), (0.8, 0))(t), f((0, 0), (0.3, -20 * (1 if k % 2 else -1)), (0.5, 30 * (1 if k % 2 else -1)), (0.8, 0))(t), 0))
    elif r.fam == 'quad':
        a.rot('body', lambda t: (f((0, 0), (0.25, -22), (0.45, 16), (0.65, 8), (0.8, 0))(t), 0, 0))
        a.pos('body', lambda t: (0, f((0, 0), (0.25, 5 * sc), (0.45, -2), (0.8, 0))(t), f((0, 0), (0.25, 3), (0.45, -10 * sc), (0.8, 0))(t)))
        a.rot(r.up[0], lambda t: (f((0, 0), (0.25, -65), (0.45, 38), (0.8, 0))(t), 0, 0)); a.rot(r.up[1], lambda t: (f((0, 0), (0.28, -50), (0.48, 30), (0.8, 0))(t), 0, 0))
        for g in r.lo: a.rot(g, lambda t: (f((0, 0), (0.25, -25), (0.45, 10), (0.8, 0))(t), 0, 0))
        for g in r.hind: a.rot(g, lambda t: (f((0, 0), (0.25, 20), (0.45, -12), (0.8, 0))(t), 0, 0))
        a.rot('head', lambda t: (f((0, 0), (0.25, -20), (0.45, 28), (0.8, 0))(t), 0, 0))
        for n in r.neck: a.rot(n, lambda t: (f((0, 0), (0.25, -14), (0.45, 22), (0.8, 0))(t), 0, 0))
        if id == 'isobu':
            a.pos('neck', lambda t: (0, f((0, 0), (0.25, 0), (0.45, 0), (0.8, 0))(t), f((0, 0), (0.25, 5), (0.45, -9), (0.8, 0))(t)))
    elif r.fam == 'slug':
        for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (0.28, -14 - i), (0.45, 12), (0.8, 0))(t), 0, 0))
        a.rot(r.up[0], lambda t: (f((0, 0), (0.28, -50), (0.45, 25), (0.8, 0))(t), 0, 0)); a.rot(r.up[1], lambda t: (f((0, 0), (0.3, -40), (0.48, 20), (0.8, 0))(t), 0, 0))
        a.rot('head', lambda t: (f((0, 0), (0.28, -14), (0.45, 22), (0.8, 0))(t), 0, 0))
        for sd in ('l', 'r'): a.rot(f'stalk_{sd}', lambda t: (f((0, 0), (0.45, 22), (0.8, 0))(t), 0, 0))
    elif r.fam == 'insect':
        a.rot('abd0', lambda t: (f((0, 0), (0.3, -8), (0.45, 38), (0.8, 0))(t), 0, 0)); a.rot('abd1', lambda t: (f((0, 0), (0.3, -6), (0.45, 32), (0.8, 0))(t), 0, 0)); a.rot('abd2', lambda t: (f((0, 0), (0.3, -5), (0.45, 28), (0.8, 0))(t), 0, 0))
        a.rot('body', lambda t: (f((0, 0), (0.3, -8), (0.45, 12), (0.8, 0))(t), 0, 0))
        a.rot('head', lambda t: (f((0, 0), (0.3, -14), (0.45, 16), (0.8, 0))(t), 0, 0))
        for sd, n in ((1, 'l'), (-1, 'r')):
            a.rot(f'mand_{n}', lambda t, sd=sd: (0, sd * f((0, 0), (0.3, -22), (0.5, 12), (0.8, 0))(t), 0))
            a.rot(f'leg0_{n}', lambda t, sd=sd: (0, sd * f((0, 0), (0.3, -30), (0.45, 30), (0.8, 0))(t), 0))
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1; a.rot(g, R(y=sd * 40, z=lambda t, sd=sd: sd * f((0, 0), (0.45, 20), (0.8, 0))(t)))
    if r.fam != 'tent':
        tail_flare(a, r, [(0, 0), (0.3, 10), (0.45, 22), (0.8, 0)], [(0, 0), (0.3, 20), (0.5, -26), (0.8, 0)], spread=6)
    a.done()

def dama(m, r):
    L = 3.0; a = Anim(m, 'dama', L, False); sc = r.sc; f = K
    a.rot('jaw', lambda t: (f((0, 0), (0.6, 22), (2.7, 48), (2.82, 6), (3.0, 4))(t), 0, 0))
    a.rot('head', lambda t: (f((0, 0), (0.8, -22), (2.7, -34), (2.82, 22), (3.0, 12))(t), f((0, 0), (1.5, 0), (2.7, 4))(t), 0))
    for n in r.neck: a.rot(n, lambda t: (f((0, 0), (0.8, -12), (2.7, -18), (2.82, 14), (3.0, 6))(t), 0, 0))
    a.rot('body', lambda t: (f((0, 0), (0.8, -8), (2.7, -16), (2.82, 14), (3.0, 6))(t), 0, 0))
    a.pos('body', lambda t: (0, f((0, 0), (0.8, 1 * sc), (2.7, 4 * sc), (2.82, -3 * sc), (3.0, 0))(t) + 0.5 * math.sin(t * 22) * min(1, t / 2.7) * (t < 2.8), f((0, 0), (2.7, 4 * sc), (2.82, -8 * sc), (3.0, -4 * sc))(t)))
    for i, g in enumerate(r.up):
        sd = 1 if i == 0 else -1
        a.rot(g, lambda t, sd=sd: (f((0, 0), (0.8, -30), (2.7, -50), (2.82, -10), (3.0, 0))(t), 0, sd * f((0, 0), (0.8, 8), (2.7, 28), (3.0, 12))(t)))
    for g in r.lo: a.rot(g, lambda t: (f((0, 0), (0.8, -30), (2.7, -40), (3.0, 0))(t), 0, 0))
    for g in r.hind: a.rot(g, lambda t: (f((0, 0), (0.8, 6), (2.7, 14), (3.0, 0))(t), 0, 0))
    for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (2.7, -12 - i), (3.0, 0))(t), 0, 0))
    for g in r.abd: a.rot(g, lambda t: (f((0, 0), (2.7, 24), (3.0, 8))(t), 0, 0))
    for g in r.wings:
        sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * 10, sd * (25 * math.sin(t * 16) * min(1, t / 2.6))))
    tail_flare(a, r, [(0, 0), (0.8, 30), (2.7, 52 if r.fam != 'tent' else 20), (2.82, 20), (3.0, 10)], [(0, 0), (2.7, 0)], spread=(16 if r.fam != 'tent' else 24))
    if r.fam == 'tent':
        for k, seg in enumerate(r.ch):
            for j, g in enumerate(seg): a.rot(g, lambda t, k=k, j=j: (0, 0, 8 * math.sin(t * 12 + k + j) * min(1, t / 2.6)))
    a.done()

def skill(m, r, idx, typ):
    L = 1.0; a = Anim(m, f'skill{idx + 1}', L, False); sc = r.sc; f = K
    ups = r.up
    def arms(pts_x, pts_z=None, pts_lo=None):
        for i, g in enumerate(ups):
            sd = 1 if i == 0 else -1
            a.rot(g, lambda t, sd=sd: (f(*pts_x)(t), 0, sd * f(*(pts_z or [(0, 0), (L, 0)]))(t)))
        for g in r.lo: a.rot(g, lambda t: (f(*(pts_lo or [(0, 0), (L, 0)]))(t), 0, 0))
    def body(px, py=None, pz=None, rx=None):
        a.rot('body', lambda t: (f(*px)(t), 0, 0))
        a.pos('body', lambda t: (0, f(*(py or [(0, 0), (L, 0)]))(t) * sc, f(*(pz or [(0, 0), (L, 0)]))(t) * sc))
    def head(px): a.rot('head', lambda t: (f(*px)(t), 0, 0)); [a.rot(n, lambda t: (f(*px)(t) * 0.5, 0, 0)) for n in r.neck]
    def jawo(px): a.rot('jaw', lambda t: (f(*px)(t), 0, 0))
    if typ == 'SHIELD':
        body([(0, 0), (0.25, 12), (0.8, 12), (1, 0)], [(0, 0), (0.25, -6), (0.8, -6), (1, 0)])
        arms([(0, 0), (0.25, -80), (0.8, -80), (1, 0)], [(0, 0), (0.25, 20), (0.8, 20), (1, 0)], [(0, 0), (0.25, -60), (0.8, -60), (1, 0)])
        head([(0, 0), (0.25, 14), (0.8, 14), (1, 0)])
        tail_flare(a, r, [(0, 0), (0.25, 35), (0.8, 35), (1, 0)], [(0, 0), (0.3, 0), (1, 0)], spread=40 if r.fam != 'tent' else 30)
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * f((0, 0), (0.3, 20), (0.8, 20), (1, 0))(t), 0))
    elif typ == 'ZONE':
        body([(0, 0), (0.3, -14), (0.5, 14), (0.7, 6), (1, 0)], [(0, 0), (0.3, 4), (0.5, -5), (1, 0)], [(0, 0), (0.3, 2), (0.5, -4), (1, 0)])
        arms([(0, 0), (0.3, -120 if r.fam in ('biped', 'tent', 'slug') else -70), (0.5, 20), (0.8, 10), (1, 0)], [(0, 0), (0.3, 8), (0.5, 0), (1, 0)], [(0, 0), (0.3, -20), (0.5, 0), (1, 0)])
        head([(0, 0), (0.3, -12), (0.5, 20), (1, 0)])
        for g in r.hind: a.rot(g, lambda t: (f((0, 0), (0.5, -14), (1, 0))(t), 0, 0))
        tail_flare(a, r, [(0, 0), (0.3, 24), (0.5, 40), (1, 0)], [(0, 0), (0.5, 0), (1, 0)], spread=10)
        if r.fam == 'insect':
            for g in r.abd: a.rot(g, lambda t: (f((0, 0), (0.3, -10), (0.5, 36), (1, 0))(t), 0, 0))
    elif typ == 'CONE':
        body([(0, 0), (0.3, -14), (0.55, -6), (1, 0)])
        head([(0, 0), (0.3, -26), (0.55, -10), (0.9, 6), (1, 0)])
        jawo([(0, 0), (0.3, 40), (0.85, 40), (1, 0)])
        arms([(0, 0), (0.3, -24), (0.6, -10), (1, 0)], [(0, 0), (0.3, 16), (0.6, 12), (1, 0)])
        tail_flare(a, r, [(0, 0), (0.3, 24), (0.7, 30), (1, 0)], [(0, 0), (1, 0)], spread=12)
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * 20, sd * 24 * math.sin(t * 20)))
    elif typ == 'DASH':
        body([(0, 0), (0.25, -6), (0.4, 20), (0.8, 16), (1, 0)], [(0, 0), (0.25, -7), (0.45, 2), (1, 0)], [(0, 0), (0.25, 5), (0.45, -10), (0.8, -4), (1, 0)])
        arms([(0, 0), (0.25, 30), (0.4, -60), (0.8, -40), (1, 0)], None, [(0, 0), (0.4, -30), (1, 0)])
        for g in r.hind: a.rot(g, lambda t: (f((0, 0), (0.25, -30), (0.4, 40), (0.8, 30), (1, 0))(t), 0, 0))
        head([(0, 0), (0.25, -8), (0.4, 14), (0.8, 10), (1, 0)])
        tail_flare(a, r, [(0, 0), (0.25, 30), (0.4, -8), (0.8, -4), (1, 0)], [(0, 0), (1, 0)], spread=-4)
        for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (0.4, 8), (1, 0))(t), 0, 0))
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * 28, sd * 16 * math.sin(t * 24)))
    elif typ == 'PROJ':
        body([(0, 0), (0.35, -18), (0.55, 12), (1, 0)], None, [(0, 0), (0.35, 3), (0.55, -5), (1, 0)])
        arms([(0, 0), (0.35, 40), (0.55, -70), (0.85, -50), (1, 0)], [(0, 0), (0.35, 8), (0.55, 0), (1, 0)], [(0, 0), (0.55, -30), (1, 0)])
        head([(0, 0), (0.35, -26), (0.55, 18), (0.85, 8), (1, 0)])
        jawo([(0, 0), (0.35, 44), (0.8, 30), (1, 0)])
        tail_flare(a, r, [(0, 0), (0.35, 22), (0.55, 34), (1, 0)], [(0, 0), (1, 0)], spread=8)
        if r.fam == 'slug':
            for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (0.35, -16), (0.55, 8), (1, 0))(t), 0, 0))
    elif typ == 'AOE':
        body([(0, 0), (0.3, -16), (0.55, 18), (1, 0)], [(0, 0), (0.3, 6), (0.55, -6), (1, 0)])
        arms([(0, 0), (0.3, -100 if r.fam != 'quad' else -80), (0.55, 30), (0.85, 12), (1, 0)], [(0, 0), (0.3, 40), (0.55, 8), (1, 0)], [(0, 0), (0.3, -24), (1, 0)])
        for g in r.hind: a.rot(g, lambda t: (f((0, 0), (0.3, 10), (0.55, -14), (1, 0))(t), 0, 0))
        head([(0, 0), (0.3, -28), (0.55, 20), (1, 0)])
        jawo([(0, 0), (0.3, 38), (0.7, 38), (1, 0)])
        tail_flare(a, r, [(0, 0), (0.3, 40), (0.55, 20), (1, 0)], [(0, 0), (1, 0)], spread=34)
        if r.fam == 'tent':
            for k, seg in enumerate(r.ch):
                for j, g in enumerate(seg): a.rot(g, lambda t, k=k, j=j: (0, 0, f((0, 0), (0.3, 30 + 6 * j), (0.55, 10), (1, 0))(t)))
    elif typ == 'BARRAGE':
        body([(0, 0), (0.2, -8), (1, -6), (1.0, 0)])
        head([(0, 0), (1, 0)])
        a.rot('head', lambda t: (math.sin(t * 36) * 10 - 8, 0, 0))
        jawo([(0, 0), (0.15, 30), (0.85, 30), (1, 0)])
        for i, g in enumerate(ups):
            a.rot(g, lambda t, i=i: (-40 + 24 * math.sin(t * 30 + i * math.pi), 0, 6))
        tail_flare(a, r, [(0, 0), (0.2, 28), (0.85, 28), (1, 0)], [(0, 0), (1, 0)], spread=16)
        for k, seg in enumerate(r.ch):
            for j, g in enumerate(seg): a.rot(g, lambda t, k=k, j=j: (0, 16 * math.sin(t * 26 + k * 1.3 - j * 0.6) * (0.2 < t < 0.9), 0))
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1; a.rot(g, lambda t, sd=sd: (0, sd * 14, sd * 32 * math.sin(t * 30)))
        for i, g in enumerate(r.segs): a.rot(g, lambda t, i=i: (f((0, 0), (0.2, -10), (0.85, -10), (1, 0))(t), 0, 0))
    elif typ == 'FLIGHT':
        body([(0, 0), (0.25, 14), (0.45, -16), (1, -8)], [(0, 0), (0.25, -6), (0.45, 10), (1, 8)])
        for g in r.wings:
            sd = 1 if g.endswith('l') else -1
            a.rot(g, lambda t, sd=sd, g=g: (0, sd * 8, sd * (f((0, 0), (0.25, -30), (1, 0))(t) + 62 * math.sin(t * 24) * (t > 0.25))))
        head([(0, 0), (0.25, 12), (0.45, -14), (1, 0)])
        for g in r.abd: a.rot(g, lambda t: (f((0, 0), (0.25, -10), (0.45, 22), (1, 18))(t), 0, 0))
        for i in range(3):
            for sd, n in ((1, 'l'), (-1, 'r')): a.rot(f'leg{i}_{n}', lambda t, sd=sd: (0, 0, sd * f((0, 0), (0.45, -26), (1, -12))(t)))
        tail_flare(a, r, [(0, 0), (0.3, -6), (0.5, 22), (1, 12)], [(0, 0), (1, 0)], spread=4)
    a.done()

def build_all(m):
    r = Rig(m)
    idle(m, r); walk(m, r); walk(m, r, True); attack(m, r); hurt(m, r); death(m, r); dama(m, r)
    for i, typ in enumerate(SKILLS[m.id]): skill(m, r, i, typ)
    weak(m, r)
    if m.id == 'chomei': fly(m, r)
