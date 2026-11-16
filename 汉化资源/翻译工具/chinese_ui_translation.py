#!/usr/bin/env python3
"""Extract desktop UI strings and build the Simplified Chinese lookup table."""

from __future__ import annotations

import json
import pathlib
import re
import time
import urllib.parse
import urllib.request


ROOT = pathlib.Path(__file__).resolve().parents[1]
CACHE = ROOT / "tools" / "zh-translations.json"
OUTPUT = ROOT / "src" / "commonMain" / "kotlin" / "com" / "usagemonitor" / "presentation" / "ui" / "ZhTranslations.kt"

UI_EXTRA_FILES = {
    "SettingsWindowHost.kt",
    "ModalWindowsHost.kt",
    "HelpWindow.kt",
    "PdfUsageReportRenderer.kt",
    "SettingsHudPreview.kt",
    "StartupTipWindow.kt",
    "ReleaseNotesWindow.kt",
    "TrayMenuLabels.kt",
    "HudWindowDecisions.kt",
    "AppWindowAnchor.kt",
    "CommandLineHelp.kt",
    "BugReportHost.kt",
    "DesktopBugReportMachineInfo.kt",
}

LITERAL = re.compile(r'^"(?:[^"\\]|\\.)*"$', re.S)
INLINE_IF = re.compile(
    r'if\s*\(([^)\n]{0,120}?(?:AppLanguage\.PT|isPt|portuguese)[^)\n]{0,120}?)\)'
    r'\s*"((?:[^"\\]|\\.)*)"\s*else\s*"((?:[^"\\]|\\.)*)"'
)
PLACEHOLDER = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*|%[sdfo]|\{[A-Za-z_][A-Za-z0-9_]*\}")
HTML_TAG = re.compile(r"</?[A-Za-z][^>]*>")
SEPARATOR = "\n@@@\n"

OVERRIDES = {
    "5h session": "5 小时窗口",
    "By member": "按成员",
    "By branch": "按分支",
    "turns": "轮次",
    "Turns": "轮次",
    "$count turns": "$count 轮",
    "$count previous windows": "前 $count 个窗口",
    "1 previous window": "前 1 个窗口",
    "points": "个百分点",
    "point": "个百分点",
    "tokens": "Token",
    "Tokens": "Token",
    "Avg context/turn": "平均上下文/轮",
    "Burn rate": "消耗速度",
    "Usage credits": "用量额度",
    "Credits": "额度",
}


def is_ui_file(path: pathlib.Path) -> bool:
    text = str(path)
    return "/presentation/ui/" in text or path.name in UI_EXTRA_FILES


def decode_literal(raw: str) -> str:
    body = raw[1:-1]
    result: list[str] = []
    index = 0
    while index < len(body):
        char = body[index]
        if char != "\\" or index + 1 >= len(body):
            result.append(char)
            index += 1
            continue
        escaped = body[index + 1]
        result.append({"n": "\n", "r": "\r", "t": "\t", '"': '"', "'": "'", "\\": "\\", "$": "$"}.get(escaped, escaped))
        index += 2
    return "".join(result)


def extract_localized_pairs(text: str) -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    for match in re.finditer(r"localized\s*\(", text):
        index = match.end()
        depth = 1
        start = index
        arguments: list[str] = []
        in_string = False
        escaped = False
        while index < len(text) and depth:
            char = text[index]
            if in_string:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == '"':
                    in_string = False
            else:
                if char == '"':
                    in_string = True
                elif char in "([":
                    depth += 1
                elif char in ")]":
                    depth -= 1
                    if depth == 0:
                        arguments.append(text[start:index].strip())
                        break
                elif char == "," and depth == 1:
                    arguments.append(text[start:index].strip())
                    start = index + 1
            index += 1
        if len(arguments) == 3 and LITERAL.fullmatch(arguments[1]) and LITERAL.fullmatch(arguments[2]):
            pairs.append((decode_literal(arguments[1]), decode_literal(arguments[2])))
        elif len(arguments) == 2 and LITERAL.fullmatch(arguments[0]) and LITERAL.fullmatch(arguments[1]):
            pairs.append((decode_literal(arguments[0]), decode_literal(arguments[1])))
    return pairs


