#!/usr/bin/env python3
import json
import os
import sys
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

# ── Frameworks kept after the ABQ/LBQ/CLQ removal ──────────────────────────
FRAMEWORKS = [
    'zThread',
    'SynchronousQueue',
    'Netty',
    'Vert.x',
    'Reactor',
    'Virtual Threads',
]

# SpscEventBenchmark method → display name
SPSC_NAME_MAP = {
    'benchZThread':          'zThread',
    'benchNetty':            'Netty',
    'benchVertx':            'Vert.x',
    'benchReactor':          'Reactor',
    'benchVirtualThreads':   'Virtual Threads',
    'benchSynchronousQueue': 'SynchronousQueue',
}

# S1_S5 framework param → display name
SCALING_NAME_MAP = {
    'ZTHREAD':          'zThread',
    'NETTY':            'Netty',
    'VERTX':            'Vert.x',
    'REACTOR':          'Reactor',
    'VIRTUAL_THREADS':  'Virtual Threads',
    'SYNCHRONOUS_QUEUE':'SynchronousQueue',
    'THREAD_POOL':      'Thread Pool',
    'FORK_JOIN':        'Fork/Join',
}

# README table: full display name → arch description
FULL_NAME_MAP = {
    'benchZThread':          'zThread (Linux FFM / Epoll)',
    'benchNetty':            'Netty (NIO EventLoop)',
    'benchVertx':            'Vert.x (Event Loop)',
    'benchReactor':          'Project Reactor (Schedulers)',
    'benchVirtualThreads':   'Java Virtual Threads (Loom)',
    'benchSynchronousQueue': 'SynchronousQueue',
}

ARCH_MAP = {
    'zThread (Linux FFM / Epoll)':   'Kernel `epoll` + Lock-free RingBuffer via Panama FFM',
    'Project Reactor (Schedulers)':  'RingBuffer-backed Schedulers',
    'SynchronousQueue':              'Dual stack / queue thread handoff',
    'Vert.x (Event Loop)':          'Netty-backed event loop dispatch',
    'Netty (NIO EventLoop)':        '`Selector` + ConcurrentLinkedQueue dispatch',
    'Java Virtual Threads (Loom)':  'Carrier thread park/unpark overhead',
}

# ── Color palette (modern dark-theme) ──────────────────────────────────────
FRAMEWORK_COLORS = {
    'zThread':          '#10b981',  # emerald
    'Netty':            '#38bdf8',  # sky
    'Vert.x':           '#a78bfa',  # violet
    'Reactor':          '#fb923c',  # orange
    'Virtual Threads':  '#f87171',  # rose
    'SynchronousQueue': '#facc15',  # amber
    'Thread Pool':      '#34d399',  # teal
    'Fork/Join':        '#60a5fa',  # blue
}

BG_COLOR = '#0d1117'
GRID_COLOR = '#1e293b'
TEXT_COLOR = '#e2e8f0'
SUBTLE_COLOR = '#94a3b8'


def parse_benchmark_data(data):
    """Parse merged JMH JSON into SPSC and scaling data dicts keyed by framework display name."""
    spsc = {}   # { display_name: throughput_ops_s }
    # scaling: { (display_name, concurrency): throughput_ops_s }
    scaling_low = {}
    scaling_high = {}

    for entry in data:
        name = entry.get('benchmark', '')
        score = float(entry.get('primaryMetric', {}).get('score', 0))

        if 'SpscEventBenchmark' in name:
            method = name.split('.')[-1]
            dname = SPSC_NAME_MAP.get(method)
            if dname:
                spsc[dname] = score

        elif 'S1_S5_ThroughputLatencyBenchmark' in name:
            params = entry.get('params', {})
            fw_raw = params.get('framework', '')
            conc = params.get('concurrency', '1:1')
            dname = SCALING_NAME_MAP.get(fw_raw)
            if not dname:
                continue
            if conc in ('1:1', '4:1'):
                scaling_low.setdefault(dname, {})[conc] = score
            elif conc in ('8:1', '16:4', '32:8'):
                scaling_high.setdefault(dname, {})[conc] = score

    return spsc, scaling_low, scaling_high


