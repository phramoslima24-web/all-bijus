import math
from lib import *

def X(s, x, w): return x if s > 0 else -x - w
SIDES = ((1, 'l'), (-1, 'r'))
def mat(base, alt=None, **kw):
    d = dict(base=base, noise=kw.pop('noise', 6))
    if alt: d['alt'] = alt
    d.update(kw); return d

def tail(m, parent, name, base, n, size, step, matn, grow=0, glow_from=None, glowc=None, flame=None, flame_mat=None):
    p = parent
    for k in range(n):
        w = size[0] + grow * k; h = size[1] + grow * k; d = size[2]
        x = base[0] + step[0] * k - grow * k / 2; y = base[1] + step[1] * k - grow * k / 2; z = base[2] + step[2] * k
        pv = (x + w / 2 - step[0] / 2, y + h / 2 - step[1] / 2, z + d / 2 - step[2] / 2)
        gn = f'{name}_{k}'; m.g(gn, pv, p)
        gl = (glowc, 'all') if glow_from is not None and k >= glow_from else None
        m.c(gn, gn + '_c', (x, y, z), (w, h, d), matn, glow=gl)
        if flame and k in flame:
            m.c(gn, gn + '_fire', (x - 2, y - 2, z - 1), (w + 4, h + 4, d + 3), flame_mat, glow=(glowc, 'all'))
        p = gn

def shukaku():
    M = dict(sand=mat((214, 176, 104), (186, 148, 80), pat='sand', noise=8), belly=mat((238, 214, 162), (214, 190, 140), pat='stripe_h', p=3),
             dark=mat((66, 46, 34), noise=4), band=mat((214, 176, 104), (74, 52, 40), pat='stripe_v', p=3, noise=7), tuft=mat((120, 84, 52), (80, 56, 36), pat='stripe_v', p=2))
    m = Model('shukaku', M)
    m.g('body', (0, 30, 0))
    m.c('body', 'belly', (-24, 16, -16), (48, 30, 34), 'belly'); m.c('body', 'chest', (-27, 44, -15), (54, 20, 30), 'sand'); m.c('body', 'hump', (-20, 38, 14), (40, 24, 10), 'sand')
    m.g('head', (0, 62, -6), 'body')
    m.c('head', 'skull', (-19, 62, -26), (38, 24, 32), 'sand', detail=('ring', (255, 205, 40)))
    m.c('head', 'muzzle', (-10, 60, -36), (20, 12, 10), 'belly'); m.c('head', 'nose', (-3, 69, -38), (6, 4, 3), 'dark'); m.c('head', 'tuft', (-4, 86, -14), (8, 8, 8), 'tuft')
    for s, n in SIDES: m.c('head', f'ear_{n}', (X(s, 10, 9), 84, -12), (9, 9, 6), 'sand')
    m.g('jaw', (0, 62, -10), 'head'); m.c('jaw', 'jawc', (-10, 57, -34), (20, 5, 26), 'belly'); m.c('jaw', 'fang', (-8, 62, -33), (3, 3, 3), 'belly'); m.c('jaw', 'fang2', (5, 62, -33), (3, 3, 3), 'belly')
    for s, n in SIDES:
        m.g(f'arm_{n}', (s * 30, 62, -2), 'body'); m.c(f'arm_{n}', f'upper_{n}', (X(s, 24, 14), 40, -10), (14, 24, 16), 'sand')
        m.g(f'forearm_{n}', (s * 31, 42, -2), f'arm_{n}'); m.c(f'forearm_{n}', f'fore_{n}', (X(s, 24, 14), 20, -10), (14, 22, 16), 'sand')
        m.g(f'hand_{n}', (s * 31, 22, -2), f'forearm_{n}'); m.c(f'hand_{n}', f'handc_{n}', (X(s, 23, 16), 10, -14), (16, 12, 22), 'sand')
        for i in range(3): m.c(f'hand_{n}', f'claw_{n}{i}', (X(s, 24 + i * 6, 3), 8, -20), (3, 3, 6), 'dark')
        m.g(f'leg_{n}', (s * 14, 20, 0), 'body'); m.c(f'leg_{n}', f'thigh_{n}', (X(s, 4, 22), 4, -12), (22, 16, 24), 'sand')
        m.g(f'foot_{n}', (s * 15, 6, -4), f'leg_{n}'); m.c(f'foot_{n}', f'footc_{n}', (X(s, 3, 24), 0, -22), (24, 8, 30), 'sand')
        for i in range(3): m.c(f'foot_{n}', f'toe_{n}{i}', (X(s, 4 + i * 8, 5), 0, -27), (5, 5, 5), 'dark')
    tail(m, 'body', 'tail0', (-8, 28, 16), 4, (16, 16, 14), (0, 6, 13), 'band', grow=2)
    return m

