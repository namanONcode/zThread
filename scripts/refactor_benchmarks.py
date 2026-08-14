import os
import shutil
import xml.etree.ElementTree as ET

BASE = "zthread-benchmark/src/main/java/io/github/namanoncode/zthread/benchmark"

# Map old paths to new paths (relative to BASE)
# We also want to keep the adapters. Let's move them to 'adapters' package.
MOVES = {
    "queue/SpscEventBenchmark.java": "throughput/SpscEventBenchmark.java",
    "queue/MpscEventBenchmark.java": "throughput/MpscEventBenchmark.java",
    "queue/MpmcEventBenchmark.java": "throughput/MpmcEventBenchmark.java",
    "scenarios/S1_S5_ThroughputLatencyBenchmark.java": "scaling/ThroughputLatencyBenchmark.java",
    "scenarios/S6_IdleRuntimeBenchmark.java": "idle/ScenariosIdleBenchmark.java",
    "core/IdleRuntimeBenchmark.java": "idle/IdleRuntimeBenchmark.java",
    "timer/TimerExecutionBenchmark.java": "timers/TimerExecutionBenchmark.java",
    "file/FileWatchBenchmark.java": "inotify/FileWatchBenchmark.java",
    "socket/LoopbackTcpBenchmark.java": "socket/LoopbackTcpBenchmark.java",
    "signal/SignalBenchmark.java": "eventfd/SignalBenchmark.java", # Moved here as closest fit
}

ADAPTERS = [
    "framework/BenchmarkEvent.java",
    "framework/EventHandler.java",
    "framework/adapter/EventRuntimeAdapter.java",
    "framework/adapter/ZThreadAdapter.java",
    "framework/adapter/BlockingQueueAdapter.java",
    "framework/adapter/ExecutorAdapter.java",
    "framework/adapter/VirtualThreadAdapter.java",
    "framework/adapter/ReactorAdapter.java",
    "framework/adapter/NettyAdapter.java",
    "framework/adapter/VertxAdapter.java"
]

for adapter in ADAPTERS:
    MOVES[adapter] = adapter.replace("framework/adapter/", "adapters/").replace("framework/", "adapters/")

def refactor_java_file(old_path, new_path):
    with open(old_path, 'r') as f:
        content = f.read()
    
    # Calculate old and new package strings
    old_pkg = "io.github.namanoncode.zthread.benchmark." + os.path.dirname(os.path.relpath(old_path, BASE)).replace('/', '.')
    new_pkg = "io.github.namanoncode.zthread.benchmark." + os.path.dirname(os.path.relpath(new_path, BASE)).replace('/', '.')
    
    # Replace package declaration
    content = content.replace(f"package {old_pkg};", f"package {new_pkg};")
    
    # Update imports for other moved files (especially adapters)
    for old, new in MOVES.items():
        old_class = "io.github.namanoncode.zthread.benchmark." + old.replace('/', '.')[:-5]
        new_class = "io.github.namanoncode.zthread.benchmark." + new.replace('/', '.')[:-5]
        content = content.replace(old_class, new_class)
    
    with open(new_path, 'w') as f:
        f.write(content)

def main():
    # 1. Create new directories
    new_dirs = ["throughput", "latency", "memory", "gc", "scaling", "timers", "eventfd", "inotify", "socket", "http", "websocket", "idle", "lifecycle", "adapters"]
    for d in new_dirs:
        os.makedirs(os.path.join(BASE, d), exist_ok=True)

    # 2. Move and refactor files
    for old, new in MOVES.items():
        old_full = os.path.join(BASE, old)
        new_full = os.path.join(BASE, new)
        if os.path.exists(old_full):
            refactor_java_file(old_full, new_full)
            os.remove(old_full)
        else:
            print(f"Warning: {old_full} not found.")

    # Clean up old directories
    for d in ["core", "queue", "timer", "file", "signal", "scenarios", "socket", "framework/adapter", "framework"]:
        dir_path = os.path.join(BASE, d)
        if os.path.exists(dir_path) and not os.listdir(dir_path):
            os.rmdir(dir_path)

if __name__ == "__main__":
    main()
