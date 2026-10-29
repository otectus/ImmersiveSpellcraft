#!/usr/bin/env python3
"""Renders docs/coverage.md from the runtime coverage manifest docs/coverage.json.

The manifest is written by the running game (-PcoverageExport=<file> on a run task, or
/immersivespellcraft coverage export); this script only formats it, so every number in the Markdown
comes from that run. Usage: python3 tools/coverage_report.py [manifest] [output]
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "coverage.json")
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "docs", "coverage.md")


def cell(text):
    return str(text).replace("|", "\\|").replace("\n", " ")


def main():
    m = json.load(open(SRC, encoding="utf-8"))
    lines = ["# Immersive Spellcraft coverage manifest", ""]
    lines.append(f"Generated from the running game by `{m['generated_by']}` at {m['generated_at']}, run profile "
                 f"`{m['profile']}`. Machine-readable source: [`coverage.json`](coverage.json). Regenerate with "
                 "`gradlew runGameTestServer -Pwith=all -PcoverageExport=$PWD/docs/coverage.json` and "
                 "`python3 tools/coverage_report.py`.")
    lines.append("")
    s = m["summary"]
    lines.append("## Summary")
    lines.append("")
    lines.append("| Status | Operation groups |")
    lines.append("| --- | --- |")
    for k in ("performed", "attuned", "native_preserved", "unavailable_in_this_version", "unsupported"):
        lines.append(f"| `{k}` | {s.get(k, 0)} |")
    lines.append("")
    lines.append("`unsupported` is a release gap, not a synonym for preserved: the station's native behavior is "
                 "untouched, but its required treatment is not built yet; the milestone column names when it is due. "
                 "Registry entries are listed below but not yet triaged one by one (that is the M4 gate).")
    lines.append("")

    lines.append("## Loaded mods")
    lines.append("")
    lines.append("| Mod | Loaded | Version | File | SHA-256 |")
    lines.append("| --- | --- | --- | --- | --- |")
    for mod, d in m["mods"].items():
        lines.append(f"| `{mod}` | {'yes' if d.get('loaded') else 'no'} | {cell(d.get('version', ''))} | "
                     f"{cell(d.get('file', ''))} | {('`' + d['sha256'] + '`') if d.get('sha256') else ''} |")
    lines.append("")
    lines.append("Hashes are of the files the run loaded. In a development run the target mods are ForgeGradle's "
                 "deobfuscated copies; the release artifacts' hashes are pinned in [compatibility.md](compatibility.md).")
    lines.append("")

    lines.append("## Integrations and owners")
    lines.append("")
    lines.append("| Integration | Version | Supported range | Active | Detail |")
    lines.append("| --- | --- | --- | --- | --- |")
    for mod, d in m["integrations"].items():
        lines.append(f"| `{mod}` | {cell(d.get('version') or 'absent')} | {cell(d['supported_range'])} | "
                     f"{'yes' if d['active'] else 'no'} | {cell(d['detail'])} |")
    lines.append("")
    lines.append("| Operation | Adapter | Capability | Game |")
    lines.append("| --- | --- | --- | --- |")
    for op, d in m["operation_owners"].items():
        lines.append(f"| `{op}` | `{d['adapter']}` v{d['adapter_version']} | {d['capability']} | `{d['game']}` |")
    lines.append("")
    if m.get("compat_mixins_applied"):
        lines.append("Compat mixins applied: " + ", ".join(f"`{k}` {'yes' if v else 'no'}" for k, v in m["compat_mixins_applied"].items()) + ".")
        lines.append("")
    if m.get("ownership_diagnostics"):
        lines.append("Ownership diagnostics: " + "; ".join(m["ownership_diagnostics"]))
        lines.append("")

    lines.append("## Operations")
    lines.append("")
    lines.append("| ID | System | Target treatment | Status | Adapter / profile | Milestone | Notes |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    for o in m["operations"]:
        ap = ""
        if o.get("adapter"):
            ap = f"`{o['adapter']}`" + (" (active)" if o.get("adapter_active") else " (dormant here)")
            if o.get("profile"):
                ap += f"<br>`{o['profile']}`"
        lines.append(f"| {o['id']} | {cell(o['system'])} | {cell(o['target_treatment'])} | `{o['status']}` | {ap} | "
                     f"{o['milestone']} | {cell(o['notes'])} |")
    lines.append("")
    lines.append("### Native entry and commit points of the implemented operations")
    lines.append("")
    for o in m["operations"]:
        if o.get("native_entry_and_commit"):
            lines.append(f"- **{o['id']}**: {o['native_entry_and_commit']} Entry routes: {o.get('entry_routes', '')}")
    lines.append("")

    lines.append("## Registered surfaces of the target mods (untriaged inventory)")
    lines.append("")
    for ns, r in m["registries"].items():
        lines.append(f"### `{ns}`")
        lines.append("")
        lines.append(f"- {len(r['blocks_with_block_entity'])} blocks with a block entity, "
                     f"{r['blocks_without_block_entity_count']} without; {r['item_count']} items; {r['entity_type_count']} entity types.")
        lines.append(f"- Block entity types ({len(r['block_entity_types'])}): " + ", ".join(f"`{x}`" for x in r["block_entity_types"]))
        lines.append(f"- Menu types ({len(r['menu_types'])}): " + ", ".join(f"`{x}`" for x in r["menu_types"]))
        lines.append(f"- Recipe types ({len(r['recipe_types'])}): " + ", ".join(f"`{x}`" for x in r["recipe_types"]))
        lines.append(f"- Recipe serializers ({len(r['recipe_serializers'])}): " + ", ".join(f"`{x}`" for x in r["recipe_serializers"]))
        lines.append(f"- Functional blocks: " + ", ".join(f"`{x}`" for x in r["blocks_with_block_entity"]))
        lines.append("")
    lines.append("### Loaded recipes by type")
    lines.append("")
    lines.append("| Recipe type | Count |")
    lines.append("| --- | --- |")
    for t, d in m["recipes_by_type"].items():
        lines.append(f"| `{t}` | {d['count']} |")
    lines.append("")
    inv = m.get("integration_inventories", {})
    if inv:
        lines.append("### Integration inventories")
        lines.append("")
        a = inv.get("ars_nouveau")
        if a:
            counts = ", ".join(f"`{k}` {v}" for k, v in a.get("glyph_count_by_namespace", {}).items())
            lines.append(f"- Ars Nouveau glyphs by namespace: {counts}; rituals ({a.get('ritual_count', 0)}): "
                         + ", ".join(f"`{x}`" for x in a.get("rituals", [])))
        i = inv.get("irons_spellbooks")
        if i:
            lines.append(f"- Iron's spells: {i.get('spell_count')} registered, {i.get('enabled_spell_count')} enabled; schools: "
                         + ", ".join(f"`{x}`" for x in i.get("schools", [])))
        n = inv.get("ars_n_spells")
        if n:
            lines.append(f"- Ars 'n Spells rituals: " + ", ".join(f"`{x}`" for x in n.get("rituals", []))
                         + f"; imported Iron's glyphs: {n.get('imported_glyph_count')}")
        lines.append("")
        lines.append("The full glyph, spell and recipe-id lists are in `coverage.json`.")
        lines.append("")
    open(OUT, "w", encoding="utf-8").write("\n".join(lines))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