def matatabi():
    M = dict(fur=mat((70, 122, 214), (32, 58, 150), pat='stripe_v', p=3, down=(224, 232, 250)), cream=mat((226, 234, 250), noise=5), dark=mat((24, 30, 80), noise=4),
             pink=mat((230, 120, 150)), fire=mat((120, 220, 255), noise=10), tailf=mat((60, 110, 205), (30, 50, 140), pat='stripe_h', p=3))
    m = Model('matatabi', M)
    m.g('body', (0, 30, 0))
    m.c('body', 'chest', (-9, 22, -24), (18, 19, 20), 'fur'); m.c('body', 'abdomen', (-8, 22, -6), (16, 16, 18), 'fur'); m.c('body', 'hips', (-9, 22, 10), (18, 18, 16), 'fur')
    m.c('body', 'spine', (-3, 38, -20), (6, 3, 40), 'dark')
    m.g('neck', (0, 36, -24), 'body'); m.c('neck', 'neckc', (-5, 32, -34), (10, 13, 12), 'fur'); m.c('neck', 'ruff', (-7, 30, -32), (14, 5, 8), 'cream')
    m.g('head', (0, 38, -34), 'neck')
    m.c('head', 'skull', (-7, 34, -46), (14, 12, 12), 'fur', detail=('ring', (130, 245, 255))); m.c('head', 'muzzle', (-4, 34, -52), (8, 6, 7), 'cream'); m.c('head', 'nose', (-1, 38, -53), (3, 2, 1), 'pink')
    for s, n in SIDES:
        m.g(f'ear_{n}', (s * 5, 45, -38), 'head'); m.c(f'ear_{n}', f'earc_{n}', (X(s, 2, 5), 46, -40), (5, 6, 3), 'fur'); m.c(f'ear_{n}', f'eart_{n}', (X(s, 3, 3), 52, -39), (3, 3, 2), 'dark')
    m.g('jaw', (0, 36, -42), 'head'); m.c('jaw', 'jawc', (-3, 33, -51), (6, 2, 9), 'cream'); m.c('jaw', 'fang', (-3, 35, -51), (1, 2, 1), 'cream')
    for s, n in SIDES:
        m.g(f'leg_f{n}', (s * 8, 26, -18), 'body'); m.c(f'leg_f{n}', f'fu_{n}', (X(s, 5, 7), 14, -21), (7, 14, 7), 'fur')
        m.g(f'shin_f{n}', (s * 8, 14, -18), f'leg_f{n}'); m.c(f'shin_f{n}', f'fl_{n}', (X(s, 5, 7), 3, -21), (7, 11, 7), 'fur')
        m.g(f'paw_f{n}', (s * 8, 3, -18), f'shin_f{n}'); m.c(f'paw_f{n}', f'fp_{n}', (X(s, 4, 9), 0, -27), (9, 3, 12), 'cream')
        m.g(f'leg_b{n}', (s * 9, 30, 14), 'body'); m.c(f'leg_b{n}', f'bu_{n}', (X(s, 4, 11), 14, 8), (11, 18, 13), 'fur')
        m.g(f'shin_b{n}', (s * 9, 14, 14), f'leg_b{n}'); m.c(f'shin_b{n}', f'bl_{n}', (X(s, 5, 8), 4, 10), (8, 10, 9), 'fur')
        m.g(f'paw_b{n}', (s * 9, 4, 14), f'shin_b{n}'); m.c(f'paw_b{n}', f'bp_{n}', (X(s, 4, 10), 0, 6), (10, 4, 14), 'cream')
    tail(m, 'body', 'tail0', (-9, 28, 24), 5, (5, 5, 7), (-3, 4, 7), 'tailf', glow_from=1, glowc=(120, 225, 255), flame=(1, 2, 3, 4), flame_mat='fire')
    tail(m, 'body', 'tail1', (4, 28, 24), 5, (5, 5, 7), (3, 4, 7), 'tailf', glow_from=1, glowc=(120, 225, 255), flame=(1, 2, 3, 4), flame_mat='fire')
    return m

