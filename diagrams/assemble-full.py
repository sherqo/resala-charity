#!/usr/bin/env python3
"""Assemble 99-full.d2 from the module diagrams in this directory.

The module files are the source of truth; 99-full.d2 is a generated artifact.
This script:
  * wraps com.resala.model classes in a `model:` container,
  * merges every module's `sqlite:` persistence container into one,
  * drops the per-module `external:` stub boxes and remaps their references
    to the real definitions (e.g. external.Donor -> model.Donor),
  * remaps cross-module references between modules the same way,
  * keeps the pattern «boundaries» the module files define.

Usage:  python3 diagrams/assemble-full.py [out.d2]   (default: diagrams/d2/99-full.d2)
"""
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE / 'd2'
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else SRC / '99-full.d2'

def read(f): return (SRC / f).read_text()

def strip_vars(s):
    s = re.sub(r'\A#[^\n]*\n', '', s)
    m = re.match(r'\s*vars:\s*\{.*?\n\}\s*\n', s, re.S)
    if m: s = s[m.end():]
    return s.strip() + '\n'

def extract_block(s, key):
    idx = s.find('\n' + key + ':')
    if idx == -1:
        if s.startswith(key + ':'): idx = 0
        else: raise ValueError('not found: ' + key)
    else: idx += 1
    j = s.index('{', idx)
    depth = 0; k = j
    while k < len(s):
        if s[k] == '{': depth += 1
        elif s[k] == '}':
            depth -= 1
            if depth == 0: break
        k += 1
    return (s[:idx] + s[k + 1:]).strip() + '\n', s[idx:k + 1]

def inner(block):
    j = block.index('{')
    return block[j + 1:block.rindex('}')].rstrip() + '\n'

REF_MAP = [
    ('external.PaymentRepository', 'payment.PaymentRepository'),
    ('external.PaymentSettlement', 'payment.PaymentSettlement'),
    ('external.Payment', 'payment.Payment'),
    ('external.DonationSubject', 'notification.observer.DonationSubject'),
    ('external.DonationObserver', 'notification.observer.DonationObserver'),
    ('external.DonationType', 'model.DonationType'),
    ('external.Donor', 'model.Donor'),
    ('external.EventSubject', 'notification.observer.EventSubject'),
    ('external.EventObserver', 'notification.observer.EventObserver'),
    ('external.EventRegistrationRepository', 'event.EventRegistrationRepository'),
    ('external.EventRegistration', 'event.EventRegistration'),
    ('external.EventRepository', 'event.EventRepository'),
    ('external.EventType', 'model.EventType'),
    ('external.Event', 'model.Event'),
    ('external.VolunteerEventAssignment', 'model.VolunteerEventAssignment'),
    ('external.ModelVolunteer', 'model.Volunteer'),
    ('external.Skill', 'model.Skill'),
    ('external.Beneficiary', 'model.Beneficiary'),
    ('external.NeedType', 'model.NeedType'),
    ('external.TransactionManager', 'core.TransactionManager'),
    ('external.JdbcRepository', 'core.JdbcRepository'),
    ('external.DonationService', 'donation.DonationService'),
    ('external.EventTypeService', 'event.EventTypeService'),
    ('external.EventService', 'event.EventService'),
    ('external.VolunteerService', 'volunteer.VolunteerService'),
    ('external.PaymentService', 'payment.PaymentService'),
    ('external.BeneficiaryService', 'beneficiary.BeneficiaryService'),
]

def remap(text):
    for a, b in REF_MAP:
        text = re.sub(r'\bexternal\.' + re.escape(a.split('.', 1)[1]) + r'\b', b, text)
    return text

sqlite_bodies = []
parts = []

# --- model (wrap bare classes in a model: container) ---
m = strip_vars(read('10-model.d2'))
for n in ('Event', 'EventType', 'Beneficiary', 'NeedType', 'VolunteerEventAssignment',
          'Volunteer', 'Skill', 'Role', 'Permission'):
    m = re.sub(r'(?<![\w.])' + n + r' ->', 'model.' + n + ' ->', m)
    m = re.sub(r'-> ' + n + r':', '-> model.' + n + ':', m)
parts.append(
    'model: com.resala.model {\n  style.fill: "#F5F4F0"\n  style.stroke: "#C8C5BA"\n'
    '  style.font-size: 15\n  style.bold: true\n\n' + m.rstrip() + '\n}\n')

# --- core + contracts ---
c = strip_vars(read('20-core-persistence.d2'))
c = re.sub(r'AuditLog: \{ shape: class; "«model» AuditLog": "" \}\n', '', c)
c = c.replace('core.singletons.AuditLogManager -> AuditLog: "records"',
              'core.singletons.AuditLogManager -> model.AuditLog: "records"')
c = re.sub(r'\ncontracts\.DonorRepository -> impl\.note:[^\n]*\n', '\n', c)
parts.append(c.strip() + '\n')

# --- sqlite merged container ---
module_order = ['30-donation.d2', '40-payment.d2', '50-event.d2', '60-volunteer.d2',
                '70-beneficiary.d2', '80-notification.d2', '90-presentation.d2']
module_parts = []
for fname in module_order:
    s = strip_vars(read(fname))
    try:
        s, sq = extract_block(s, 'sqlite')
        sqlite_bodies.append(inner(sq))
    except ValueError:
        pass
    try:
        s, _ext = extract_block(s, 'external')
    except ValueError:
        pass
    module_parts.append(remap(s).strip() + '\n')

sqlite = ('sqlite: com.resala.persistence.sqlite {\n  style.fill: "#F5F4F0"\n  style.stroke: "#C8C5BA"\n'
          '  style.font-size: 15\n  style.bold: true\n\n')
sqlite += '\n'.join(b.rstrip() for b in sqlite_bodies) + '\n}\n'
parts.insert(1, sqlite)
parts += module_parts

header = '''# Resala Charity — Complete UML class diagram (source: src/main/java)
# GENERATED by diagrams/assemble-full.py from the module diagrams — do not edit.
vars: {
  d2-config: {
    layout-engine: tala
    theme-id: 1
  }
}
'''
OUT.write_text(header + '\n' + '\n'.join(parts))
print(f'assembled {OUT}')
