#!/usr/bin/env python3
"""Build output/Resala-Charity-UML.pptx from diagrams/out/*.png.

Design system (restrained, print-first):
  paper #FBFAF7   ink #1E2A32   teal #174E5B   gold #B08D57   grey #6E6A5E
  titles Georgia · body/labels Arial · dark slides for title & section dividers
Images are fitted preserving aspect ratio, centered in the content area.
"""
from pathlib import Path
from PIL import Image
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn


def strip_style(shape):
    el = shape._element
    style = el.find(qn('p:style'))
    if style is not None:
        el.remove(style)

ROOT      = Path(__file__).resolve().parent.parent
OUT       = ROOT / 'diagrams' / 'out'
DEST      = ROOT / 'output'
DEST.mkdir(exist_ok=True)

PAPER     = RGBColor(0xFB, 0xFA, 0xF7)
PANEL     = RGBColor(0xF5, 0xF4, 0xF0)
INK       = RGBColor(0x1E, 0x2A, 0x32)
DARK      = RGBColor(0x18, 0x22, 0x29)
TEAL      = RGBColor(0x17, 0x4E, 0x5B)
GOLD      = RGBColor(0xB0, 0x8D, 0x57)
GREY      = RGBColor(0x6E, 0x6A, 0x5E)
LINE      = RGBColor(0xE3, 0xE0, 0xD5)
DARK_PAPER= RGBColor(0xF5, 0xF4, 0xEF)
DARK_MUTE = RGBColor(0xC9, 0xC5, 0xBA)

SW, SH    = 13.333, 7.5
MARGIN    = 0.55
IMG_TOP   = 1.42
IMG_BOT   = 6.78
CAPTION_Y = 6.83
FOOT_RULE = 7.08
FOOT_Y    = 7.13

prs = Presentation()
prs.slide_width, prs.slide_height = Inches(SW), Inches(SH)
BLANK = prs.slide_layouts[6]
page = 0


def new_slide(bg=PAPER):
    global page
    page += 1
    s = prs.slides.add_slide(BLANK)
    r = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    r.fill.solid(); r.fill.fore_color.rgb = bg; r.line.fill.background()
    r.shadow.inherit = False
    strip_style(r)
    return s, page


def box(s, x, y, w, h):
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    return tb, tf


def para(tf, text, *, size=12, bold=False, color=INK, font='Arial', first=False,
         align=PP_ALIGN.LEFT, space_after=2, italic=False):
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.alignment = align
    p.space_after = Pt(space_after)
    r = p.add_run(); r.text = text
    f = r.font
    f.size, f.bold, f.italic = Pt(size), bold, italic
    f.name, f.color.rgb = font, color
    return p


def rect(s, x, y, w, h, fill):
    r = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(w), Inches(h))
    r.fill.solid(); r.fill.fore_color.rgb = fill; r.line.fill.background()
    r.shadow.inherit = False
    strip_style(r)
    return r


def header(s, kicker, title):
    _, tf = box(s, MARGIN, 0.30, SW - 2 * MARGIN, 0.22)
    para(tf, kicker.upper(), size=10.5, bold=True, color=TEAL, font='Arial', first=True, space_after=0)
    _, tf = box(s, MARGIN, 0.52, SW - 2 * MARGIN, 0.62)
    para(tf, title, size=24.5, bold=True, color=INK, font='Georgia', first=True, space_after=0)
    rect(s, MARGIN, 1.24, 1.5, 0.032, GOLD)


def footer(s, page_no, left='RESALA CHARITY — UML CLASS DIAGRAMS & DESIGN PATTERNS'):
    rect(s, MARGIN, FOOT_RULE, SW - 2 * MARGIN, 0.010, LINE)
    _, tf = box(s, MARGIN, FOOT_Y, 9.5, 0.2)
    para(tf, left, size=7.5, color=GREY, font='Arial', first=True, space_after=0)
    _, tf = box(s, SW - MARGIN - 1.2, FOOT_Y, 1.2, 0.2)
    para(tf, f'{page_no:02d}', size=9, bold=True, color=INK, font='Georgia',
         first=True, align=PP_ALIGN.RIGHT, space_after=0)