def isobu():
    M = dict(shell=mat((58, 102, 112), (34, 64, 76), pat='hex', p=5), skin=mat((126, 174, 152), (98, 146, 128), pat='spots', dens=0.05, down=(190, 215, 190)),
             bone=mat((226, 214, 172), (190, 176, 130), pat='stripe_h', p=3), dark=mat((30, 52, 56), noise=4), plast=mat((210, 205, 160), (180, 170, 120), pat='hex', p=6))
    m = Model('isobu', M)
    m.g('body', (0, 16, 0))
    m.c('body', 'plastron', (-22, 8, -24), (44, 8, 50), 'plast'); m.c('body', 'shell_base', (-26, 14, -28), (52, 10, 58), 'shell')
    m.c('body', 'shell_mid', (-22, 24, -24), (44, 8, 50), 'shell'); m.c('body', 'shell_top', (-15, 32, -18), (30, 6, 38), 'shell')
    for x in (-12, 0, 12):
        for z in (-12, 3, 18): m.c('body', f'spike_{x}_{z}', (x - 3, 38, z - 3), (6, 10, 6), 'bone')
    for s in (1, -1):
        for z in (-16, 2, 20): m.c('body', f'side_{s}_{z}', (X(s, 26, 9), 16, z - 2), (9, 5, 5), 'bone')
    m.c('body', 'rear_spike', (-4, 20, 30), (8, 8, 6), 'bone')
    m.g('neck', (0, 18, -26), 'body'); m.c('neck', 'neckc', (-6, 12, -36), (12, 10, 12), 'skin')
    m.g('head', (0, 20, -36), 'neck')
    m.c('head', 'skull', (-9, 10, -52), (18, 16, 16), 'skin', detail=('eyes', (255, 140, 70))); m.c('head', 'beak', (-6, 10, -58), (12, 6, 6), 'skin')
    for i, x in enumerate((-5, -1, 3)): m.c('head', f'crest{i}', (x, 26, -46), (3, 5, 3), 'bone')
    m.g('jaw', (0, 12, -44), 'head'); m.c('jaw', 'jawc', (-6, 8, -57), (12, 3, 12), 'skin')
    for s, n in SIDES:
        for z, f in ((-18, 'f'), (20, 'b')):
            m.g(f'leg_{f}{n}', (s * 22, 16, z), 'body'); m.c(f'leg_{f}{n}', f'leg_{f}{n}_c', (X(s, 16, 12), 2, z - 7), (12, 14, 14), 'skin')
            m.g(f'paw_{f}{n}', (s * 22, 2, z), f'leg_{f}{n}'); m.c(f'paw_{f}{n}', f'paw_{f}{n}_c', (X(s, 15, 14), 0, z - 9), (14, 3, 18), 'skin')
    for k, (bx, dx) in enumerate(((-14, -4), (-4, 0), (6, 4))):
        tail(m, 'body', f'tail{k}', (bx, 14, 30), 3, (9, 9, 12), (dx, 2, 10), 'skin', grow=-1)
        m.c(f'tail{k}_2', f'tail{k}_tip', (bx + 2 + 2 * dx - 1, 18, 51), (4, 4, 5), 'bone')
    return m

