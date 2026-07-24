#!/usr/bin/env python3
import json
import os
import matplotlib.pyplot as plt
import numpy as np

def main():
    json_path = 'jmh-result.json'
    output_path = 'assets/benchmark_graph.svg'

    if not os.path.exists(json_path):
        print(f"File {json_path} not found. Cannot generate graph.")
        return

    with open(json_path, 'r') as f:
        data = json.load(f)

    benchmarks = []
    scores = []
    errors = []

    # Map method names to pretty names
    name_map = {
        'benchZThread': 'zThread (FFM)',
        'benchNetty': 'Netty (NIO)',
        'benchVertx': 'Vert.x',
        'benchArrayBlockingQueue': 'ArrayBlockingQueue',
        'benchLinkedBlockingQueue': 'LinkedBlockingQueue',
        'benchConcurrentLinkedQueue': 'ConcurrentLinkedQueue',
        'benchSynchronousQueue': 'SynchronousQueue'
    }

    for result in data:
        full_name = result['benchmark']
        method_name = full_name.split('.')[-1]
        
        display_name = name_map.get(method_name, method_name)
        score = float(result['primaryMetric']['score'])
        
        # Handle NaN error
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

    # Sort by score
    sorted_data = sorted(zip(scores, benchmarks, errors))
    scores, benchmarks, errors = zip(*sorted_data)

    # Convert to millions for readability
    scores_m = [s / 1_000_000 for s in scores]
    errors_m = [e / 1_000_000 for e in errors]

    # Create figure
    plt.style.use('dark_background')
    fig, ax = plt.subplots(figsize=(10, 6))

    colors = ['#1f77b4' if 'zThread' not in b else '#2ca02c' for b in benchmarks]

    bars = ax.barh(benchmarks, scores_m, xerr=errors_m, color=colors, capsize=5, height=0.6)

    # Add labels
    ax.set_xlabel('Throughput (Million ops/sec)', fontsize=12, fontweight='bold')
    ax.set_title('SPSC Event Loop Performance', fontsize=16, fontweight='bold', pad=20)
    ax.grid(axis='x', linestyle='--', alpha=0.3)

    # Remove top and right spines
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    
    # Add value labels on bars
    for bar in bars:
        width = bar.get_width()
        ax.text(width + 0.5, bar.get_y() + bar.get_height()/2, f'{width:.2f} M', 
                ha='left', va='center', fontweight='bold')

    plt.tight_layout()
    plt.savefig(output_path, format='svg', bbox_inches='tight', transparent=True)
    print(f"Graph generated at {output_path}")

if __name__ == "__main__":
    main()
