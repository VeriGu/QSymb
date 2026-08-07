
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def generate_s_curves():
    """
    This script reads the all_comparison_data.csv file, calculates the reduction rates,
    and generates S-curve plots for 5 different methods, sorted by the reduction
    rate of beam_symb_2q.
    """
    try:
        # Load the dataset
        df = pd.read_csv('all_comparison_data.csv')

        # Define the columns to be processed
        methods = ['beam_symb_2q', 'guoq_rewrite_2q', 'qiskit_2q', 'quartz_2q', 'tket_2q', 'queso_2q']
        all_cols = ['original_2q'] + methods

        # Convert columns to numeric, coercing errors to NaN
        for col in all_cols:
            df[col] = pd.to_numeric(df[col], errors='coerce')

        # Drop rows where original_2q is NaN or zero
        df.dropna(subset=['original_2q'], inplace=True)
        df = df[df['original_2q'] > 0]

        # Calculate reduction rates
        for method in methods:
            df[f'{method}_reduction'] = (df['original_2q'] - df[method]) / df['original_2q']

        # Sort the dataframe by 'beam_symb_2q_reduction'
        df_sorted = df.sort_values(by='beam_symb_2q_reduction').reset_index(drop=True)

        # The 5 methods to plot
        plot_methods = ['guoq_rewrite_2q', 'qiskit_2q', 'quartz_2q', 'tket_2q', 'queso_2q']

        # Create a figure with 5 subplots in a row
        fig, axes = plt.subplots(1, 5, figsize=(25, 5), sharey=True)
        fig.suptitle('S-Curve Comparison of Reduction Rates', fontsize=20, fontweight='bold')

        for i, method in enumerate(plot_methods):
            ax = axes[i]
            reduction_col = f'{method}_reduction'
            
            # Drop NaNs for the current method for plotting
            plot_data = df_sorted.dropna(subset=[reduction_col])
            
            ax.scatter(np.arange(len(plot_data)), plot_data[reduction_col], s=10)
            
            ax.set_title(method.replace('_', ' ').title(), fontsize=16, fontweight='bold')
            ax.set_xlabel('Benchmarks (Sorted by Our Method)', fontsize=12, fontweight='bold')
            ax.grid(True, linestyle='--', alpha=0.6)
            ax.set_xticks([])

        # Set common Y-axis label
        axes[0].set_ylabel('Reduction Rate', fontsize=14, fontweight='bold')

        plt.tight_layout(rect=[0, 0.03, 1, 0.95])
        plt.savefig('s_curve_comparison.png', dpi=300)
        plt.close()

        print("S-curve comparison plot saved as s_curve_comparison.png")

    except FileNotFoundError:
        print("Error: 'all_comparison_data.csv' not found. Please ensure the file is in the correct directory.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == '__main__':
    generate_s_curves()