def songoku():
    M = dict(fur=mat((172, 40, 30), (128, 26, 22), pat='spots', dens=0.06), mane=mat((236, 236, 240), (200, 200, 214), pat='spots', dens=0.06), skin=mat((64, 38, 38)),
             dark=mat((38, 20, 20), noise=4), lava=mat((255, 120, 30), noise=10))
    m = Model('songoku', M)
    m.g('body', (0, 30, 0))
    m.c('body', 'pelvis', (-14, 24, -10), (28, 12, 20), 'fur'); m.c('body', 'abdomen', (-17, 34, -11), (34, 14, 22), 'fur'); m.c('body', 'chest', (-26, 46, -14), (52, 24, 28), 'fur', glow=((255, 110, 30), 'cracks'))
    m.c('body', 'mane_back', (-22, 56, -2), (44, 18, 12), 'mane'); m.c('body', 'loin', (-16, 22, -9), (32, 5, 18), 'dark')
    m.g('head', (0, 70, -12), 'body')
    m.c('head', 'skull', (-12, 68, -24), (24, 18, 22), 'fur'); m.c('head', 'face', (-9, 68, -27), (18, 12, 3), 'skin', detail=('eyes', (255, 230, 80)))
    m.c('head', 'brow', (-13, 78, -27), (26, 5, 5), 'dark'); m.c('head', 'mane_h', (-16, 70, -2), (32, 18, 14), 'mane'); m.c('head', 'crest', (-4, 86, -14), (8, 6, 18), 'mane')
    for s, n in SIDES: m.c('head', f'ear_{n}', (X(s, 12, 3), 74, -8), (3, 6, 4), 'skin')
    m.g('jaw', (0, 70, -14), 'head'); m.c('jaw', 'jawc', (-10, 64, -26), (20, 6, 18), 'skin'); m.c('jaw', 'tusk_l', (-8, 70, -25), (2, 4, 2), 'mane'); m.c('jaw', 'tusk_r', (6, 70, -25), (2, 4, 2), 'mane')
    for s, n in SIDES:
        m.g(f'arm_{n}', (s * 31, 66, 0), 'body'); m.c(f'arm_{n}', f'upper_{n}', (X(s, 24, 16), 40, -8), (16, 28, 16), 'fur'); m.c(f'arm_{n}', f'shoulder_{n}', (X(s, 22, 20), 64, -10), (20, 10, 20), 'mane')
        m.g(f'forearm_{n}', (s * 34, 42, 0), f'arm_{n}'); m.c(f'forearm_{n}', f'fore_{n}', (X(s, 25, 18), 16, -9), (18, 26, 18), 'fur', glow=((255, 110, 30), 'cracks'))
        m.g(f'hand_{n}', (s * 34, 18, 0), f'forearm_{n}'); m.c(f'hand_{n}', f'fist_{n}', (X(s, 24, 20), 4, -11), (20, 14, 22), 'skin')
        m.g(f'leg_{n}', (s * 13, 30, 0), 'body'); m.c(f'leg_{n}', f'thigh_{n}', (X(s, 5, 16), 14, -9), (16, 18, 18), 'fur')
        m.g(f'shin_{n}', (s * 13, 14, 0), f'leg_{n}'); m.c(f'shin_{n}', f'shinc_{n}', (X(s, 6, 14), 4, -8), (14, 10, 16), 'fur')
        m.g(f'foot_{n}', (s * 13, 4, 0), f'shin_{n}'); m.c(f'foot_{n}', f'footc_{n}', (X(s, 4, 18), 0, -14), (18, 5, 24), 'skin')
    for k, (bx, dx) in enumerate(((-10, -5), (-3, -2), (3, 2), (10, 5))):
        tail(m, 'body', f'tail{k}', (bx - 3, 26, 10), 4, (6, 6, 10), (dx, 5, 9), 'fur', glow_from=2, glowc=(255, 120, 30))
    return m