def diagram_slide(kicker, title, image, caption, dark_caption=False):
    img = OUT / image
    s, n = new_slide(PAPER)
    header(s, kicker, title)
    with Image.open(img) as im:
        w_px, h_px = im.size
    area_w, area_h = SW - 2 * MARGIN, IMG_BOT - IMG_TOP
    scale = min(area_w / (w_px / 96), area_h / (h_px / 96))
    w_in, h_in = (w_px / 96) * scale, (h_px / 96) * scale
    s.shapes.add_picture(str(img), Inches((SW - w_in) / 2), Inches(IMG_TOP + (area_h - h_in) / 2),
                         Inches(w_in), Inches(h_in))
    if caption:
        _, tf = box(s, MARGIN, CAPTION_Y, SW - 2 * MARGIN, 0.2)
        para(tf, caption, size=8.5, italic=True, color=GREY, font='Georgia',
             first=True, align=PP_ALIGN.CENTER, space_after=0)
    footer(s, n)
    return s


def dark_slide():
    return new_slide(DARK)


def dark_footer(s, page_no, left='RESALA CHARITY'):
    _, tf = box(s, MARGIN, 6.95, 8.0, 0.2)
    para(tf, left, size=8, bold=True, color=GOLD, font='Arial', first=True, space_after=0)
    _, tf = box(s, SW - MARGIN - 1.2, 6.93, 1.2, 0.22)
    para(tf, f'{page_no:02d}', size=9, bold=True, color=DARK_PAPER, font='Georgia',
         first=True, align=PP_ALIGN.RIGHT, space_after=0)


# ── 1 · title ────────────────────────────────────────────────────────────────
s, n = dark_slide()
_, tf = box(s, MARGIN, 0.42, 6, 0.25)
para(tf, 'A R C H I T E C T U R E   R E V I E W', size=11, bold=True, color=GOLD, font='Arial',
     first=True, space_after=0)
_, tf = box(s, MARGIN, 1.35, 11.5, 2.6)
para(tf, 'Resala Charity', size=58, bold=True, color=DARK_PAPER, font='Georgia', first=True, space_after=4)
para(tf, 'UML class diagrams & design patterns', size=27, color=DARK_MUTE, font='Georgia', space_after=0)
rect(s, MARGIN, 4.15, 2.1, 0.04, GOLD)
_, tf = box(s, MARGIN, 4.45, 11.0, 1.2)
para(tf, 'Architecture of the current codebase, module by module — every diagram generated from source.',
     size=13, color=DARK_MUTE, font='Georgia', italic=True, first=True, space_after=0)
_, tf = box(s, MARGIN, 6.35, 8.0, 0.5)
para(tf, 'SOURCE OF TRUTH — src/main/java · 132 files', size=8.5, bold=True, color=GOLD, font='Arial',
     first=True, space_after=2)
para(tf, 'Compiled UML rendered with d2 · November 2026', size=8.5, color=DARK_MUTE, font='Arial', space_after=0)
dark_footer(s, n)

# ── 2 · how to read ─────────────────────────────────────────────────────────
s, n = new_slide(PAPER)
header(s, 'How to read these diagrams', 'Notation & legend')
_, tf = box(s, MARGIN, 1.55, 6.1, 4.9)
para(tf, 'Edges', size=13, bold=True, color=TEAL, font='Arial', first=True, space_after=6)
edges = [
    ('────────▷', 'extends / inherits (solid line, hollow triangle)'),
    ('- - - - ▷', 'implements (dashed line, hollow triangle)'),
    ('───────◇', 'wraps / composes (decorator, aggregate)'),
    ('────────→', 'uses / association'),
]
for glyph, desc in edges:
    para(tf, f'{glyph}', size=13, color=INK, font='Menlo', first=False, space_after=0)
    para(tf, desc, size=10, color=GREY, font='Arial', space_after=5)
para(tf, 'Member markers', size=13, bold=True, color=TEAL, font='Arial', space_after=6)
para(tf, '+  public      –  private      #  protected', size=11, color=INK, font='Menlo', space_after=5)
for marker in ['«abstract»  abstract class', '«interface»  interface', '«record»  immutable value carrier',
               '«enum»  enumerated type', '«singleton»  one instance per JVM']:
    para(tf, marker, size=10, color=INK, font='Menlo', space_after=3)
_, tf = box(s, 7.35, 1.55, 5.4, 4.9)
para(tf, 'Containers', size=13, bold=True, color=TEAL, font='Arial', first=True, space_after=6)
for line in ['Package = labelled container (e.g. com.resala.donation)',
             'Dashed teal border = «pattern» boundary — participants of one design pattern',
             'Navy header, white text = class; white header rows below list members then methods',
             'Each count: “Module k/9” and “Pattern k/13” on its own slide']:
    para(tf, '·  ' + line, size=11, color=INK, font='Georgia', space_after=7)
