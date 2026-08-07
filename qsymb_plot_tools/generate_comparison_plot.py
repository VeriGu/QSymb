import re
import matplotlib.pyplot as plt
import numpy as np

def parse_log_file(file_path):
    """Parses a log file to extract time and 2q gate count."""
    times = []
    two_q_counts = []
    
    with open(file_path, 'r') as f:
        content = f.read()
        
    # Find the original 2q count
    original_2q_match = re.search(r"Original 2q:(\d+)", content)
    if not original_2q_match:
        raise ValueError(f"Could not find 'Original 2q' in {file_path}")
    original_2q = int(original_2q_match.group(1))
    
    # Add the initial point at time 0
    times.append(0.0)
    two_q_counts.append(original_2q)

    # Regex to find Time and Best Optimized 2q
    matches = re.findall(r"Time: ([\d.E-]+) minutes\nBest Optimized Size:\d+\nBest Optimized 2q:(\d+)", content)
    for time_str, two_q_str in matches:
        times.append(float(time_str))
        two_q_counts.append(int(two_q_str))
        
    return times, two_q_counts

def get_data_for_plot(times, two_q_counts):
    """Processes the raw data to get one data point per minute for the plot."""
    # Create a list of (time, 2q_count) tuples and sort by time
    data_points = sorted(zip(times, two_q_counts))

    # Time intervals for the plot (0 to 30 minutes, step 3)
    plot_times = list(range(0, 61, 5))
    plot_reduction_rate = []

    original_2q = data_points[0][1] if data_points else 0
    if original_2q == 0:
        return plot_times, [0] * len(plot_times)

    # Find the best 2q count for each time interval
    current_best_2q = original_2q
    data_idx = 0
    for t in plot_times:
        # Iterate through data points to find the best 2q up to time t
        while data_idx < len(data_points) and data_points[data_idx][0] <= t:
            current_best_2q = min(current_best_2q, data_points[data_idx][1])
            data_idx += 1
        
        reduction_rate = (original_2q - current_best_2q) / original_2q * 100
        plot_reduction_rate.append(reduction_rate)

    return plot_times, plot_reduction_rate


def main():
    """Main function to generate the plot."""
    #4gt4-v0_73.qasm_rule_copy.txt_SA_rules_ibmnew_q3_3_symb_nm.txt_usesymbnm_[10, 30].log
    #4gt4-v0_73.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log
    #4gt4-v0_73.qasm_anchor_[10,30].log
    #4gt4-v0_73.qasm_canonical.log
    anchor_log = 'alu-v2_30.qasm_rule_copy.txt_SA_anchored.txt_usesymbnm_[10, 30].log'
    canonical_log = 'alu-v2_30.qasm_rule_copy.txt_SA_rules_ibmnew_q3_3_symb_nm.txt_usesymbnm_[10, 30].log'

    try:
        anchor_times, anchor_2q = parse_log_file(anchor_log)
        canonical_times, canonical_2q = parse_log_file(canonical_log)
    except ValueError as e:
        print(f"Error: {e}")
        return

    plot_times_anchor, reduction_anchor = get_data_for_plot(anchor_times, anchor_2q)
    plot_times_canonical, reduction_canonical = get_data_for_plot(canonical_times, canonical_2q)

    plt.figure(figsize=(12, 10))
    plt.plot(plot_times_anchor, reduction_anchor, marker='o', linestyle='-', label='anchor', linewidth=3)
    plt.plot(plot_times_canonical, reduction_canonical, marker='x', linestyle='--', label='canonical', linewidth=3)

    plt.xlabel('Time (minutes)', fontsize=28, fontweight='bold')
    plt.ylabel('2q Reduction Rate (%)', fontsize=28, fontweight='bold')
    plt.xticks(np.arange(0, 61, 5), fontsize=24)
    plt.yticks(fontsize=24)
    plt.grid(True)
    plt.legend(fontsize=25)
    
    output_file = '/Users/weiqiang/Downloads/quality_vs_time_comparison.pdf'
    plt.savefig(output_file)
    print(f"Plot saved to {output_file}")

if __name__ == '__main__':
    main()
