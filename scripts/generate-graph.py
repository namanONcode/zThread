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


def draw_bar_chart(spsc, output_path):
    """Generate a modern dark-theme horizontal bar chart for SPSC throughput."""
    # Sort frameworks by throughput ascending for horizontal bar chart
    sorted_items = sorted(spsc.items(), key=lambda x: x[1])
    frameworks = [item[0] for item in sorted_items]
    scores = [item[1] for item in sorted_items]

    fig, ax = plt.subplots(figsize=(10, 6), facecolor=BG_COLOR)
    ax.set_facecolor(BG_COLOR)

    # Plot bars
    y_pos = np.arange(len(frameworks))
    colors = [FRAMEWORK_COLORS.get(fw, '#38bdf8') for fw in frameworks]
    
    bars = ax.barh(y_pos, scores, color=colors, height=0.6, alpha=0.9)
    
    # Grid and styling
    ax.set_yticks(y_pos)
    ax.set_yticklabels(frameworks, fontsize=12, fontweight='bold', color=TEXT_COLOR)
    ax.xaxis.set_major_formatter(mticker.FuncFormatter(lambda v, _: f'{v / 1e6:.1f}M'))
    ax.tick_params(axis='x', colors=SUBTLE_COLOR, labelsize=11)
    ax.tick_params(axis='y', colors=TEXT_COLOR, length=0)
    
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_color(GRID_COLOR)
    
    ax.xaxis.grid(True, color=GRID_COLOR, linestyle='--', alpha=0.7)
    ax.set_axisbelow(True)

    # Value labels on bars
    for bar in bars:
        width = bar.get_width()
        label_x = width - (max(scores) * 0.05) if width > (max(scores) * 0.15) else width + (max(scores) * 0.02)
        align = 'right' if width > (max(scores) * 0.15) else 'left'
        color = '#ffffff' if align == 'right' else TEXT_COLOR
        
        ax.text(label_x, bar.get_y() + bar.get_height()/2, 
                f'{width / 1e6:.1f} M ops/sec',
                va='center', ha=align, color=color, fontweight='bold', fontsize=11)

    # Title
    fig.suptitle('Throughput (Operations / sec)', fontsize=16, fontweight='bold', color='#ffffff', y=0.96)
    ax.set_title('Single-Producer Single-Consumer (Higher is better) · JDK 25 · Linux', 
                 fontsize=11, color=SUBTLE_COLOR, style='italic', pad=15)

    plt.subplots_adjust(top=0.85, bottom=0.1)
    os.makedirs(os.path.dirname(output_path) or '.', exist_ok=True)
    plt.savefig(output_path, format='svg', facecolor=fig.get_facecolor(), edgecolor='none',
                bbox_inches='tight', pad_inches=0.3)
    plt.close()
    print(f"Bar chart generated at {output_path}")


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

    draw_bar_chart(spsc, output_path)
    update_readme_table(spsc, readme_path)


if __name__ == "__main__":
    main()