def read_string_expression(text: str, start: int) -> tuple[str, int]:
    pieces: list[str] = []
    index = start
    while True:
        while index < len(text) and text[index].isspace():
            index += 1
        if index >= len(text) or text[index] != '"':
            raise ValueError(f"expected string at {index}")
        end = index + 1
        escaped = False
        while end < len(text):
            char = text[end]
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                break
            end += 1
        pieces.append(decode_literal(text[index : end + 1]))
        index = end + 1
        while index < len(text) and text[index].isspace():
            index += 1
        if index < len(text) and text[index] == "+":
            index += 1
            continue
        return "".join(pieces), index


def split_arguments(text: str, start: int) -> tuple[list[str], int]:
    arguments: list[str] = []
    index = start
    while index < len(text):
        while index < len(text) and text[index].isspace():
            index += 1
        try:
            value, index = read_string_expression(text, index)
            arguments.append(value)
        except ValueError:
            break
        while index < len(text) and text[index].isspace():
            index += 1
        if index < len(text) and text[index] == ",":
            index += 1
            continue
        break
    return arguments, index


CONCATENATED_STRING = r'"(?:[^"\\]|\\.)*"(?:\s*\+\s*"(?:[^"\\]|\\.)*")*'


def evaluate_concatenation(expression: str) -> str:
    return "".join(decode_literal(match.group(0)) for match in re.finditer(r'"(?:[^"\\]|\\.)*"', expression, re.S))


def extract_long_strings() -> list[str]:
    values: set[str] = set()
    onboarding = ROOT / "src/commonMain/kotlin/com/usagemonitor/presentation/ui/help/OnboardingGuide.kt"
    text = onboarding.read_text(errors="replace")
    for match in re.finditer(r"(?:titleEn|textEn)\s*=\s*", text):
        value, _ = read_string_expression(text, match.end())
        values.add(value)

    tips = ROOT / "src/commonMain/kotlin/com/usagemonitor/presentation/ui/help/StartupTips.kt"
    text = tips.read_text(errors="replace")
    pattern = re.compile(
        rf"StartupTip\(\s*HelpTopic\.\w+\s*,\s*({CONCATENATED_STRING})\s*,\s*({CONCATENATED_STRING})",
        re.S,
    )
    for match in pattern.finditer(text):
        values.add(evaluate_concatenation(match.group(2)))

    catalog = ROOT / "src/commonMain/kotlin/com/usagemonitor/presentation/ui/help/HelpCatalogEn.kt"
    text = catalog.read_text(errors="replace")
    for match in re.finditer(r"(?:title|summary|description)\s*=\s*", text):
        value, _ = read_string_expression(text, match.end())
        values.add(value)
    for match in re.finditer(r"steps\s*=\s*listOf\s*\(", text):
        arguments, _ = split_arguments(text, match.end())
        values.update(arguments)

    values.add(
        "One a day, on the day's first opening, startup with the system included. Every feature stays in Help."
    )
    return sorted(values)


def collect_english_strings() -> list[str]:
    english: set[str] = set()
    for path in (ROOT / "src").rglob("*.kt"):
        if not is_ui_file(path):
            continue
        text = path.read_text(errors="replace")
        for _, english_text in extract_localized_pairs(text):
            english.add(english_text)
        for match in INLINE_IF.finditer(text):
            english.add(decode_literal('"' + match.group(3) + '"'))
    return sorted(english)


def protect(text: str) -> tuple[str, list[str]]:
    saved: list[str] = []

    def replace(match: re.Match[str]) -> str:
        saved.append(match.group(0))
        return f"__PH{len(saved) - 1}__"

    protected = HTML_TAG.sub(replace, text)
    protected = PLACEHOLDER.sub(replace, protected)
    return protected, saved


def restore(text: str, saved: list[str]) -> str:
    for index, original in enumerate(saved):
        text = text.replace(f"__PH{index}__", original)
    return text


def translate_batch(batch: list[str]) -> list[str]:
    protected: list[str] = []
    saved_per_item: list[list[str]] = []
    for item in batch:
        value, saved = protect(item)
        protected.append(value)
        saved_per_item.append(saved)
    query = SEPARATOR.join(protected)
    url = "https://api.mymemory.translated.net/get?" + urllib.parse.urlencode(
        {"q": query, "langpair": "en|zh-CN", "de": "usage-monitor-l10n@example.com"}
    )
    with urllib.request.urlopen(url, timeout=60) as response:
        payload = json.load(response)
    translated = payload["responseData"]["translatedText"]
    translated = (
        translated.replace("&#10;", "\n")
        .replace("&quot;", '"')
        .replace("&#39;", "'")
        .replace("&amp;", "&")
    )
    parts = translated.split("@@@")
    if len(parts) != len(batch):
        raise RuntimeError(f"batch split mismatch: {len(parts)} != {len(batch)}")
    result: list[str] = []
    for index, part in enumerate(parts):
        if index:
            part = part.lstrip("\n")
        if index < len(parts) - 1:
            part = part.rstrip("\n")
        result.append(restore(part.strip(), saved_per_item[index]))
    return result


