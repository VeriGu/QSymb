
import re
import matplotlib.pyplot as plt
import numpy as np

def parse_log_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    original_2q_match = re.search(r"Original 2q:(\d+)", content)
    original_2q = int(original_2q_match.group(1)) if original_2q_match else 0

    times = [0.0]
    best_2qs = [original_2q]

    for match in re.finditer(r"Time: ([\d.eE-]+) minutes\nBest Optimized Size:\d+\nBest Optimized 2q:(\d+)", content):
        times.append(float(match.group(1)))
        best_2qs.append(int(match.group(2)))
    
    # Also capture the final best values if they exist after the last time report
    final_best_2q_matches = re.findall(r"Best Optimized 2q:(\d+)", content)
    if final_best_2q_matches:
        final_best_2q = int(final_best_2q_matches[-1])
        if final_best_2q != best_2qs[-1]:
             if times:
                times.append(times[-1]) # a small increment or same as last? let's use same
                best_2qs.append(final_best_2q)


    return original_2q, np.array(times), np.array(best_2qs)

def main():
    log_files = [
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log",
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 40].log",
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 50].log",
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[20, 40].log",
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[30, 50].log",
        "alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, inf].log",
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_usesymbnm_[0, 20].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 40].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 50].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[20, 40].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[30, 50].log',
        # '/Users/weiqiang/Downloads/4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 500].log'
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log",
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 40].log",
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 50].log",
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[20, 40].log",
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[30, 50].log",
        # "alu-v0_26.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 500].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 40].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 50].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, inf].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[20, 40].log",
        # "sym6_145.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[30, 50].log",
    ]

    plt.figure(figsize=(12, 10))
    
    target_times = np.arange(0, 61, 5)

    for log_file in log_files:
        label_match = re.search(r"_\[(\d+,\s*(\d+|inf))\]", log_file)
        label = label_match.group(1) if label_match else 'Unknown'

        original_2q, times, best_2qs = parse_log_file(log_file)
        
        if original_2q == 0:
            print(f"Could not find Original 2q in {log_file}, or it is 0. Skipping.")
            continue

        reduction = original_2q - best_2qs
        reduction_rate = reduction / original_2q
        
        # Interpolate reduction rate at target times
        interpolated_reduction_rate = np.interp(target_times, times, reduction_rate, right=reduction_rate[-1])

        plt.plot(target_times, interpolated_reduction_rate, marker='o', linestyle='-', label=f'window: [{label}]', linewidth=2)

    #plt.title('2q Reduction Rate vs. Time for different SA rule sizes', fontsize=16)
    plt.xlabel('Time (minutes)', fontsize=28, fontweight='bold')
    plt.ylabel('2q Reduction Rate (%)', fontsize=28, fontweight='bold')
    plt.legend(fontsize=25)
    plt.grid(True)
    plt.xticks(target_times, fontsize=24)
    plt.yticks(fontsize=24)
    
    
    output_path = '/Users/weiqiang/Downloads/sa_rule_size_comparison_rate.pdf'
    plt.savefig(output_path)
    print(f"Plot saved to {output_path}")
    plt.close()

if __name__ == '__main__':
    main()
