import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.font_manager import FontProperties


def generate_fidelity_s_curves():
    df = pd.read_csv('/Users/weiqiang/Downloads/benchmark_fidelity.csv')

    cols = ['qsymb_fidelity', 'quartz_fidelity', 'queso_fidelity', 'tket_fidelity', 'guoq_fidelity', 'qiskit_fidelity']
    for col in cols:
        df[col] = pd.to_numeric(df[col], errors='coerce')

    df.dropna(subset=['qsymb_fidelity'], inplace=True)
    df_sorted = df.sort_values(by='qsymb_fidelity').reset_index(drop=True)

    plot_methods = [
        ('qiskit_fidelity',  'Qiskit'),
        ('guoq_fidelity',    'Guoq-Rewrite'),
        ('quartz_fidelity',  'Quartz'),
        ('tket_fidelity',    'Tket'),
        ('queso_fidelity',   'Queso'),
    ]

    fig, axes = plt.subplots(1, 5, figsize=(25, 5))
    plt.subplots_adjust(left=0.05, right=0.98, top=1, bottom=0.25, wspace=0.1)

    font_props = FontProperties(weight='bold', size=20)

    for i, (method_col, method_label) in enumerate(plot_methods):
        ax = axes[i]

        comparison_data = df_sorted.dropna(subset=['qsymb_fidelity', method_col])

        ax.scatter(np.arange(len(comparison_data)), comparison_data['qsymb_fidelity'],
                   s=15, label='Ours')
        ax.scatter(np.arange(len(comparison_data)), comparison_data[method_col],
                   s=15, color='orange', label=method_label)

        ax.grid(True, linestyle='--', alpha=0.6)
        ax.set_xticks([])
        ax.tick_params(axis='y', labelsize=16)
        ax.legend(prop=font_props)
        ax.set_aspect('auto')
        ax.set_ylim(0, 1)

        if i == 0:
            ax.set_yticks(np.arange(0.2, 1.0, 0.2))
            ax.set_ylabel('Fidelity', fontsize=22, fontweight='bold')
            for label in ax.get_yticklabels():
                label.set_fontweight('bold')
                label.set_fontsize(19)
        else:
            ax.set_yticklabels([])

        # Comparison stats (higher fidelity = better)
        ours_better  = (comparison_data['qsymb_fidelity'] > comparison_data[method_col]).sum()
        theirs_better = (comparison_data[method_col] > comparison_data['qsymb_fidelity']).sum()
        equal = len(comparison_data) - ours_better - theirs_better
        total = len(comparison_data)

        ax_pos = ax.get_position()
        bar_height = 0.06
        bar_y_start = ax_pos.y0 - 0.1
        bar_width = ax_pos.width * 0.88
        bar_x_start = ax_pos.x0 + (ax_pos.width * 0.1)

        # Ours better bar
        ax_ours = fig.add_axes([bar_x_start, bar_y_start, bar_width, bar_height])
        ax_ours.set_frame_on(False)
        ax_ours.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
        ax_ours.add_patch(patches.Rectangle((0, 0), ours_better / total, 1, facecolor='skyblue', edgecolor='none'))
        ax_ours.set_xticks([])
        ax_ours.set_yticks([])
        ax_ours.text(0.5, 0.5, f'{ours_better}/{total}', ha='center', va='center', fontsize=18, color='black', fontweight='bold')

        # Theirs better bar
        ax_theirs = fig.add_axes([bar_x_start, bar_y_start - bar_height, bar_width, bar_height])
        ax_theirs.set_frame_on(False)
        ax_theirs.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
        ax_theirs.add_patch(patches.Rectangle((0, 0), theirs_better / total, 1, facecolor='orange', edgecolor='none'))
        ax_theirs.set_xticks([])
        ax_theirs.set_yticks([])
        ax_theirs.text(0.5, 0.5, f'{theirs_better}/{total}', ha='center', va='center', fontsize=18, color='black', fontweight='bold')

        # Equal bar
        ax_equal = fig.add_axes([bar_x_start, bar_y_start - 2 * bar_height, bar_width, bar_height])
        ax_equal.set_frame_on(False)
        ax_equal.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
        ax_equal.add_patch(patches.Rectangle((0, 0), equal / total, 1, facecolor='green', edgecolor='none'))
        ax_equal.set_xticks([])
        ax_equal.set_yticks([])
        ax_equal.text(0.5, 0.5, f'{equal}/{total}', ha='center', va='center', fontsize=18, color='black', fontweight='bold')

        if i == 0:
            legend_x = bar_x_start - 0.01
            fig.text(legend_x, bar_y_start + bar_height / 2,       'Ours Better', ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - bar_height / 2,       'Ours Worse',  ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - 1.5 * bar_height,     'Equal',       ha='right', va='center', fontsize=18, fontweight='bold')

    plt.savefig('/Users/weiqiang/Downloads/fidelity_s_curve_comparison.pdf', dpi=800, bbox_inches='tight', pad_inches=0)
    plt.close()
    print("Saved fidelity_s_curve_comparison.pdf")


if __name__ == '__main__':
    generate_fidelity_s_curves()
