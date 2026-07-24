#!/bin/bash
set -e

if [ ! -f "jmh-result.json" ]; then
    echo "Running benchmarks to generate JSON output..."
    java -jar ../target/zthread-benchmark.jar -f 1 -wi 3 -i 5 -rf json -rff jmh-result.json
fi

echo "Generating charts from jmh-result.json..."

cat << 'EOF' > plot_results.py
import json
import matplotlib.pyplot as plt
import sys
import os

if not os.path.exists('jmh-result.json'):
    print("jmh-result.json not found")
    sys.exit(1)

with open('jmh-result.json', 'r') as f:
    data = json.load(f)

# Group by benchmark class (Scenario)
scenarios = {}
for entry in data:
    name = entry['benchmark'].split('.')[-1]
    scenario = entry['benchmark'].split('.')[-2]
    score = entry['primaryMetric']['score']
    
    if scenario not in scenarios:
        scenarios[scenario] = {}
    scenarios[scenario][name] = score

# Plot each scenario
for scenario, bench_data in scenarios.items():
    labels = list(bench_data.keys())
    values = list(bench_data.values())
    
    plt.figure(figsize=(10, 6))
    bars = plt.bar(labels, values, color='skyblue')
    plt.ylabel('Operations / Second')
    plt.title(f'{scenario.capitalize()} Benchmark Throughput')
    plt.xticks(rotation=45, ha="right")
    
    # Add values on top of bars
    for bar in bars:
        yval = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2, yval, f'{yval:,.0f}', ha='center', va='bottom')
        
    plt.tight_layout()
    plt.savefig(f'{scenario}_chart.png')
    plt.close()
    
print("Charts generated successfully!")
EOF

python3 plot_results.py
rm plot_results.py
