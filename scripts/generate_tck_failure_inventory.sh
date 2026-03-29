#!/usr/bin/env bash
set -euo pipefail

REPORT_DIR="${1:-/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports}"
OUT_FILE="${2:-/Users/stefano.reksten/IdeaProjects/common-utils/tck-failure-inventory.md}"

TESTNG_XML="$REPORT_DIR/testng-results.xml"
SUITE_TXT="$REPORT_DIR/TestSuite.txt"

if [[ ! -f "$TESTNG_XML" || ! -f "$SUITE_TXT" ]]; then
  echo "Missing Surefire files in $REPORT_DIR"
  exit 1
fi

python3 - "$TESTNG_XML" "$SUITE_TXT" "$OUT_FILE" <<'PY'
import re
import sys
from collections import Counter, defaultdict

xml_path, suite_path, out_path = sys.argv[1:]

xml = open(xml_path, encoding="utf-8", errors="ignore").read().splitlines()
suite = open(suite_path, encoding="utf-8", errors="ignore").read()

failed_classes = []
for line in xml:
    if 'status="FAIL"' not in line:
        continue
    m = re.search(r'instance:([^@\]]+)@', line)
    if m:
        failed_classes.append(m.group(1))

summary = re.search(r'Tests run:\s*(\d+), Failures:\s*(\d+), Errors:\s*(\d+), Skipped:\s*(\d+)', suite)
if summary:
    tests_run, failures, errors, skipped = summary.groups()
else:
    tests_run = failures = errors = skipped = "unknown"

areas = {
    "Core DI resolution & injection": ["lookup", "injectionpoint", "clientproxy", "beancontainer", "invoker"],
    "Extensions SPI & lifecycle": ["extensions", "build.compatible", "process", "aftertypediscovery", "beforebeandiscovery"],
    "Events/observers": [".event.", "observer"],
    "Interceptors": ["interceptor", "aroundinvoke", "aroundconstruct"],
    "Producers/disposers/initializers": ["producer", "dispos", "initializer"],
    "Contexts/scopes": [".context.", "passivating", "session", "conversation", "requestscoped", "applicationscoped"],
    "Alternatives/stereotypes/specialization": ["alternative", "stereotype", "specialization"],
    "Decorators": ["decorator"],
    "Bean definition/typing": ["definition", "managedbean", "generic", "bean.types", "qualifier"],
}

area_counts = Counter()
area_samples = defaultdict(list)
for cls in failed_classes:
    lc = cls.lower()
    matched = False
    for area, keys in areas.items():
        if any(k in lc for k in keys):
            area_counts[area] += 1
            if len(area_samples[area]) < 8:
                area_samples[area].append(cls)
            matched = True
            break
    if not matched:
        area_counts["Unclassified"] += 1
        if len(area_samples["Unclassified"]) < 8:
            area_samples["Unclassified"].append(cls)

cause_counter = Counter(re.findall(r'Caused by:\s*([^\n]+)', suite))

with open(out_path, "w", encoding="utf-8") as f:
    f.write("# TCK Failure Inventory\n\n")
    f.write(f"- Source: `{xml_path}` and `{suite_path}`\n")
    f.write(f"- Summary: Tests run: {tests_run}, Failures: {failures}, Errors: {errors}, Skipped: {skipped}\n")
    f.write(f"- Failed class entries: {len(failed_classes)}\n\n")

    f.write("## Top Causes\n")
    for msg, count in cause_counter.most_common(20):
        f.write(f"- {count}x `{msg}`\n")
    f.write("\n")

    f.write("## By Functional Area\n")
    for area, count in area_counts.most_common():
        f.write(f"\n### {area} ({count})\n")
        for cls in area_samples[area]:
            f.write(f"- [ ] **{cls}**\n")

print(out_path)
PY

echo "Wrote inventory to $OUT_FILE"
