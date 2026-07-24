#!/bin/bash
set -e

echo "Running Linux-specific benchmarks (Idle, Socket, File, Signal)..."

# Ensure FFM access
export JVM_ARGS="--enable-native-access=ALL-UNNAMED -XX:+UseZGC"

java $JVM_ARGS -jar ../target/zthread-benchmark.jar -f 1 -wi 2 -i 3 ".*Idle.*|.*Tcp.*|.*File.*|.*Signal.*"

echo "Linux benchmarks completed."
