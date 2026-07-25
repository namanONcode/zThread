#!/usr/bin/env python3
import json
import os
import sys

# Frameworks removed from benchmarks — filter them out of charts
EXCLUDED_FRAMEWORKS = {'ARRAY_BLOCKING_QUEUE', 'LINKED_BLOCKING_QUEUE', 'CONCURRENT_LINKED_QUEUE'}

def main():
    if len(sys.argv) < 2:
        print("Usage: generate_charts.py <path_to_jmh_json>")
        sys.exit(1)

    json_path = sys.argv[1]
    
    if not os.path.exists(json_path):
        print(f"Error: {json_path} not found.")
        sys.exit(1)
        
    try:
        import matplotlib.pyplot as plt
        import pandas as pd
        import seaborn as sns
    except ImportError:
        print("Missing required libraries. Run: pip install matplotlib pandas seaborn")
        sys.exit(1)
        
    with open(json_path, 'r') as f:
        data = json.load(f)

    # Simplified parsing for the S1_S5_ThroughputLatencyBenchmark
    records = []
    for bench in data:
        name = bench.get("benchmark", "")
        if "S1_S5_ThroughputLatencyBenchmark" in name:
            params = bench.get("params", {})
            framework = params.get("framework", "UNKNOWN")
            
            # Skip removed frameworks
            if framework in EXCLUDED_FRAMEWORKS:
                continue
                
            concurrency = params.get("concurrency", "1:1")
            payload = params.get("payloadSize", "64")
            
            score = bench.get("primaryMetric", {}).get("score", 0.0)
            score_err = bench.get("primaryMetric", {}).get("scoreError", 0.0)
            
            records.append({
                "Framework": framework,
                "Concurrency": concurrency,
                "PayloadSize": payload,
                "Throughput (ops/s)": score,
                "Error": score_err
            })
            
    if not records:
        print("No valid benchmark data found for charting.")
        sys.exit(0)
        
    df = pd.DataFrame(records)
    
    # Create charts directory
    out_dir = os.path.join(os.path.dirname(json_path), "../charts")
    os.makedirs(out_dir, exist_ok=True)
    
    # Dark theme styling
    plt.style.use('dark_background')
    
    # Filter for 64B payload
    df_64 = df[df['PayloadSize'] == '64']
    
    # Plot Scaling (Throughput vs Concurrency)
    fig, ax = plt.subplots(figsize=(12, 7))
    fig.patch.set_facecolor('#0d1117')
    ax.set_facecolor('#0d1117')
    
    palette = sns.color_palette([
        '#10b981', '#38bdf8', '#a78bfa', '#fb923c', 
        '#f87171', '#facc15', '#34d399', '#60a5fa'
    ])
    
    sns.barplot(data=df_64, x='Concurrency', y='Throughput (ops/s)', 
                hue='Framework', palette=palette, ax=ax)
    
    ax.set_title('Throughput Scaling · 64B Payload', fontsize=15, 
                 fontweight='bold', color='#ffffff', pad=16)
    ax.set_xlabel('Concurrency (Producers : Consumers)', fontsize=12, 
                  color='#e2e8f0', labelpad=10)
    ax.set_ylabel('Throughput (ops/s)', fontsize=12, color='#e2e8f0', labelpad=10)
    ax.tick_params(colors='#cbd5e1')
    ax.grid(axis='y', linestyle=':', color='#1e293b', alpha=0.6)
    ax.legend(title='Framework', fontsize=9, title_fontsize=10,
              loc='upper right', framealpha=0.3, labelcolor='#e2e8f0')
    
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'scaling_throughput.png'), 
                dpi=150, facecolor=fig.get_facecolor())
    plt.close()
    
    print(f"Charts generated in {out_dir}")

    # Generate Markdown Report
    report_path = os.path.join(out_dir, '../benchmark.md')
    with open(report_path, 'w') as f:
        f.write("# zThread Benchmark Results\n\n")
        f.write("## Scaling Throughput (64B)\n")
        f.write("![Scaling](charts/scaling_throughput.png)\n\n")
        f.write("## Raw Data\n")
        f.write(df.to_markdown(index=False))
        f.write("\n")
        
    print(f"Report generated at {report_path}")

if __name__ == "__main__":
    main()