para(tf, 'A note on fidelity', size=13, bold=True, color=TEAL, font='Arial', space_after=6)
para(tf, 'Member names and types are transcribed from the source. Where the code ships dead weight — e.g. the unused com.resala.volunteer.Volunteer duplicate of model.Volunteer — it is drawn but marked «unused», not hidden.',
     size=10.5, color=GREY, font='Georgia', space_after=0)
footer(s, n)

# ── 3 · system at a glance ──────────────────────────────────────────────────
diagram_slide('System at a glance', 'The layers, one picture', '00-overview.png',
              'Presentation → controllers → services → domain patterns → persistence → SQLite')

# ── 4 · design patterns at a glance ─────────────────────────────────────────
s, n = new_slide(PAPER)
header(s, 'Design patterns at a glance', 'All thirteen, as they appear in code')
rows = [
    ('Singleton',            'core',         'ConfigManager · DatabaseManager · AuditLogManager'),
    ('Factory Method',       'donation',     'DonationFactory → Online · Check · InKind'),
    ('Abstract Factory',     'view',         'UiFactory → ConsoleUiFactory · JavaFxUiFactory'),
    ('Strategy — 3 families','payment · notification · beneficiary',
                                                     'PaymentMethod · NotificationStrategy · DistributionPolicy'),
    ('Observer — 2 subjects','notification', 'DonationSubject · EventSubject · FeedObserver (dual)'),
    ('Decorator',            'donation',     'Receipt ← Tax / ThankYou decorators'),
    ('Adapter',              'payment',      'FakePaymentGatewayAdapter → FakePaymentProvider'),
    ('Command',              'payment',      'PaymentCommand → PaymentService → gateway'),
    ('State',                'payment',      'PaymentStatus (PENDING → PAID / FAILED)'),
    ('Repository',           'persistence',  '8 contracts + JdbcRepository base · Sqlite* impls'),
    ('Unit of Work',         'core · payment','TransactionManager.inTransaction(…)'),
    ('Static factory',       'payment',      'Payment.createPending / restore · PaymentMethods.fromCode'),
    ('Value objects',        'everywhere',   'Records: DonationRequest, PaymentRequest, GatewayResult…'),
]
col_w = [2.9, 2.6, 6.6]
x0 = MARGIN
y, row_h = 1.42, 0.335
table_rows = [('Pattern', 'Where', 'Key types')] + rows
for i, (a, b, c) in enumerate(table_rows):
    is_head = i == 0
    fill = INK if is_head else (PANEL if i % 2 == 0 else PAPER)
    rect(s, x0, y, sum(col_w), row_h, fill)
    tc = DARK_PAPER if is_head else INK
    pad = 0.09
    _, tf = box(s, x0 + pad, y + 0.015, col_w[0] - 2 * pad, row_h - 0.03)
    para(tf, a, size=10, bold=True, color=tc, font='Arial', first=True, space_after=0)
    _, tf = box(s, x0 + col_w[0] + pad, y + 0.015, col_w[1] - 2 * pad, row_h - 0.03)
    para(tf, b, size=9, color=(GOLD if is_head else GREY), font='Arial', first=True, space_after=0)
    _, tf = box(s, x0 + col_w[0] + col_w[1] + pad, y + 0.015, col_w[2] - 2 * pad, row_h - 0.03)
    para(tf, c, size=9, color=(DARK_PAPER if is_head else INK), font=('Menlo' if not is_head else 'Arial'),
         first=True, space_after=0)
    y += row_h
footer(s, n)
print('front matter built, slides:', n)

# ── 5–13 · modules ──────────────────────────────────────────────────────────
module_slides = [
    ('Module 1 / 9 — Model', 'The domain vocabulary', '10-model.png',
     'com.resala.model — entities, enums, lookup tables · 12 classes'),
    ('Module 2 / 9 — Core & persistence', 'One SQLite file, one connection', '20-core-persistence.png',
     'com.resala.core · persistence.sqlite — Singletons · JdbcRepository · contracts'),
    ('Module 3 / 9 — Donation', 'Factory, decorator receipts, observer', '30-donation.png',
     'com.resala.donation — Factory Method · Decorator · DonationSubject'),
    ('Module 4 / 9 — Payment', 'Strategy, adapter, idempotent retries', '40-payment.png',
     'com.resala.payment — Strategy · Adapter · Command · State (enum) · static factory'),
    ('Module 5 / 9 — Event', 'Lifecycle & attendee notifications', '50-event.png',
     'com.resala.event — DRAFT → OPEN → CLOSED · EventSubject'),
    ('Module 6 / 9 — Volunteer', 'Assignments, skills, certificates', '60-volunteer.png',
     'com.resala.volunteer — VolunteerService · CertificateGenerator (20 h threshold)'),
    ('Module 7 / 9 — Beneficiary', 'Swappable distribution policies', '70-beneficiary.png',
     'com.resala.beneficiary — Strategy: ByNeed · Equal · FIFO'),
    ('Module 8 / 9 — Notification', 'Two subjects, three channels', '80-notification.png',
     'com.resala.notification — Observer · Strategy · dual FeedObserver'),
    ('Module 9 / 9 — Presentation', 'JavaFX shell & console UI', '90-presentation.png',
     'com.resala.controller · com.resala.gui · com.resala.view — controllers · AppShell · views'),
]
for kicker, title, img, caption in module_slides:
    diagram_slide(kicker, title, img, caption)
