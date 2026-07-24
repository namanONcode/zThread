#!/usr/bin/env python3
import json
import os
import sys

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
    
    # Filter out by default payload for a clean scaling chart
    df_64 = df[df['PayloadSize'] == '64']
    
    # Plot Scaling (Throughput vs Concurrency)
    plt.figure(figsize=(10, 6))
    sns.barplot(data=df_64, x='Concurrency', y='Throughput (ops/s)', hue='Framework')
    plt.title('Throughput Scaling (64B Payload)')
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, 'scaling_throughput.png'))
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