def kokuo():
    M = dict(white=mat((240, 244, 252), (214, 222, 240), pat='spots', dens=0.03, noise=4), blue=mat((84, 120, 196), (56, 84, 150), pat='stripe_h', p=3), hoof=mat((60, 70, 110), noise=4),
             fin=mat((120, 158, 226), (84, 120, 196), pat='stripe_v', p=2), horn=mat((236, 232, 214), (200, 196, 176), pat='stripe_h', p=3))
    m = Model('kokuo', M)
    m.g('body', (0, 30, 0))
    m.c('body', 'barrel', (-13, 26, -20), (26, 24, 40), 'white'); m.c('body', 'chest', (-12, 28, -28), (24, 20, 10), 'white'); m.c('body', 'rump', (-12, 28, 18), (24, 20, 10), 'white')
    m.c('body', 'dorsal1', (-1, 50, -8), (3, 10, 12), 'fin'); m.c('body', 'dorsal2', (-1, 50, 6), (3, 7, 10), 'fin')
    m.g('neck', (0, 46, -26), 'body'); m.c('neck', 'neck_a', (-6, 44, -38), (12, 14, 14), 'white')
    for i in range(3): m.c('neck', f'mane{i}', (-1, 56 + i * 2, -32 - i * 3), (2, 8, 5), 'fin')
    m.g('neck2', (0, 56, -36), 'neck'); m.c('neck2', 'neck_b', (-6, 54, -44), (12, 12, 10), 'white')
    m.g('head', (0, 64, -40), 'neck2')
    m.c('head', 'cranium', (-7, 60, -52), (14, 14, 14), 'white', detail=('eyes', (255, 90, 80))); m.c('head', 'melon', (-6, 70, -58), (12, 6, 8), 'white'); m.c('head', 'rostrum', (-4, 60, -66), (8, 8, 14), 'blue')
    m.c('head', 'horn', (-1, 74, -48), (3, 10, 3), 'horn')
    m.g('jaw', (0, 60, -54), 'head'); m.c('jaw', 'jawc', (-3, 57, -64), (6, 3, 12), 'white')
    for s, n in SIDES:
        m.g(f'leg_f{n}', (s * 8, 30, -16), 'body'); m.c(f'leg_f{n}', f'fu_{n}', (X(s, 4, 8), 14, -20), (8, 16, 8), 'white')
        m.g(f'shin_f{n}', (s * 8, 14, -16), f'leg_f{n}'); m.c(f'shin_f{n}', f'fl_{n}', (X(s, 5, 6), 4, -19), (6, 10, 6), 'white'); m.c(f'shin_f{n}', f'ffin_{n}', (X(s, 11, 2), 6, -20), (2, 8, 8), 'fin')
        m.g(f'paw_f{n}', (s * 8, 4, -16), f'shin_f{n}'); m.c(f'paw_f{n}', f'fh_{n}', (X(s, 4, 8), 0, -20), (8, 4, 8), 'hoof')
        m.g(f'leg_b{n}', (s * 8, 30, 16), 'body'); m.c(f'leg_b{n}', f'bu_{n}', (X(s, 4, 9), 14, 10), (9, 16, 12), 'white')
        m.g(f'shin_b{n}', (s * 8, 14, 16), f'leg_b{n}'); m.c(f'shin_b{n}', f'bl_{n}', (X(s, 5, 6), 4, 13), (6, 10, 6), 'white'); m.c(f'shin_b{n}', f'bfin_{n}', (X(s, 11, 2), 6, 16), (2, 8, 8), 'fin')
        m.g(f'paw_b{n}', (s * 8, 4, 16), f'shin_b{n}'); m.c(f'paw_b{n}', f'bh_{n}', (X(s, 4, 8), 0, 12), (8, 4, 8), 'hoof')
    for k in range(5):
        tail(m, 'body', f'tail{k}', (-2.5 + (k - 2) * 3, 42, 26), 4, (5, 5, 10), ((k - 2) * 3.5, -4, 9), 'white' if k % 2 == 0 else 'blue')
        m.c(f'tail{k}_3', f'tail{k}_fin', (-6 + (k - 2) * 3 + (k - 2) * 10.5, 28, 56), (12, 2, 9), 'fin')
    return m