print('module slides built, slides:', n)

# ── 14 · patterns divider ────────────────────────────────────────────────────
s, n = dark_slide()
_, tf = box(s, MARGIN, 0.42, 6, 0.25)
para(tf, 'S E C T I O N   2', size=11, bold=True, color=GOLD, font='Arial', first=True, space_after=0)
_, tf = box(s, MARGIN, 1.30, 12, 1.4)
para(tf, 'Design patterns,', size=52, bold=True, color=DARK_PAPER, font='Georgia', first=True, space_after=0)
para(tf, 'as the code ships them', size=52, bold=True, color=DARK_MUTE, font='Georgia', space_after=0)
rect(s, MARGIN, 3.55, 2.1, 0.04, GOLD)
_, tf = box(s, MARGIN, 3.85, 12, 2.2)
para(tf, 'Thirteen diagrams. Every pattern found in the source, each inside a dashed «pattern» boundary.',
     size=13, color=DARK_MUTE, font='Georgia', italic=True, first=True, space_after=14)
para(tf, 'SINGLETON ×3   ·   FACTORY METHOD   ·   ABSTRACT FACTORY   ·   STRATEGY ×3', size=10.5, bold=True,
     color=DARK_PAPER, font='Arial', space_after=5)
para(tf, 'OBSERVER ×2   ·   DECORATOR   ·   ADAPTER   ·   COMMAND   ·   STATE   ·   REPOSITORY   ·   UNIT OF WORK',
     size=10.5, bold=True, color=DARK_PAPER, font='Arial', space_after=0)
dark_footer(s, n, 'DESIGN PATTERNS')

# ── 15–27 · patterns ─────────────────────────────────────────────────────────
pattern_slides = [
    ('Pattern 1 / 13 — Singleton', 'One instance per JVM', 'patterns/p01-singleton.png',
     'Where — com.resala.core.ConfigManager · DatabaseManager · AuditLogManager'),
    ('Pattern 2 / 13 — Factory Method', 'DonationFactory decides the product', 'patterns/p02-factory-method.png',
     'Where — com.resala.donation.DonationFactory (Online · Check · InKind)'),
    ('Pattern 3 / 13 — Abstract Factory', 'A whole family of views, one factory', 'patterns/p03-abstract-factory.png',
     'Where — com.resala.view.UiFactory · ConsoleUiFactory · JavaFxUiFactory'),
    ('Pattern 4 / 13 — Strategy / Notification', 'Channels swappable at runtime', 'patterns/p04-strategy-notification.png',
     'Where — com.resala.notification.Notifier · Email / Sms / SocialStrategy'),
    ('Pattern 5 / 13 — Strategy / Payment', 'Cash, card, wallet — same contract', 'patterns/p05-strategy-payment.png',
     'Where — com.resala.payment.PaymentMethod · Cash / Card / WalletPaymentMethod · PaymentMethods'),
    ('Pattern 6 / 13 — Strategy / Distribution', 'Three allocation policies, one call', 'patterns/p06-strategy-distribution.png',
     'Where — com.resala.beneficiary.policy.ByNeed · Equal · FifoDistributionPolicy'),
    ('Pattern 7 / 13 — Observer', 'Subjects notify; observers react', 'patterns/p07-observer.png',
     'Where — com.resala.notification (DonationSubject · EventSubject · FeedObserver) · services implement subjects'),
    ('Pattern 8 / 13 — Decorator', 'Receipts composed at runtime', 'patterns/p08-decorator.png',
     'Where — com.resala.donation.Receipt · BasicReceipt · Tax / ThankYouReceiptDecorator'),
    ('Pattern 9 / 13 — Adapter', 'Provider’s API, our port', 'patterns/p09-adapter.png',
     'Where — com.resala.payment.FakePaymentGatewayAdapter → FakePaymentProvider'),
    ('Pattern 10 / 13 — Command', 'The charge, carried as a value', 'patterns/p10-command.png',
     'Where — com.resala.payment.PaymentCommand → gateway.charge(...) · idempotency key = retry safety'),
    ('Pattern 11 / 13 — State', 'Transitions guarded by the enum', 'patterns/p11-state.png',
     'Where — com.resala.payment.PaymentStatus (PENDING → PAID / FAILED · PAY-006 on illegal moves)'),
    ('Pattern 12 / 13 — Repository', 'Contracts in, SQL out of sight', 'patterns/p12-repository.png',
     'Where — com.resala.{donation,payment,event,volunteer,beneficiary}.*Repository · JdbcRepository'),
    ('Pattern 13 / 13 — Unit of Work', 'Settle atomically or not at all', 'patterns/p13-unit-of-work.png',
     'Where — com.resala.core.TransactionManager · PaymentService · DonationPaymentSettlement.settle()'),
]
for kicker, title, img, caption in pattern_slides:
    diagram_slide(kicker, title, img, caption)
