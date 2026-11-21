"""Reads the Java sources of NextFit and builds a small model of every top-level type.

Only the standard library is used. The model feeds generate_docs.py, which writes the
catalog and the diagrams, so the documents always match the code.
"""
import os
import re
from dataclasses import dataclass, field

ROOT = os.path.join("src", "main", "java", "com", "nextfit")

DECL = re.compile(
    r"^(?P<mods>(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp)\s+)*)"
    r"(?P<kind>class|interface|enum|record)\s+(?P<name>[A-Z]\w*)(?P<generics><[^{(]*?>)?"
    r"(?P<rest>[^{;]*)", re.M)


@dataclass
class Member:
    name: str
    type: str
    doc: str = ""
    params: str = ""


@dataclass
class JType:
    name: str
    package: str
    kind: str
    abstract: bool
    doc: str
    extends: list = field(default_factory=list)
    implements: list = field(default_factory=list)
    fields: list = field(default_factory=list)
    methods: list = field(default_factory=list)
    constants: list = field(default_factory=list)
    file: str = ""

    @property
    def label(self):
        if self.kind == "interface":
            return "Interface"
        if self.kind == "enum":
            return "Enum"
        if self.kind == "record":
            return "Record"
        return "Abstract class" if self.abstract else "Class"


def first_sentence(text):
    text = re.sub(r"\{@(?:link|code|linkplain)\s+([^}]*)\}", lambda m: m.group(1).split()[-1].lstrip("#"), text)
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return ""
    match = re.match(r"(.+?[.!?])(\s|$)", text)
    return match.group(1) if match else text


def javadoc_before(source, index):
    head = source[:index].rstrip()
    head = re.sub(r"(?:@\w+(?:\([^)]*\))?\s*)+$", "", head).rstrip()
    if not head.endswith("*/"):
        return ""
    start = head.rfind("/**")
    if start < 0:
        return ""
    body = head[start + 3:-2]
    lines = [re.sub(r"^\s*\*\s?", "", line) for line in body.splitlines()]
    kept = []
    for line in lines:
        if line.strip().startswith("@"):
            break
        kept.append(line)
    return first_sentence(" ".join(kept))


def strip_comments_and_strings(source):
    source = re.sub(r"/\*(?!\*).*?\*/", " ", source, flags=re.S)
    source = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', source)
    return source


def names(text):
    text = re.sub(r"<[^<>]*(?:<[^<>]*>[^<>]*)*>", "", text)
    return [n.strip().split(".")[-1] for n in text.split(",") if n.strip()]


def body_of(source, open_index):
    depth = 0
    for i in range(open_index, len(source)):
        c = source[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return source[open_index + 1:i]
    return source[open_index + 1:]


def top_level(body):
    """Keeps only the text at brace depth 0 of the type body (members of the type itself)."""
    out, depth = [], 0
    for c in body:
        if c == "{":
            depth += 1
            if depth == 1:
                out.append("{")
            continue
        if c == "}":
            depth -= 1
            if depth == 0:
                out.append("}")
            continue
        if depth == 0:
            out.append(c)
    return "".join(out)


FIELD = re.compile(r"^\s*(?:(?:private|protected|public|static|final|volatile|transient)\s+)+"
                   r"(?P<type>[\w.<>\[\], ?]+?)\s+(?P<name>\w+)\s*(?:=[^;]*)?;", re.M)
METHOD = re.compile(r"^\s*(?P<mods>(?:(?:public|static|final|abstract|default|synchronized)\s+)*)"
                    r"(?:<[^>]+>\s+)?(?P<type>[\w.<>\[\], ?]+?)\s+(?P<name>[a-z]\w*)\s*\((?P<params>[^)]*)\)"
                    r"\s*(?:throws [\w., ]+)?\s*[{;]", re.M)


def parse_file(path):
    raw = open(path, encoding="utf-8").read()
    package = re.search(r"^package ([\w.]+);", raw, re.M).group(1)[len("com.nextfit."):] or "."
    if package == "com.nextfit":
        package = "."
    package = re.search(r"^package ([\w.]+);", raw, re.M).group(1).replace("com.nextfit", "", 1).lstrip(".") or "(root)"
    clean = strip_comments_and_strings(raw)
    match = None
    for m in DECL.finditer(clean):
        if m.group("mods").strip() == "" or "public" in m.group("mods") or True:
            line_start = clean.rfind("\n", 0, m.start()) + 1
            if m.start() == line_start:
                match = m
                break
    if not match:
        return None
    kind = match.group("kind")
    rest = match.group("rest")
    ext = re.search(r"\bextends\s+(.+?)(?=\bimplements\b|\bpermits\b|$)", rest, re.S)
    imp = re.search(r"\bimplements\s+(.+?)(?=\bpermits\b|$)", rest, re.S)
    extends = names(ext.group(1)) if ext else []
    implements = names(imp.group(1)) if imp else []
    if kind == "interface":
        implements, extends = [], extends
    doc = javadoc_before(raw, raw.find(match.group(0).strip().split("{")[0][:40]))
    jtype = JType(match.group("name"), package, kind, "abstract" in match.group("mods"), doc,
                  extends, implements, file=path)
    open_index = clean.find("{", match.end() - 1)
    # record components
    if kind == "record":
        header = clean[match.end("name"): clean.find("{", match.end("name"))]
        comp = re.search(r"\((.*)\)", header, re.S)
        if comp:
            depth, cur, parts = 0, "", []
            for c in comp.group(1):
                if c in "<(":
                    depth += 1
                elif c in ">)":
                    depth -= 1
                if c == "," and depth == 0:
                    parts.append(cur)
                    cur = ""
                else:
                    cur += c
            parts.append(cur)
            for part in parts:
                bits = part.strip().rsplit(None, 1)
                if len(bits) == 2:
                    jtype.fields.append(Member(bits[1], re.sub(r"@\w+\s*", "", bits[0]).strip()))
    if open_index < 0:
        return jtype
    body = top_level(body_of(clean, open_index))
    raw_body = body_of(raw, raw.find("{", raw.find(match.group("name"), 0)))
    if kind == "enum":
        head = body.split(";", 1)[0] if ";" in body else body
        jtype.constants = [c.strip().split("(")[0].strip() for c in re.split(r",\s*(?![^()]*\))", head)
                           if re.match(r"^\s*[A-Z_0-9]+\b", c)][:20]
    for f in FIELD.finditer(body):
        mods = f.group(0)
        if "static" in mods.split(f.group("name"))[0]:
            continue
        jtype.fields.append(Member(f.group("name"), " ".join(f.group("type").split())))
    for m in METHOD.finditer(body):
        mods = m.group("mods")
        if m.group("type") in ("return", "new", "else", "throw"):
            continue
        public = "public" in mods or kind == "interface"
        if not public:
            continue
        doc = ""
        idx = raw_body.find(m.group("name") + "(")
        if idx > 0:
            doc = javadoc_before(raw_body, raw_body.rfind("\n", 0, idx))
        jtype.methods.append(Member(m.group("name"), " ".join(m.group("type").split()), doc,
                                    " ".join(m.group("params").split())))
    return jtype


def load():
    types = []
    for d, _, files in os.walk(ROOT):
        for f in sorted(files):
            if f.endswith(".java"):
                t = parse_file(os.path.join(d, f))
                if t:
                    types.append(t)
    return types