def load_cache() -> dict[str, str]:
    if CACHE.exists():
        return json.loads(CACHE.read_text())
    return {}


def main() -> None:
    english = sorted(set(collect_english_strings()) | set(extract_long_strings()))
    cache = load_cache()
    missing = [text for text in english if text not in cache]
    for offset in range(0, len(missing), 20):
        batch = missing[offset : offset + 20]
        try:
            translations = translate_batch(batch)
        except Exception as error:  # noqa: BLE001 - retry each item once
            print(f"batch failed ({error}); retrying one by one")
            translations = []
            for item in batch:
                translations.append(translate_batch([item])[0])
                time.sleep(0.15)
        for source, target in zip(batch, translations):
            cache[source] = target
        CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
        print(f"translated {min(offset + len(batch), len(missing))}/{len(missing)}")
        time.sleep(0.2)

    def kotlin_literal(value: str) -> str:
        escaped = (
            value.replace("\\", "\\\\")
            .replace('"', '\\"')
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        )
        return f'"{escaped}"'

    lines = [
        "package com.usagemonitor.presentation.ui",
        "",
        "/** Traducoes do ingles para o chines simplificado, por texto de origem. */",
        "internal object ZhTranslations {",
        "    private val exact: Map<String, String> = mapOf(",
    ]
    entries: list[str] = []
    for source in sorted(cache):
        target = cache[source]
        target = OVERRIDES.get(source, target)
        entries.append(f"{kotlin_literal(source)} to {kotlin_literal(target)},")
    for index in range(0, len(entries), 2):
        lines.append("        " + " ".join(entries[index : index + 2]))
    lines.extend(
        [
            "    )",
            "",
            "    private val placeholder = Regex(\"\"\"\\$\\{[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*|%[sdfo]|\\{[A-Za-z_][A-Za-z0-9_]*\\}\"\"\")",
            "",
            "    private class DynamicEntry(",
            "        val pattern: Regex,",
            "        val translated: String,",
            "        val placeholders: List<String>",
            "    )",
            "",
            "    private val dynamic: List<DynamicEntry> by lazy {",
            "        exact.mapNotNull { (source, translated) ->",
            "            val placeholders = placeholder.findAll(source).map { match -> match.value }.toList()",
            "            if (placeholders.isEmpty()) null else DynamicEntry(Regex(patternOf(source, placeholders)), translated, placeholders)",
            "        }.sortedByDescending { entry -> entry.placeholders.size }",
            "    }",
            "",
            "    private fun patternOf(source: String, placeholders: List<String>): String {",
            "        val pattern = StringBuilder(\"^\")",
            "        var cursor = 0",
            "        for (item in placeholders) {",
            "            val position = source.indexOf(item, cursor)",
            "            pattern.append(escape(source.substring(cursor, position))).append(\"(.+?)\")",
            "            cursor = position + item.length",
            "        }",
            "        pattern.append(escape(source.substring(cursor))).append('$')",
            "        return pattern.toString()",
            "    }",
            "",
            "    private fun escape(text: String): String {",
            "        val escaped = StringBuilder()",
            "        for (char in text) {",
            "            if (char in \"\\\\^$.*+?()[]{}|\") escaped.append('\\\\')",
            "            escaped.append(char)",
            "        }",
            "        return escaped.toString()",
            "    }",
            "",
            "    fun of(english: String): String {",
            "        exact[english]?.let { translated -> return translated }",
            "        for (entry in dynamic) {",
            "            val match = entry.pattern.matchEntire(english) ?: continue",
            "            var translated = entry.translated",
            "            entry.placeholders.forEachIndexed { index, item ->",
            "                translated = translated.replace(item, match.groupValues[index + 1])",
            "            }",
            "            return translated",
            "        }",
            "        return english",
            "    }",
            "}",
            "",
        ]
    )
    OUTPUT.write_text("\n".join(lines))
    print(f"wrote {OUTPUT} with {len(english)} entries")


if __name__ == "__main__":
    main()
