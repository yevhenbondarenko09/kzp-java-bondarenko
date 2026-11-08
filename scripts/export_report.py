"""Export the audited Markdown report to DOCX using the Python standard library."""
from pathlib import Path
import re
from zipfile import ZipFile, ZIP_DEFLATED
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
ET.register_namespace('w', W)


def el(tag, attrs=None):
    return ET.Element(f'{{{W}}}{tag}', {f'{{{W}}}{k}': str(v) for k, v in (attrs or {}).items()})


def sub(parent, tag, attrs=None):
    item = el(tag, attrs)
    parent.append(item)
    return item


def paragraph(parent, text='', style=None, code=False):
    p = sub(parent, 'p')
    if style:
        sub(sub(p, 'pPr'), 'pStyle', {'val': style})
    pieces = [text] if code else re.split(r'(\*\*.*?\*\*|`[^`]+`)', text)
    for piece in pieces:
        if not piece:
            continue
        bold = piece.startswith('**') and piece.endswith('**') and not code
        mono = code or (piece.startswith('`') and piece.endswith('`'))
        if bold:
            piece = piece[2:-2]
        elif mono and not code:
            piece = piece[1:-1]
        r = sub(p, 'r')
        if bold or mono:
            props = sub(r, 'rPr')
            if bold:
                sub(props, 'b')
            if mono:
                sub(props, 'rFonts', {'ascii': 'Consolas', 'hAnsi': 'Consolas'})
                sub(props, 'sz', {'val': '19'})
        t = sub(r, 't')
        t.set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
        t.text = piece
    return p


def table(parent, lines):
    rows = [[c.strip() for c in line.strip().strip('|').split('|')] for line in lines]
    rows = [row for row in rows if not all(re.fullmatch(r':?-+:?', c) for c in row)]
    tbl = sub(parent, 'tbl')
    props = sub(tbl, 'tblPr')
    sub(props, 'tblW', {'w': '9638', 'type': 'dxa'})
    borders = sub(props, 'tblBorders')
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        sub(borders, edge, {'val': 'single', 'sz': '4', 'color': 'AAAAAA'})
    grid = sub(tbl, 'tblGrid')
    width = 9638 // len(rows[0])
    for _ in rows[0]:
        sub(grid, 'gridCol', {'w': str(width)})
    for index, row in enumerate(rows):
        tr = sub(tbl, 'tr')
        if index == 0:
            sub(sub(tr, 'trPr'), 'tblHeader')
        for cell in row:
            tc = sub(tr, 'tc')
            cp = sub(tc, 'tcPr')
            sub(cp, 'tcW', {'w': str(width), 'type': 'dxa'})
            if index == 0:
                sub(cp, 'shd', {'fill': 'E8EEF4'})
                cell = f'**{cell}**'
            paragraph(tc, cell, 'TableText')
    paragraph(parent)


source = ROOT.joinpath('REPORT.md').read_text(encoding='utf-8')
document = el('document')
body = sub(document, 'body')
lines = source.splitlines()
i = 0
in_code = False
while i < len(lines):
    line = lines[i].rstrip()
    if line.startswith('```'):
        in_code = not in_code
    elif in_code:
        paragraph(body, line, 'Code', code=True)
    elif line.startswith('|'):
        block = []
        while i < len(lines) and lines[i].startswith('|'):
            block.append(lines[i])
            i += 1
        table(body, block)
        continue
    elif line.startswith('### '):
        paragraph(body, line[4:], 'Heading2')
    elif line.startswith('## '):
        paragraph(body, line[3:], 'Heading1')
    elif line.startswith('# '):
        paragraph(body, line[2:], 'Title')
    elif line:
        paragraph(body, line)
    i += 1

section = sub(body, 'sectPr')
sub(section, 'pgSz', {'w': '11906', 'h': '16838'})
sub(section, 'pgMar', {'top': '1134', 'right': '1134', 'bottom': '1134', 'left': '1134', 'header': '567', 'footer': '567', 'gutter': '0'})

styles = el('styles')
defaults = sub(styles, 'docDefaults')
rp = sub(sub(defaults, 'rPrDefault'), 'rPr')
sub(rp, 'rFonts', {'ascii': 'Times New Roman', 'hAnsi': 'Times New Roman', 'cs': 'Times New Roman'})
sub(rp, 'sz', {'val': '24'})
sub(rp, 'lang', {'val': 'uk-UA'})
pp = sub(sub(defaults, 'pPrDefault'), 'pPr')
sub(pp, 'spacing', {'after': '120', 'line': '276', 'lineRule': 'auto'})
for name, size, bold in [('Normal', 24, False), ('Title', 36, True), ('Heading1', 28, True), ('Heading2', 25, True), ('Code', 19, False), ('TableText', 21, False)]:
    style = sub(styles, 'style', {'type': 'paragraph', 'styleId': name})
    if name == 'Normal':
        style.set(f'{{{W}}}default', '1')
    sub(style, 'name', {'val': name})
    sp = sub(style, 'pPr')
    if name in ('Title', 'Heading1', 'Heading2'):
        sub(sp, 'keepNext')
        sub(sp, 'spacing', {'before': '240', 'after': '120'})
    if name in ('Code', 'TableText'):
        sub(sp, 'spacing', {'after': '40', 'line': '240', 'lineRule': 'auto'})
    sr = sub(style, 'rPr')
    sub(sr, 'sz', {'val': str(size)})
    if bold:
        sub(sr, 'b')

content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>'''
rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''
doc_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>'''
output = ROOT / 'REPORT.docx'
with ZipFile(output, 'w', ZIP_DEFLATED) as package:
    package.writestr('[Content_Types].xml', content_types)
    package.writestr('_rels/.rels', rels)
    package.writestr('word/_rels/document.xml.rels', doc_rels)
    package.writestr('word/document.xml', ET.tostring(document, encoding='utf-8', xml_declaration=True))
    package.writestr('word/styles.xml', ET.tostring(styles, encoding='utf-8', xml_declaration=True))

with ZipFile(output) as package:
    assert package.testzip() is None
    for name in package.namelist():
        if name.endswith(('.xml', '.rels')):
            ET.fromstring(package.read(name))
    text = ''.join(ET.fromstring(package.read('word/document.xml')).itertext())
    for token in ('Бондаренко Євген Віталійович', 'КІ-304', '8800.00', '12. Висновки', '--input'):
        assert token in text, token
print(f'Created and structurally validated {output.name}: {output.stat().st_size} bytes')
