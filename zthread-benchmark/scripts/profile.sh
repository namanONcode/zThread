#!/bin/bash
set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: ./profile.sh <benchmark_regex>"
    exit 1
fi

BENCH=$1

echo "Profiling $BENCH using async-profiler via JMH..."

java -jar ../target/zthread-benchmark.jar -f 1 -wi 2 -i 3 "$BENCH" \
    -prof async:libPath=/usr/lib/async-profiler/libasyncProfiler.so;output=flamegraph;dir=profiling-results \
    -prof gc \
    --enable-native-access=ALL-UNNAMED -XX:+UseZGC

echo "Profiling results saved to profiling-results/"
