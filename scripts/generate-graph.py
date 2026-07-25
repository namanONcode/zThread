#!/usr/bin/env python3
import json
import os
import sys
import matplotlib.pyplot as plt
import numpy as np

def main():
    if len(sys.argv) > 1:
        json_path = sys.argv[1]
    else:
        json_path = 'jmh-result.json'
        
    output_path = 'assets/benchmark_graph.svg'
    readme_path = 'README.md'

    if not os.path.exists(json_path):
        print(f"File {json_path} not found. Cannot generate graph.")
        return

    with open(json_path, 'r') as f:
        data = json.load(f)

    benchmarks = []
    scores = []
    errors = []

    # Map method names to clear, descriptive names
    name_map = {
        'benchZThread': 'zThread (Linux FFM / Epoll)',
        'benchNetty': 'Netty (NIO EventLoop)',
        'benchVertx': 'Vert.x (Event Loop)',
        'benchReactor': 'Project Reactor (Schedulers)',
        'benchVirtualThreads': 'Java Virtual Threads (Loom)',
        'benchArrayBlockingQueue': 'ArrayBlockingQueue',
        'benchLinkedBlockingQueue': 'LinkedBlockingQueue',
        'benchConcurrentLinkedQueue': 'ConcurrentLinkedQueue',
        'benchSynchronousQueue': 'SynchronousQueue'
    }

    for result in data:
        full_name = result.get('benchmark', '')
        if 'SpscEventBenchmark' not in full_name:
            continue
        method_name = full_name.split('.')[-1]
        
        display_name = name_map.get(method_name, method_name)
        score = float(result['primaryMetric']['score'])
        
        raw_error = result['primaryMetric'].get('scoreError', 0)
        try:
            error = float(raw_error)
            if np.isnan(error):
                error = 0
        except:
            error = 0
            
        benchmarks.append(display_name)
        scores.append(score)
        errors.append(error)

    if not benchmarks:
        print("No benchmarks found.")
        return

    # Sort by score ascending (lowest to highest) so highest score is at top of barh chart
    sorted_data = sorted(zip(scores, benchmarks, errors))
    scores, benchmarks, errors = zip(*sorted_data)

    # Convert to Millions (ops/sec)
    scores_m = [s / 1_000_000 for s in scores]
    errors_m = [e / 1_000_000 for e in errors]

    # Calculate average latency in nanoseconds (1 / ops_sec * 1e9)
    latencies_ns = [(1.0 / s) * 1e9 if s > 0 else 0 for s in scores]

    # Set up dark theme figure with explicit background color (not transparent)
    fig, ax = plt.subplots(figsize=(11, 6.5), facecolor='#0d1117')
    ax.set_facecolor('#0d1117')

    # Color palette
    colors = []
    for b in benchmarks:
        if 'zThread' in b:
            colors.append('#10b981')  # Vibrant Emerald Green for zThread
        elif 'Virtual' in b:
            colors.append('#ef4444')  # Red accent for low-throughput baseline
        else:
            colors.append('#38bdf8')  # Bright Sky Blue for other frameworks

    bars = ax.barh(benchmarks, scores_m, xerr=errors_m, color=colors, capsize=4, height=0.55,
                   error_kw={'ecolor': '#94a3b8', 'linewidth': 1.2})

    # Styling labels and title
    ax.set_xlabel('Throughput (Million Operations / Sec — Higher is Better)', 
                  fontsize=12, fontweight='bold', color='#e2e8f0', labelpad=12)
    
    fig.suptitle('Event Loop & Micro-Task Throughput Benchmark', 
                 fontsize=16, fontweight='bold', color='#ffffff', y=0.96)
    ax.set_title('Evaluated using JMH • SPSC Event Passing • JDK 25 Linux', 
                 fontsize=10, color='#94a3b8', style='italic', pad=12)

    # Gridlines and spines
    ax.grid(axis='x', linestyle=':', color='#334155', alpha=0.7)
    ax.set_axisbelow(True)
    
    for spine in ax.spines.values():
        spine.set_color('#334155')
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)

    # Tick parameters
    ax.tick_params(axis='x', colors='#cbd5e1', labelsize=10)
    ax.tick_params(axis='y', colors='#f8fafc', labelsize=11)
    
    # Max score for padding
    max_score = max(scores_m)
    ax.set_xlim(0, max_score * 1.35)

    # Add numeric annotation: "13.21 M ops/s (~75.7 ns)"
    for idx, (bar, score_m, latency_ns) in enumerate(zip(bars, scores_m, latencies_ns)):
        width = bar.get_width()
        bench_name = benchmarks[idx]
        
        text_color = '#34d399' if 'zThread' in bench_name else '#cbd5e1'
        font_weight = 'bold' if 'zThread' in bench_name else 'normal'
        
        annotation = f'{score_m:.2f} M ops/s ({latency_ns:.1f} ns/op)'
        ax.text(width + 0.3, bar.get_y() + bar.get_height()/2, annotation, 
                ha='left', va='center', color=text_color, fontweight=font_weight, fontsize=10)

    plt.subplots_adjust(left=0.32, right=0.96, top=0.84, bottom=0.12)
    plt.savefig(output_path, format='svg', facecolor=fig.get_facecolor(), edgecolor='none')
    print(f"Graph successfully generated at {output_path}")

    # --- Update README.md Table ---
    if not os.path.exists(readme_path):
        print(f"README file {readme_path} not found. Skipping table update.")
        return

    # Generate new table content
    table_lines = [
        "| Framework / Mechanism | Throughput (Higher is better) | Average Latency (Lower is better) | Engine Architecture |",
        "| :--- | :--- | :--- | :--- |"
    ]
    
    # Engine architecture mapping
    arch_map = {
        'zThread (Linux FFM / Epoll)': 'Kernel `epoll` + Lock-free RingBuffer via Panama FFM',
        'Project Reactor (Schedulers)': 'RingBuffer-backed Schedulers',
        'SynchronousQueue': 'Dual stack / queue thread handoff',
        'ArrayBlockingQueue': 'ReentrantLock + Condition queues',
        'LinkedBlockingQueue': 'Two-lock queue algorithm',
        'Vert.x (Event Loop)': 'Netty-backed event loop dispatch',
        'Netty (NIO EventLoop)': '`Selector` + ConcurrentLinkedQueue dispatch',
        'Java Virtual Threads (Loom)': 'Carrier thread park/unpark overhead',
        'ConcurrentLinkedQueue': 'Lock-free queue algorithm'
    }

    # Sort descending for table (highest score first)
    sorted_table_data = sorted(zip(scores_m, benchmarks, latencies_ns), reverse=True)
    
    for score_m, bench_name, latency_ns in sorted_table_data:
        arch = arch_map.get(bench_name, "Unknown architecture")
        
        # Bold zThread for emphasis
        if "zThread" in bench_name:
            bench_display = f"**{bench_name}**"
            score_display = f"**~{score_m:.2f} M ops/sec**"
            lat_display = f"**~{latency_ns:.1f} ns / event**"
        else:
            bench_display = f"**{bench_name}**"
            score_display = f"~{score_m:.2f} M ops/sec"
            lat_display = f"~{latency_ns:.1f} ns / event"
            
        table_lines.append(f"| {bench_display} | {score_display} | {lat_display} | {arch} |")

    new_table_str = "\n".join(table_lines) + "\n"
    
    # Read README, find markers, and replace
    with open(readme_path, 'r') as f:
        content = f.read()
        
    start_marker = "<!-- BENCHMARK_TABLE_START -->"
    end_marker = "<!-- BENCHMARK_TABLE_END -->"
    
    start_idx = content.find(start_marker)
    end_idx = content.find(end_marker)
    
    if start_idx != -1 and end_idx != -1:
        new_content = content[:start_idx + len(start_marker)] + "\n" + new_table_str + content[end_idx:]
        with open(readme_path, 'w') as f:
            f.write(new_content)
        print(f"Table successfully updated in {readme_path}")
    else:
        print(f"Markers not found in {readme_path}. Could not update table.")

if __name__ == "__main__":
    main()
