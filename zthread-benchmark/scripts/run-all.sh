#!/bin/bash
set -e

echo "Building zThread Benchmark Suite..."
cd ..
./mvnw clean install -DskipTests

echo "Running all basic throughput benchmarks..."
java -jar zthread-benchmark/target/zthread-benchmark.jar -f 1 -wi 3 -i 5

echo "All basic benchmarks completed."
