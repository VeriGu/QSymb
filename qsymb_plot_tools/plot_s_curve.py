
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as patches

def plot_improvement_scatter():
    """
    This script reads a CSV file, calculates the improvement ratios,
    and plots a scatter plot to compare the results.
    """
    try:
        # Load the dataset
        df_results = pd.read_csv('guoq_results1.csv')

        # Convert columns to numeric, coercing errors to NaN
        for col in ['original_2q', 'best_size_2q', 'BEAM Symb 2q']:
             df_results[col] = pd.to_numeric(df_results[col], errors='coerce')

        # Drop rows with NaN values in essential columns
        df_results.dropna(subset=['original_2q', 'best_size_2q', 'BEAM Symb 2q'], inplace=True)

        # Ensure original_2q is not zero to avoid division by zero errors
        df_results = df_results[df_results['original_2q'] > 0]

        # Calculate the improvement ratios
        df_results['beam_symb_improvement'] = (df_results['original_2q'] - df_results['BEAM Symb 2q']) / df_results['original_2q']
        df_results['best_size_improvement'] = (df_results['original_2q'] - df_results['best_size_2q']) / df_results['original_2q']

        # Sort the dataframe by 'beam_symb_improvement'
        df_results = df_results.sort_values('beam_symb_improvement').reset_index(drop=True)

        # Count how many are better
        ours_better = (df_results['beam_symb_improvement'] > df_results['best_size_improvement']).sum()
        guoq_better = (df_results['best_size_improvement'] > df_results['beam_symb_improvement']).sum()
        total = len(df_results)
        equal = total - (ours_better + guoq_better)
        ours_percentage = ours_better / total
        guoq_percentage = guoq_better / total
        equal_percentage = 1 - (ours_percentage + guoq_percentage)

        # Plotting the scatter plot
        fig, ax = plt.subplots(figsize=(10, 8))
        index = np.arange(len(df_results['benchmark']))

        ax.scatter(index, df_results['beam_symb_improvement'], alpha=0.7, s=50, label='Ours')
        ax.scatter(index, df_results['best_size_improvement'], alpha=0.7, s=50, label='Guoq-Rewrite')

        ax.set_title('Improvement Ratio (Ours vs Guoq-Rewrite)', fontsize=20)
        ax.set_xlabel('Benchmarks (Sorted)', fontsize=16)
        ax.set_ylabel('Improvement Ratio', fontsize=16)
        ax.set_xticks([])  # Remove x-axis labels
        ax.grid(True)
        ax.legend(fontsize=14)
        
        # Add progress bars at the bottom
        fig.subplots_adjust(bottom=0.3)
        
        # Ours progress bar
        ax_ours = fig.add_axes([0.25, 0.2, 0.6, 0.04])
        ax_ours.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray'))
        ax_ours.add_patch(patches.Rectangle((0, 0), ours_percentage, 1, facecolor='orange'))
        ax_ours.set_xticks([])
        ax_ours.set_yticks([])
        ax_ours.text(-0.01, 0.5, f"Ours better ({ours_better}/{total})", ha='right', va='center', fontsize=14)

        # Guoq progress bar
        ax_guoq = fig.add_axes([0.25, 0.15, 0.6, 0.04])
        ax_guoq.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray'))
        ax_guoq.add_patch(patches.Rectangle((0, 0), guoq_percentage, 1, facecolor='skyblue'))
        ax_guoq.set_xticks([])
        ax_guoq.set_yticks([])
        ax_guoq.text(-0.01, 0.5, f"Other better ({guoq_better}/{total})", ha='right', va='center', fontsize=14)

        # Equal progress bar
        ax_equal = fig.add_axes([0.25, 0.1, 0.6, 0.04])
        ax_equal.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray'))
        ax_equal.add_patch(patches.Rectangle((0, 0), equal_percentage, 1, facecolor='lightgray'))
        ax_equal.set_xticks([])
        ax_equal.set_yticks([])
        ax_equal.text(-0.01, 0.5, f"Equals ({equal}/{total})", ha='right', va='center', fontsize=14)


        plt.savefig('improvement_scatter_plot.png')
        plt.show()
        
        print("Scatter plot has been saved as improvement_scatter_plot.png")

    except FileNotFoundError as e:
        print(f"Error: {e}. Please ensure 'guoq_results1.csv' is in the correct directory.")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == '__main__':
    plot_improvement_scatter()