print('pattern slides built, slides:', n)

# ── 28 · atlas ──────────────────────────────────────────────────────────────
s, n = new_slide(PAPER)
header(s, 'The whole map', 'Every class, one canvas')
img = OUT / '99-full.png'
with Image.open(img) as im:
    w_px, h_px = im.size
area_w, area_h = 8.6, IMG_BOT - IMG_TOP
scale = min(area_w / (w_px / 96), area_h / (h_px / 96))
w_in, h_in = (w_px / 96) * scale, (h_px / 96) * scale
s.shapes.add_picture(str(img), Inches(MARGIN + 0.15), Inches(IMG_TOP + (area_h - h_in) / 2),
                     Inches(w_in), Inches(h_in))
_, tf = box(s, 9.6, 1.6, 3.1, 4.8)
para(tf, 'How to read this', size=12, bold=True, color=TEAL, font='Arial', first=True, space_after=7)
for line in [
    'All 130+ classes, grouped by package.',
    'Same notation as the module pages.',
    'Text is unreadable at this size by design — this is the poster, not the reading copy.',
    'The slides 5–27 are the reading copies.',
    'Print the companion PDF page as a wall chart — or zoom the vector SVG.',
]:
    para(tf, '·  ' + line, size=11, color=INK, font='Georgia', space_after=8)
footer(s, n)

# ── 29 · closing ─────────────────────────────────────────────────────────────
s, n = dark_slide()
_, tf = box(s, MARGIN, 0.42, 8, 0.25)
para(tf, 'D E L I V E R A B L E S', size=11, bold=True, color=GOLD, font='Arial', first=True, space_after=0)
_, tf = box(s, MARGIN, 1.15, 12, 1.2)
para(tf, 'Four files, one truth', size=40, bold=True, color=DARK_PAPER, font='Georgia', first=True, space_after=0)
rect(s, MARGIN, 2.45, 2.1, 0.04, GOLD)
deliverables = [
    ('output/Resala-Charity-UML-Full.svg', 'Complete class map — vector, infinite zoom, selectable text'),
    ('output/Resala-Charity-UML.pptx', 'This deck — every module and pattern on its own slide'),
    ('output/Resala-Charity-UML.pdf', 'Same deck, print-ready (converted from the .pptx)'),
    ('output/Resala-Charity-UML-Full-poster.pdf', 'The all-system map as a single printable wall-chart page'),
]
y = 2.72
for fname, desc in deliverables:
    _, tf = box(s, MARGIN, y, 11.6, 0.28)
    para(tf, fname, size=13, bold=True, color=DARK_PAPER, font='Menlo', first=True, space_after=2)
    _, tf = box(s, MARGIN, y + 0.26, 11.6, 0.25)
    para(tf, desc, size=10.5, color=DARK_MUTE, font='Georgia', first=True, space_after=0)
    y += 0.74
_, tf = box(s, MARGIN, 5.72, 12, 0.9)
para(tf, 'REGENERATE', size=9, bold=True, color=GOLD, font='Arial', first=True, space_after=4)
para(tf, './build-diagrams  →  SVG + PNG + PDF for every diagram (incl. 99-full, assembled from the modules)',
     size=10.5, color=DARK_PAPER, font='Menlo', space_after=3)
para(tf, 'python3 diagrams/assemble-full.py  →  rebuild only the all-system map from module sources',
     size=10.5, color=DARK_PAPER, font='Menlo', space_after=0)
dark_footer(s, n, 'RESALA CHARITY')

prs.save(DEST / 'Resala-Charity-UML.pptx')
print('saved', DEST / 'Resala-Charity-UML.pptx', '— slides:', n)