def saiken():
    M = dict(slug=mat((238, 240, 248), (150, 184, 232), pat='spots', dens=0.05, down=(206, 210, 230), noise=4), pink=mat((240, 150, 172)), eye=mat((255, 230, 120)), blue=mat((150, 184, 232), (110, 150, 214), pat='stripe_v', p=3),
             tailm=mat((238, 240, 248), (120, 160, 220), pat='stripe_h', p=3))
    m = Model('saiken', M)
    m.g('body', (0, 12, 0))
    zs = [-26, -10, 6, 22, 36, 48, 58]; hs = [24, 24, 22, 20, 16, 12]; ws = [28, 28, 26, 22, 16, 10]
    p = 'body'
    for i in range(6):
        gn = f'seg{i}'; m.g(gn, (0, 8, zs[i]), p)
        m.c(gn, f'seg{i}_c', (-ws[i] / 2, 0, zs[i]), (ws[i], hs[i], zs[i + 1] - zs[i]), 'slug')
        if i in (1, 2): m.c(gn, f'seg{i}_back', (-ws[i] / 2 + 3, hs[i], zs[i] + 2), (ws[i] - 6, 3, 12), 'blue')
        p = gn
    m.g('head', (0, 14, -26), 'seg0'); m.c('head', 'headc', (-12, 4, -40), (24, 20, 14), 'slug'); m.c('head', 'mouth', (-7, 4, -43), (14, 6, 3), 'pink'); m.c('head', 'brow', (-9, 20, -41), (18, 3, 3), 'blue')
    for s, n in SIDES:
        m.g(f'stalk_{n}', (s * 7, 24, -34), 'head'); m.c(f'stalk_{n}', f'st1_{n}', (X(s, 6, 3), 24, -35), (3, 8, 3), 'slug')
        m.g(f'stalk2_{n}', (s * 7, 32, -34), f'stalk_{n}'); m.c(f'stalk2_{n}', f'st2_{n}', (X(s, 6, 3), 32, -35), (3, 4, 3), 'slug'); m.c(f'stalk2_{n}', f'eye_{n}', (X(s, 5, 5), 35, -36), (5, 5, 5), 'eye', glow=((255, 220, 90), 'all'))
        m.g(f'arm_{n}', (s * 14, 16, -16), 'seg0'); m.c(f'arm_{n}', f'au_{n}', (X(s, 13, 5), 8, -19), (5, 10, 5), 'slug')
        m.g(f'hand_{n}', (s * 15, 8, -16), f'arm_{n}'); m.c(f'hand_{n}', f'ah_{n}', (X(s, 13, 6), 3, -25), (6, 5, 9), 'pink')
    k = 0
    for side in (-1, 1):
        for row in range(3):
            bx = 8 if side > 0 else -13
            tail(m, 'seg2', f'tail{k}', (bx, 22, 8 + 6 * row), 4, (5, 5, 8), (side * 2.5, 5, 6), 'tailm'); k += 1
    return m

def chomei():
    M = dict(shell=mat((78, 170, 120), (46, 120, 86), pat='stripe_v', p=4, down=(224, 226, 190)), dark=mat((28, 60, 50), noise=4), eye=mat((255, 150, 40)),
             elytra=mat((92, 186, 130), (50, 130, 90), pat='stripe_h', p=4), wing=mat((214, 240, 226), (90, 150, 120), pat='lattice', p=4, holes=True, noise=3),
             tailm=mat((78, 170, 120), (30, 80, 60), pat='stripe_h', p=3))
    m = Model('chomei', M)
    m.g('body', (0, 34, 0))
    m.c('body', 'thorax', (-14, 34, -10), (28, 22, 24), 'shell'); m.c('body', 'collar', (-12, 36, -16), (24, 18, 6), 'dark')
    m.g('abd0', (0, 42, 14), 'body'); m.c('abd0', 'a0', (-12, 30, 14), (24, 20, 16), 'shell')
    m.g('abd1', (0, 40, 28), 'abd0'); m.c('abd1', 'a1', (-10, 29, 28), (20, 16, 14), 'shell')
    m.g('abd2', (0, 38, 40), 'abd1'); m.c('abd2', 'a2', (-8, 28, 40), (16, 13, 12), 'shell'); m.c('abd2', 'sting', (-3, 28, 50), (6, 6, 10), 'dark')
    m.g('head', (0, 50, -14), 'body'); m.c('head', 'headc', (-10, 42, -30), (20, 16, 16), 'shell')
    for s, n in SIDES:
        m.c('head', f'eye_{n}', (X(s, 9, 7), 46, -28), (7, 11, 10), 'eye', glow=((255, 160, 40), 'all'))
        m.c('head', f'ant_{n}', (X(s, 3, 2), 58, -22), (2, 10, 2), 'dark')
        m.g(f'mand_{n}', (s * 4, 44, -30), 'head'); m.c(f'mand_{n}', f'mandc_{n}', (X(s, 2, 4), 43, -40), (4, 4, 12), 'dark')
    m.g('horn', (0, 56, -28), 'head'); m.c('horn', 'hornc', (-2, 54, -46), (4, 5, 20), 'dark'); m.c('horn', 'horntip', (-1, 56, -50), (2, 3, 4), 'dark')
    for i, z in enumerate((-6, 3, 12)):
        for s, n in SIDES:
            m.g(f'leg{i}_{n}', (s * 10, 36, z), 'body'); m.c(f'leg{i}_{n}', f'fem{i}_{n}', (X(s, 10, 14), 32, z - 2), (14, 4, 4), 'dark')
            m.g(f'shin{i}_{n}', (s * 24, 34, z), f'leg{i}_{n}'); m.c(f'shin{i}_{n}', f'tib{i}_{n}', (X(s, 22, 4), 8, z - 2), (4, 26, 4), 'dark')
    for s, n in SIDES:
        m.g(f'wing_f{n}', (s * 6, 58, -4), 'body'); m.c(f'wing_f{n}', f'elytra_{n}', (X(s, 6, 36), 58, -6), (36, 1, 26), 'elytra')
        m.g(f'wing_h{n}', (s * 5, 57, 6), 'body'); m.c(f'wing_h{n}', f'hind_{n}', (X(s, 5, 46), 57, 3), (46, 1, 18), 'wing')
    for k in range(7):
        tail(m, 'abd2', f'tail{k}', ((k - 3) * 3.5 - 2, 30, 48), 4, (4, 4, 9), ((k - 3) * 1.8, 1.5 - abs(k - 3) * 0.8, 9), 'tailm')
    return m