def draw_radar_chart(spsc, scaling_low, scaling_high, output_path):
    """Generate a modern dark-theme radar chart with 3 series: SPSC, Scaling-Low, Scaling-High."""

    # Use the intersection of all 3 datasets as axes
    common_fw = [fw for fw in FRAMEWORKS if fw in spsc]
    if not common_fw:
        print("No common frameworks for radar chart.")
        return

    N = len(common_fw)
    angles = np.linspace(0, 2 * np.pi, N, endpoint=False).tolist()
    angles += angles[:1]  # close the polygon

    # Collect raw values for each series (average across concurrency levels for scaling)
    def avg_scaling(scaling_dict, fw):
        vals = scaling_dict.get(fw, {})
        return np.mean(list(vals.values())) if vals else 0

    spsc_vals = [spsc.get(fw, 0) for fw in common_fw]
    slo_vals = [avg_scaling(scaling_low, fw) for fw in common_fw]
    shi_vals = [avg_scaling(scaling_high, fw) for fw in common_fw]

    # Normalize to 0-1 (percentage of global max) for a balanced radar
    global_max = max(max(spsc_vals), max(slo_vals), max(shi_vals)) or 1
    spsc_norm = [v / global_max for v in spsc_vals] + [spsc_vals[0] / global_max]
    slo_norm = [v / global_max for v in slo_vals] + [slo_vals[0] / global_max]
    shi_norm = [v / global_max for v in shi_vals] + [shi_vals[0] / global_max]

    # ── Figure ─────────────────────────────────────────────────────────────
    fig, ax = plt.subplots(figsize=(9, 9), subplot_kw=dict(polar=True), facecolor=BG_COLOR)
    ax.set_facecolor(BG_COLOR)

    # Series colors & labels
    series = [
        (spsc_norm,  '#10b981', 'SPSC Queue (Single Producer → Single Consumer)'),
        (slo_norm,   '#38bdf8', 'Scaling Low  (1:1 · 4:1)'),
        (shi_norm,   '#f472b6', 'Scaling High (8:1 · 16:4 · 32:8)'),
    ]

    for vals, color, label in series:
        ax.plot(angles, vals, 'o-', linewidth=2.2, color=color, label=label, markersize=6)
        ax.fill(angles, vals, alpha=0.12, color=color)

    # Axis labels (framework names)
    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(common_fw, fontsize=12, fontweight='bold', color=TEXT_COLOR)

    # Radial grid
    ax.set_rlabel_position(30)
    ax.yaxis.set_major_formatter(mticker.FuncFormatter(
        lambda v, _: f'{v * global_max / 1e6:.0f}M' if v > 0 else ''))
    ax.tick_params(axis='y', labelsize=9, colors=SUBTLE_COLOR)

    # Grid styling
    ax.spines['polar'].set_color(GRID_COLOR)
    ax.grid(color=GRID_COLOR, linewidth=0.8, alpha=0.6)

    # Title
    fig.suptitle('zThread · Multi-Dimensional Performance Radar',
                 fontsize=17, fontweight='bold', color='#ffffff', y=0.97)
    ax.set_title('JMH Benchmark · JDK 25 · Linux',
                 fontsize=10, color=SUBTLE_COLOR, style='italic', pad=24)

    # Legend
    legend = ax.legend(loc='lower center', bbox_to_anchor=(0.5, -0.14),
                       ncol=3, fontsize=10, framealpha=0.0,
                       labelcolor=TEXT_COLOR)
    for text in legend.get_texts():
        text.set_color(TEXT_COLOR)

    plt.subplots_adjust(top=0.88, bottom=0.12)
    os.makedirs(os.path.dirname(output_path) or '.', exist_ok=True)
    plt.savefig(output_path, format='svg', facecolor=fig.get_facecolor(), edgecolor='none',
                bbox_inches='tight', pad_inches=0.3)
    plt.close()
    print(f"Radar chart generated at {output_path}")


def update_readme_table(spsc, readme_path):
    """Update the benchmark table in README.md with SPSC results only (6 frameworks)."""
    if not os.path.exists(readme_path):
        print(f"README file {readme_path} not found. Skipping table update.")
        return

    table_lines = [
        "| Framework / Mechanism | Throughput (Higher is better) | Average Latency (Lower is better) | Engine Architecture |",
        "| :--- | :--- | :--- | :--- |"
    ]

    # Build rows sorted by throughput descending
    rows = []
    for method, display in FULL_NAME_MAP.items():
        short = SPSC_NAME_MAP.get(method)
        if not short or short not in spsc:
            continue
        score = spsc[short]
        score_m = score / 1_000_000
        latency_ns = (1.0 / score * 1e9) if score > 0 else 0
        arch = ARCH_MAP.get(display, 'Unknown')
        rows.append((score_m, display, latency_ns, arch))

    rows.sort(reverse=True)

    for score_m, display, latency_ns, arch in rows:
        if 'zThread' in display:
            table_lines.append(
                f"| **{display}** | **~{score_m:.2f} M ops/sec** | **~{latency_ns:.1f} ns / event** | {arch} |")
        else:
            table_lines.append(
                f"| **{display}** | ~{score_m:.2f} M ops/sec | ~{latency_ns:.1f} ns / event | {arch} |")

    new_table = "\n".join(table_lines) + "\n"

    with open(readme_path, 'r') as f:
        content = f.read()

    start_marker = "<!-- BENCHMARK_TABLE_START -->"
    end_marker = "<!-- BENCHMARK_TABLE_END -->"
    si = content.find(start_marker)
    ei = content.find(end_marker)

    if si != -1 and ei != -1:
        new_content = content[:si + len(start_marker)] + "\n" + new_table + content[ei:]
        with open(readme_path, 'w') as f:
            f.write(new_content)
        print(f"Table updated in {readme_path}")
    else:
        print(f"Markers not found in {readme_path}. Skipping table update.")


def main():
    json_path = sys.argv[1] if len(sys.argv) > 1 else 'jmh-result.json'
    output_path = 'assets/benchmark_graph.svg'
    readme_path = 'README.md'

    if not os.path.exists(json_path):
        print(f"File {json_path} not found.")
        return

    with open(json_path, 'r') as f:
        data = json.load(f)

    spsc, scaling_low, scaling_high = parse_benchmark_data(data)

    if not spsc:
        print("No SPSC benchmark data found.")
        return

    draw_radar_chart(spsc, scaling_low, scaling_high, output_path)
    update_readme_table(spsc, readme_path)


if __name__ == "__main__":
    main()