def gyuki():
    M = dict(fur=mat((152, 64, 46), (112, 40, 32), pat='spots', dens=0.05), cream=mat((232, 208, 170), noise=5), dark=mat((54, 30, 28), noise=4), horn=mat((236, 226, 200), (190, 176, 150), pat='stripe_h', p=3),
             tent=mat((204, 104, 76), (236, 196, 166), pat='dots', p=4, down=(236, 196, 166)), hump=mat((92, 40, 32), (70, 30, 26), pat='spots', dens=0.05))
    m = Model('gyuki', M)
    m.g('body', (0, 30, 0))
    m.c('body', 'torso', (-22, 38, -14), (44, 32, 28), 'fur'); m.c('body', 'hump', (-18, 66, -14), (36, 12, 22), 'hump'); m.c('body', 'pelvis', (-18, 24, -12), (36, 16, 24), 'dark'); m.c('body', 'belly', (-16, 40, -16), (32, 24, 3), 'cream')
    m.g('head', (0, 68, -14), 'body')
    m.c('head', 'skull', (-12, 68, -34), (24, 20, 22), 'fur', detail=('ring', (255, 70, 40))); m.c('head', 'muzzle', (-9, 68, -42), (18, 11, 8), 'cream'); m.c('head', 'nose', (-5, 70, -43), (10, 3, 2), 'dark')
    for s, n in SIDES:
        m.c('head', f'horna_{n}', (X(s, 12, 7), 80, -26), (7, 5, 6), 'horn'); m.c('head', f'hornb_{n}', (X(s, 17, 6), 84, -26), (6, 5, 6), 'horn'); m.c('head', f'hornc_{n}', (X(s, 21, 5), 88, -26), (5, 9, 6), 'horn')
        m.c('head', f'ear_{n}', (X(s, 12, 6), 78, -20), (6, 4, 4), 'fur')
    m.g('jaw', (0, 70, -26), 'head'); m.c('jaw', 'jawc', (-8, 64, -42), (16, 4, 18), 'cream')
    for s, n in SIDES:
        m.g(f'arm_{n}', (s * 28, 66, -4), 'body'); m.c(f'arm_{n}', f'upper_{n}', (X(s, 22, 16), 42, -12), (16, 26, 16), 'fur')
        m.g(f'forearm_{n}', (s * 30, 44, -4), f'arm_{n}'); m.c(f'forearm_{n}', f'fore_{n}', (X(s, 22, 16), 22, -12), (16, 22, 16), 'fur'); m.c(f'forearm_{n}', f'brace_{n}', (X(s, 21, 18), 28, -13), (18, 6, 18), 'dark')
        m.g(f'hand_{n}', (s * 30, 24, -4), f'forearm_{n}'); m.c(f'hand_{n}', f'fist_{n}', (X(s, 21, 18), 10, -14), (18, 14, 20), 'dark')
    for k in range(8):
        a = math.radians(22.5 + k * 45); cx, sz = math.cos(a), math.sin(a)
        tail(m, 'body', f'tent{k}', (cx * 11 - 4.5, 21, sz * 11 - 4.5), 5, (9, 9, 9), (cx * 8, -4.2, sz * 8), 'tent', grow=-1)
    return m

BUILD = dict(shukaku=shukaku, matatabi=matatabi, isobu=isobu, songoku=songoku, kokuo=kokuo, saiken=saiken, chomei=chomei, gyuki=gyuki)
