# %%
import csv
import seaborn as sns
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FormatStrFormatter
from matplotlib.axes import Axes
import numpy as np
import json
import os
import tarfile
from scipy.stats import gmean
from qiskit import QuantumCircuit
import gc

import warnings

warnings.filterwarnings("ignore")

plt.rcParams["pdf.fonttype"] = 42
plt.rcParams["ps.fonttype"] = 42

sns.set_theme(palette="colorblind")
sns.set_style("ticks", {"font.family": "serif", "axes.grid": True})
sns.set_context("notebook", font_scale=3)

TYPE_ORDER = ["qaoa", "vqe", "nisq", "qpe", "toffoli", "qft", "ftqc"]
MARKERS = ["o", "s", "^", "P", "D", "*", "v"]
PALETTE = [
    "#0173b2",
    "#de8f05",
    "#029e73",
    "#d55e00",
    "#cc78bc",
    "#ca9161",
    "#fbafe4",
    "#949494",
]
MARKERS_DICT = {k: v for k, v in zip(MARKERS, TYPE_ORDER)}

TITLE_YAXIS_FONT = 40
SUPTITLE_FONT = 40
BAR_LABEL_FONT = 29
PAD_X = 49
ASPECT_RATIO = 3

FIDELITY_COLOR = "#e7e7eb"
TOTAL_COLOR = "#fffbdd"
TCOUNT_COLOR = TOTAL_COLOR  # "#ffe9ec"

BEAM = "BEAM"
BEAM_MCMC = "BEAM_MCMC"
MCMC = "MCMC"
SA = "SIM_ANN"
TWO_Q = "TWO_Q"
T = "T"
TOTAL = "TOTAL"
NONE = "NONE"

IBM_OLD = "IBMQ20"
IBM_NEW = "IBM-EAGLE"
ION = "IONQ"
NAM = "Nam"

VOQC = "VOQC"
TKET = "TKET"
QUESO = "QUESO"
QISKIT = "Qiskit"
QUARTZ = "Quartz"
QUARL = "Quarl"
SYNTHETIQ = "Synthetiq"
BQSKIT = "BQSKit"
PYZX = "PyZX"

US = "GUOQ"
US_REWRITE = f"{US}-REWRITE"
US_RESYNTH = f"{US}-RESYNTH"
US_BEAM = f"{US}-BEAM"
US_NONUNIFORM = f"{US}-REWEIGH"
US_RR_RESYNTH = f"{US}-SEQ\nREWRITE-RESYNTH"
US_RESYNTH_RR = f"{US}-SEQ\nRESYNTH-REWRITE"

FIDELITIES = {
    "rz": 1,
    "u1": 1,
    "u2": 0.9997188818297166,
    "u3": 0.9997188818297166,
    "cx": 0.9885404797320638,
    "sx": 0.9997188818297166,
    "x": 0.9997188818297166,
    "rx": 0.9998,  # https://ionq.com/quantum-systems/forte
    "ry": 0.9998,
    "rxx": 0.996,
}

GATE_SET_MAP = {
    "ibmo": "ibm",
    "ibmn": "ibmnew",
    "nam": "nam_rz",
    "ion": "ion",
    "cliffordt": "nam_t_tdg"
}

qiskit_ibmo_better = None
qiskit_ibmo_match = None
tket_ibmo_better = None
tket_ibmo_match = None
voqc_ibmo_better = None
voqc_ibmo_match = None
bqskit_ibmo_better = None
bqskit_ibmo_match = None
queso_ibmo_better = None
queso_ibmo_match = None
quartz_ibmo_better = None
quartz_ibmo_match = None
quarl_ibmn_better = None
quarl_ibmn_match = None

def fill_missing_data(fresh_df, backup_df, total_trials=1, circuits_ran=[]):
    if fresh_df.size == 0:
        return backup_df[~backup_df["circuit_id"].isin(circuits_ran)]
    # Ensure we have circuit_id column in both dataframes
    if "circuit_id" not in fresh_df.columns or "circuit_id" not in backup_df.columns:
        raise ValueError("Both dataframes must contain 'circuit_id' column")

    # Initialize the result DataFrame with fresh data
    result_df = fresh_df.copy()

    # Get unique circuit IDs from both dataframes
    fresh_circuits = set(fresh_df["circuit_id"].unique())
    backup_circuits = set(backup_df["circuit_id"].unique())

    # Process circuits that are in fresh data
    for circuit_id in fresh_circuits:
        # Get backup trials for this circuit
        circuit_backups = backup_df[backup_df["circuit_id"] == circuit_id]

        if len(circuit_backups) == 0:
            print(f"Warning: No backup data found for circuit_id {circuit_id}")
            continue

        # Calculate how many additional trials we need
        needed_trials = total_trials - 1  # -1 because we already have the fresh trial

        # If we have enough backup trials
        if len(circuit_backups) >= needed_trials:
            # Randomly sample the needed number of trials
            selected_backups = circuit_backups.sample(n=needed_trials, random_state=42)
        else:
            # If we don't have enough backups, use all available and warn the user
            selected_backups = circuit_backups
            print(
                f"Warning: Only {len(circuit_backups)} backup trials available for circuit_id {circuit_id}"
            )

        # Append selected backup trials to result
        result_df = pd.concat([result_df, selected_backups], ignore_index=True)

    # Add all trials for circuits that are only in backup data
    backup_only_circuits = backup_circuits - fresh_circuits
    if backup_only_circuits:
        backup_only_circuits = [x for x in backup_only_circuits if x not in circuits_ran]
        backup_only_data = backup_df[backup_df["circuit_id"].isin(backup_only_circuits)]
        result_df = pd.concat([result_df, backup_only_data], ignore_index=True)

    return result_df

def get_fidelity(file):
    fidelity = 1
    if not os.path.exists(file):
        return 0
    circuit = QuantumCircuit.from_qasm_file(file)
    gates = circuit.count_ops()
    for gate, count in gates.items():
        fidelity *= FIDELITIES[gate] ** count
    # for gate in circuit.data:
    #     fidelity *= FIDELITIES[gate.operation.name]
    return fidelity


def get_fidelity_circuit(circuit):
    fidelity = 1
    gates = circuit.count_ops()
    for gate, count in gates.items():
        fidelity *= FIDELITIES[gate] ** count
    return fidelity


def add_fidelity(df, dir):
    df["best_size_fidelity"] = df.apply(
        lambda x: get_fidelity(
            f"{dir}/results_{x['circuit_id']}/optimized_{x['cluster']}_{x['process']}_{x['circuit_id']}.qasm"
        ),
        axis=1,
    )
    gc.collect()


def fid_helper(d, circuit_id, gateset):
    if circuit_id in d:
        return d[circuit_id]
    fid = get_fidelity(f"benchmarks/{gateset}/{circuit_id}.qasm")
    d[circuit_id] = fid
    return fid


def add_fidelity_us(df, dir, original=True):
    df["best_size_fidelity"] = df.apply(
        lambda x: get_fidelity(
            f"{dir}/results_{x['circuit_id']}/latest_sol_{x['cluster']}_{x['process']}_{x['circuit_id']}.qasm"
        ),
        axis=1,
    )
    if original:
        original_dict = {}
        df["original_fidelity"] = df.apply(
            lambda x: fid_helper(original_dict, x["circuit_id"], x["gateset"]),
            axis=1,
        )
    gc.collect()


def extract(tar_filename, target_dir):
    file = tarfile.open(tar_filename)
    file.extractall(target_dir)
    file.close()


def import_data(dir, get_extra_info=False):
    data = []
    for d in os.listdir(dir):
        if not os.path.isdir(f"{dir}/{d}"):
            continue
        for f in os.listdir(f"{dir}/{d}"):
            if ".json" in f:
                with open(f"{dir}/{d}/{f}", "r") as file:
                    contents = file.read()
                    contents = contents.replace(",\n}", "\n}")
                    results = json.loads(contents)
                    results.pop("error", None)
                    results.pop("resynth_errors", None)
                    results["original_total"] = int(results["original_total"])
                    results["original_2q"] = int(results["original_2q"])
                    results["original_t"] = (
                        int(results["original_t"]) if "original_t" in results else 0
                    )
                    results["best_circuit_size"] = (
                        int(results["best_circuit_size"])
                        if "best_circuit_size" in results
                        else (
                            int(results["optimized_total"])
                            if "optimized_total" in results
                            else results["original_total"]
                        )
                    )
                    results["best_size_2q"] = (
                        int(results["best_size_2q"])
                        if "best_size_2q" in results
                        else (
                            int(results["optimized_2q"])
                            if "optimized_2q" in results
                            else results["original_2q"]
                        )
                    )
                    results["best_size_t"] = (
                        int(results["best_size_t"])
                        if "best_size_t" in results
                        else (
                            int(results["optimized_t"])
                            if "optimized_t" in results
                            else results["original_t"]
                        )
                    )
                    if "guoq_config" in results:
                        results.update(results["guoq_config"])
                    data.append(results)
                    if get_extra_info:
                        pass
    df = pd.DataFrame(data)
    if get_extra_info:
        pass
    return df


def import_quartz_data(directory, time=None):
    results = []
    directory = f"{directory}/final_results{f'_{time}' if time is not None else ''}"
    for file in os.listdir(directory):
        circ = QuantumCircuit.from_qasm_file(f"{directory}/{file}")
        results.append(
            {
                "circuit_id": file.replace(".qasm", ""),
                "best_circuit_size": circ.size(),
                "best_size_2q": circ.num_nonlocal_gates(),
                "best_size_fidelity": (
                    get_fidelity_circuit(circ) if "ibm" in directory else None
                ),
                "method": "quartz",
            }
        )
    return pd.DataFrame(results)


def import_quarl_data(directory):
    results = []
    directory = f"{directory}/final_results"
    if not os.path.exists(directory):
        return pd.DataFrame(results)
    for file in os.listdir(directory):
        data = json.load(open(f"{directory}/{file}"))[0]
        circ = QuantumCircuit.from_qasm_str(data["qasm"])
        results.append(
            {
                "circuit_id": data["name"],
                "best_circuit_size": circ.size(),
                "best_size_2q": circ.num_nonlocal_gates(),
                "best_size_fidelity": (
                    get_fidelity_circuit(circ) if "ibm" in directory else None
                ),
                "method": "quarl",
            }
        )
    return pd.DataFrame(results)


def process_other(df, method, original, metric):
    df = df[["method", "circuit_id", f"best_size_{metric}"]]
    df = pd.merge(df, original, on="circuit_id", how="right")
    df = df.fillna({"method": method})
    return df


def process_other2(df, method, original, metric1, metric2):
    df = df[["method", "circuit_id", f"best_size_{metric1}", f"best_size_{metric2}"]]
    df = pd.merge(df, original, on="circuit_id", how="right")
    df = df.fillna({"method": method})
    return df


def add_gate_set_column_us(df):
    df["gateset"] = df.apply(
        lambda x: GATE_SET_MAP[x["gate_set"].lower()],
        axis=1,
    )


def add_method_column_us(df: pd.DataFrame):
    df["method"] = df.apply(
        lambda x: f"guoq {x['search_strategy']} {x['opt_obj']} {x['resynth_alg'] if 'resynth_alg' in x else "BQSKITorSYNTHETIQ"} {x['gateset']} {x['temperature']} {x['cooling_rate']} {x['prune_temperature'] if 'prune_temperature' in x else 0} {x['iters_before_prune'] if 'iters_before_prune' in x else -1} {x['secs_before_prune'] if 'secs_before_prune' in x else -1} {x['queue_size']} {x['apply_once'] if 'apply_once' in x else False}",
        axis=1,
    )


def filter(df):
    bad = [
        # incorrect jku circs
        "qft_10",
        "qft_16",
        "ground_state_estimation_10",
    ]
    mask = df.applymap(lambda x: x in bad)
    rows_with_strings = mask.any(axis=1)
    return df[~rows_with_strings]


def plot(
    ax: Axes,
    bar: Axes,
    df: pd.DataFrame,
    baseline,
    us,
    title,
    metric,
    ylabel=None,
    legend=False,
    show_total_benchmarks=False,
    background_shade=None,
    num_tools=None,
    gate_set=None
):
    original = f"original_{metric}"
    metric_col_name = f"best_size_{metric}"
    df = df.loc[:, ["method", "circuit_id", original, metric_col_name]]

    us_df = df.loc[df["method"] == us]
    averaged_gate_count_us = us_df.pivot_table(
        index="circuit_id", columns=["method"], values=metric_col_name
    ).reset_index()
    us_df["reduction"] = (us_df[original] - us_df[metric_col_name]) / us_df[original]
    us_df["avg_reduction"] = us_df.apply(
        lambda x: (
            x[original]
            - averaged_gate_count_us.loc[
                averaged_gate_count_us["circuit_id"] == x["circuit_id"], us
            ].values[0]
        )
        / x[original],
        axis=1,
    )
    us_df["avg_2q"] = us_df.apply(
        lambda x: (
            averaged_gate_count_us.loc[
                averaged_gate_count_us["circuit_id"] == x["circuit_id"], us
            ].values[0]
        ),
        axis=1,
    )
    us_df["reduction"] = us_df["reduction"].fillna(0)
    us_df["avg_reduction"] = us_df["avg_reduction"].fillna(0)
    us_df.sort_values("avg_reduction", inplace=True)

    baseline_df = df.loc[df["method"] == baseline]
    baseline_df = baseline_df.fillna({metric_col_name: baseline_df[original]})

    averaged_gate_count_base = baseline_df.pivot_table(
        index="circuit_id", columns=["method"], values=metric_col_name
    ).reset_index()
    baseline_df["reduction"] = (
        baseline_df[original] - baseline_df[metric_col_name]
    ) / baseline_df[original]

    baseline_df["baseline_avg_reduction"] = baseline_df.apply(
        lambda x: (
            x[original]
            - averaged_gate_count_base.loc[
                averaged_gate_count_base["circuit_id"] == x["circuit_id"], baseline
            ].values[0]
        )
        / x[original],
        axis=1,
    )
    baseline_df["baseline_avg_2q"] = baseline_df.apply(
        lambda x: (
            averaged_gate_count_base.loc[
                averaged_gate_count_base["circuit_id"] == x["circuit_id"], baseline
            ].values[0]
        ),
        axis=1,
    )
    baseline_df["reduction"] = baseline_df["reduction"].fillna(0)
    baseline_df["baseline_avg_reduction"] = baseline_df[
        "baseline_avg_reduction"
    ].fillna(0)
    baseline_df = pd.merge(
        us_df[["avg_reduction", "circuit_id", "avg_2q"]], baseline_df, on="circuit_id"
    )
    baseline_df["difference"] = baseline_df["avg_2q"] - baseline_df["baseline_avg_2q"]

    #######################################################
    print(f"GUOQ average reduction: {us_df["avg_reduction"].mean()*100}%" )
    print(f"{baseline}: {baseline_df["baseline_avg_reduction"].mean()*100}%" )
    #######################################################

    g1 = sns.pointplot(
        data=baseline_df,
        x="circuit_id",
        y="reduction",
        linestyle="none",
        err_kws={"linewidth": 0.5},
        legend=legend,
        ax=ax,
    )
    g = sns.pointplot(
        data=us_df,
        x="circuit_id",
        y="reduction",
        linestyle="none",
        err_kws={"linewidth": 0.5},
        legend=legend,
        ax=ax,
    )

    g.set_title(title, fontdict={"fontsize": TITLE_YAXIS_FONT})

    # y axis
    g.set_ylabel(ylabel, fontsize=TITLE_YAXIS_FONT)
    ax.yaxis.set_major_formatter(FormatStrFormatter("%.1f"))

    total_benchmarks = len(us_df["circuit_id"].unique())
    # x axis
    g.tick_params(axis="both", which="major")
    g.grid(False, axis="x")
    g.set(xlabel=None, xticklabels=[])
    ax.tick_params(axis="x", bottom=False)
    ax.axes.get_xaxis().get_label().set_visible(False)

    # horizontal line at 0
    x = np.arange(0, total_benchmarks, 1)
    ax.plot(x, [0] * len(x), "black", linestyle="none")

    ax.set_box_aspect(1)
    if background_shade is not None:
        ax.set_facecolor(background_shade)

    # count of benchmarks above/on/below 0
    tl = (
        (ax.get_xlim()[1] - ax.get_xlim()[0]) + ax.get_xlim()[0] + 0.5,
        (ax.get_ylim()[1] - ax.get_ylim()[0]) * 0.90 + ax.get_ylim()[0],
    )

    outperform = len(
        baseline_df[
            (baseline_df["avg_reduction"] > baseline_df["baseline_avg_reduction"])
        ]["circuit_id"].unique()
    )
    match = len(
        baseline_df[
            (baseline_df["avg_reduction"] == baseline_df["baseline_avg_reduction"])
        ]["circuit_id"].unique()
    )
    underperform = len(
        baseline_df[
            (baseline_df["avg_reduction"] < baseline_df["baseline_avg_reduction"])
        ]["circuit_id"].unique()
    )

    bar_data = {
        "category": ["GUOQ better", "match", "GUOQ worse"],
        "# of benchmarks": [outperform, match, underperform],
    }

    if gate_set == IBM_OLD:
        if baseline == QISKIT:
            global qiskit_ibmo_better
            global qiskit_ibmo_match
            qiskit_ibmo_better = outperform
            qiskit_ibmo_match = match
        elif baseline == TKET:
            global tket_ibmo_better
            global tket_ibmo_match
            tket_ibmo_better = outperform
            tket_ibmo_match = match
        elif baseline == VOQC:
            global voqc_ibmo_better
            global voqc_ibmo_match
            voqc_ibmo_better = outperform
            voqc_ibmo_match = match
        elif baseline == BQSKIT:
            global bqskit_ibmo_better
            global bqskit_ibmo_match
            bqskit_ibmo_better = outperform
            bqskit_ibmo_match = match
        elif baseline == QUESO:
            global queso_ibmo_better
            global queso_ibmo_match
            queso_ibmo_better = outperform
            queso_ibmo_match = match
        elif baseline == QUARTZ:
            global quartz_ibmo_better
            global quartz_ibmo_match
            quartz_ibmo_better = outperform
            quartz_ibmo_match = match
    elif gate_set == IBM_NEW:
        if baseline == QUARL:
            global quarl_ibmn_better
            global quarl_ibmn_match
            quarl_ibmn_better = outperform
            quarl_ibmn_match = match
    

    b = sns.barplot(
        bar_data,
        x="# of benchmarks",
        y="category",
        ax=bar,
        hue="category",
        palette=[PALETTE[1], PALETTE[7], PALETTE[0]],
    )
    # b.grid(False)
    bar.axes.get_yaxis().get_label().set_visible(False)
    bar.set_box_aspect(1 / ASPECT_RATIO)
    if show_total_benchmarks:
        bar.set_xlabel(f"# of benchmarks{"\n" if num_tools < 3 else " "}({len(us_df["circuit_id"].unique())} total)")
    if baseline == PYZX and metric == "2q" and num_tools < 3:
        bar.set_xlabel(f"# of benchmarks\n")
    # for i in bar.containers:
    #     bar.bar_label(i, fontsize=BAR_LABEL_FONT)
    bar.set_xlim(0, total_benchmarks + PAD_X)
    _, xmax = bar.get_xlim()
    # bar.set_xlim(0, xmax + 300)
    max_index = bar_data["# of benchmarks"].index(max(bar_data["# of benchmarks"]))
    for i, v in enumerate(bar_data["# of benchmarks"]):
        bar.text(
            v + 1,
            i + 0.3,
            str(v),
            color="black",
            fontweight="bold" if i == max_index else "normal",
            fontsize=BAR_LABEL_FONT,
            ha="left",
            # va="center",
        )

    if metric == "t":
        g.set_facecolor(TCOUNT_COLOR)
        bar.set_facecolor(TCOUNT_COLOR)

    return g


def plot_fidelity(
    ax: Axes,
    bar: Axes,
    df: pd.DataFrame,
    baseline,
    us,
    title,
    metric,
    ylabel=None,
    legend=False,
    show_total_benchmarks=False,
    background_shade=None,
    num_tools=None,
    gate_set=None
):
    original = f"original_{metric}"
    metric_col_name = f"best_size_{metric}"
    df = df.loc[:, ["method", "circuit_id", original, metric_col_name]]

    us_df = df.loc[df["method"] == us]
    averaged_gate_count = us_df.pivot_table(
        index="circuit_id", columns=["method"], values=metric_col_name
    ).reset_index()
    us_df["avg"] = us_df.apply(
        lambda x: averaged_gate_count.loc[
            averaged_gate_count["circuit_id"] == x["circuit_id"], us
        ].values[0],
        axis=1,
    )
    us_df.sort_values("avg", inplace=True)

    baseline_df = df.loc[df["method"] == baseline]
    baseline_df = baseline_df.fillna({metric_col_name: baseline_df[original]})
    averaged_gate_count_base = baseline_df.pivot_table(
        index="circuit_id", columns=["method"], values=metric_col_name
    ).reset_index()
    baseline_df["baseline_avg"] = baseline_df.apply(
        lambda x: averaged_gate_count_base.loc[
            averaged_gate_count_base["circuit_id"] == x["circuit_id"], baseline
        ].values[0],
        axis=1,
    )
    baseline_df = pd.merge(us_df[["avg", "circuit_id"]], baseline_df, on="circuit_id")
    # baseline_df.to_csv("baseline_df.csv")

    g1 = sns.pointplot(
        data=baseline_df,
        x="circuit_id",
        y=metric_col_name,
        linestyle="none",
        err_kws={"linewidth": 0.5},
        legend=legend,
        ax=ax,
    )
    g = sns.pointplot(
        data=us_df,
        x="circuit_id",
        y=metric_col_name,
        linestyle="none",
        err_kws={"linewidth": 0.5},
        legend=legend,
        ax=ax,
    )

    g.set_facecolor(FIDELITY_COLOR)

    g.set_title(title, fontdict={"fontsize": TITLE_YAXIS_FONT})
    # g.set_title("")

    # y axis
    g.set_ylabel(ylabel, fontsize=TITLE_YAXIS_FONT)
    ax.yaxis.set_major_formatter(FormatStrFormatter("%.1f"))

    total_benchmarks = len(us_df["circuit_id"].unique())
    # x axis
    g.tick_params(axis="both", which="major")
    g.grid(False, axis="x")
    g.set(xlabel=None, xticklabels=[])
    ax.tick_params(axis="x", bottom=False)
    ax.axes.get_xaxis().get_label().set_visible(False)

    # horizontal line at 0
    x = np.arange(0, total_benchmarks, 1)
    ax.plot(x, [0] * len(x), "black", linestyle="none")

    ax.set_box_aspect(1)
    if background_shade is not None:
        ax.set_facecolor(background_shade)

    # count of benchmarks above/on/below 0
    tl = (
        (ax.get_xlim()[1] - ax.get_xlim()[0]) + ax.get_xlim()[0] + 0.5,
        (ax.get_ylim()[1] - ax.get_ylim()[0]) * 0.90 + ax.get_ylim()[0],
    )

    outperform = len(
        baseline_df[(baseline_df["avg"] > baseline_df[f"baseline_avg"])][
            "circuit_id"
        ].unique()
    )
    match = len(
        baseline_df[(baseline_df["avg"] == baseline_df[f"baseline_avg"])][
            "circuit_id"
        ].unique()
    )
    underperform = len(
        baseline_df[(baseline_df["avg"] < baseline_df[f"baseline_avg"])][
            "circuit_id"
        ].unique()
    )

    bar_data = {
        "category": ["GUOQ better", "match", "GUOQ worse"],
        "# of benchmarks": [outperform, match, underperform],
    }

    b = sns.barplot(
        bar_data,
        x="# of benchmarks",
        y="category",
        ax=bar,
        hue="category",
        palette=[PALETTE[1], PALETTE[7], PALETTE[0]],
    )
    # b.grid(False)
    bar.axes.get_yaxis().get_label().set_visible(False)
    bar.set_box_aspect(1 / ASPECT_RATIO)
    # for i in bar.containers:
    #     bar.bar_label(i, fontsize=BAR_LABEL_FONT)
    if show_total_benchmarks:
        bar.set_xlabel(f"# of benchmarks{"\n" if num_tools < 3 else " "}({len(us_df["circuit_id"].unique())} total)")
    bar.set_xlim(0, total_benchmarks + PAD_X)
    _, xmax = bar.get_xlim()
    # bar.set_xlim(0, xmax + 300)
    max_index = bar_data["# of benchmarks"].index(max(bar_data["# of benchmarks"]))
    for i, v in enumerate(bar_data["# of benchmarks"]):
        bar.text(
            v + 1,
            i + 0.3,
            str(v),
            color="black",
            fontweight="bold" if i == max_index else "normal",
            fontsize=BAR_LABEL_FONT,
            ha="left",
            # va="center",
        )

    bar.set_facecolor(FIDELITY_COLOR)

    return g


def plot_row(fig, axs, df, baseline, comparisons, titles, ylabel, metric, fidelity, gate_set):
    f = plot_fidelity if fidelity else plot
    ii = 0
    for i in range(len(axs)):
        if comparisons[i] is None:
            axs[i].remove()
            continue
        if ii == 0:
            f(
                axs[i][0],
                axs[i][1],
                df,
                comparisons[i],
                baseline,
                titles[i],
                metric,
                ylabel=ylabel,
                legend=False,
                show_total_benchmarks=True and not (comparisons[i] == PYZX and metric == "2q"),
                num_tools=len(comparisons),
                gate_set=gate_set
            )
            axs[i][0].yaxis.set_tick_params(labelbottom=True)
        else:
            f(
                axs[i][0],
                axs[i][1],
                df,
                comparisons[i],
                baseline,
                titles[i],
                metric,
                legend=False,
                num_tools=len(comparisons),
                gate_set=gate_set
            )
        ii += 1


def plot_all(
    df,
    baseline,
    comparisons,
    filename,
    gateset,
    metric,
    ylabel="2q",
    suptitle=None,
    fidelity=False,
    legend=True,
):
    ylabel = (
        r"$\bf{fidelity}$" if fidelity else r"$\bf{" + ylabel + "}$" + " gate reduction"
    )
    fig, axs = plt.subplots(
        nrows=len(comparisons) * 2,
        ncols=len(comparisons[0]),
        sharey="row",
        figsize=(
            (len(comparisons[0])) * (10 if len(comparisons[0]) == 1 else 8),
            (len(comparisons)) * 5 * 2,
        ),
        height_ratios=[ASPECT_RATIO, 1] * len(comparisons),
    )

    for i in range(len(comparisons)):
        titles = comparisons[i]
        plot_row(
            fig,
            (
                list(zip(axs[i * 2], axs[i * 2 + 1]))
                if len(comparisons[0]) > 1
                else list(zip([axs[i * 2]], [axs[i * 2 + 1]]))
            ),
            df,
            baseline,
            comparisons[i],
            titles,
            ylabel,
            metric,
            fidelity,
            gateset
        )

    if suptitle is not None:
        if not fidelity:
            fig.suptitle(
                f"Gate set: {gateset}",
                fontsize=SUPTITLE_FONT,
                fontweight="bold",
                y=suptitle,
            )

    plt.tight_layout()
    fig.savefig(f"graphs/{filename}.pdf", bbox_inches="tight", pad_inches=0.1)
    # plt.show()
    plt.close()

# %%
circuits_ran = [
    x.replace("results_", "") for x in os.listdir("fresh_results/rq1/ibm_new/bqskit")
]

# %% [markdown]
# # RQ1 IBMN

# %%
bqskit_ibmn = import_data("fresh_results/rq1/ibm_new/bqskit")
if bqskit_ibmn.size > 0:
    add_fidelity(bqskit_ibmn, "fresh_results/rq1/ibm_new/bqskit")

# %%
qiskit_ibmn = import_data("fresh_results/rq1/ibm_new/qiskit")
if qiskit_ibmn.size > 0:
    add_fidelity(qiskit_ibmn, "fresh_results/rq1/ibm_new/qiskit")

# %%
tket_ibmn = import_data("fresh_results/rq1/ibm_new/tket")
if tket_ibmn.size > 0:
    add_fidelity(tket_ibmn, "fresh_results/rq1/ibm_new/tket")

# %%
quartz_ibmn = import_quartz_data("fresh_results/rq1/ibm_new/quartz")

# %%
quarl_ibmn_1 = import_quarl_data("fresh_results/rq1/ibm_new/quarl-1")
quarl_ibmn_2 = import_quarl_data("fresh_results/rq1/ibm_new/quarl-2")
quarl_ibmn_3 = import_quarl_data("fresh_results/rq1/ibm_new/quarl-3")

# %%
queso_ibmn = import_data(f"fresh_results/rq1/ibm_new/queso")
add_gate_set_column_us(queso_ibmn)
add_method_column_us(queso_ibmn)
add_fidelity_us(queso_ibmn, "fresh_results/rq1/ibm_new/queso", False)

# %%
guoq_ibmn = import_data(f"fresh_results/rq1/ibm_new/guoq")
add_gate_set_column_us(guoq_ibmn)
add_method_column_us(guoq_ibmn)
add_fidelity_us(guoq_ibmn, "fresh_results/rq1/ibm_new/guoq")

# %%
bqskit_ibmn_paper = pd.read_csv("paper_results/rq1_bqskit_ibmn.csv")
qiskit_ibmn_paper = pd.read_csv("paper_results/rq1_qiskit_ibmn.csv")
tket_ibmn_paper = pd.read_csv("paper_results/rq1_tket_ibmn.csv")
quartz_ibmn_paper = pd.read_csv("paper_results/rq1_quartz_ibmn.csv")
quarl_ibmn_1_paper = pd.read_csv("paper_results/rq1_quarl_ibmn_1.csv")
quarl_ibmn_2_paper = pd.read_csv("paper_results/rq1_quarl_ibmn_2.csv")
quarl_ibmn_3_paper = pd.read_csv("paper_results/rq1_quarl_ibmn_3.csv")
queso_ibmn_paper = pd.read_csv("paper_results/rq1_queso_ibmn.csv")
guoq_ibmn_paper = pd.read_csv("paper_results/rq1_guoq_ibmn.csv")

# %%
guoq_ibmn = fill_missing_data(guoq_ibmn, guoq_ibmn_paper, total_trials=10)
queso_ibmn = fill_missing_data(queso_ibmn, queso_ibmn_paper, circuits_ran=circuits_ran)
tket_ibmn = fill_missing_data(tket_ibmn, tket_ibmn_paper, circuits_ran=circuits_ran)
qiskit_ibmn = fill_missing_data(
    qiskit_ibmn, qiskit_ibmn_paper, circuits_ran=circuits_ran
)
bqskit_ibmn = fill_missing_data(
    bqskit_ibmn, bqskit_ibmn_paper, circuits_ran=circuits_ran
)
quartz_ibmn = fill_missing_data(
    quartz_ibmn, quartz_ibmn_paper, circuits_ran=circuits_ran
)
quarl_ibmn_1 = fill_missing_data(
    quarl_ibmn_1, quarl_ibmn_1_paper, circuits_ran=circuits_ran
)
quarl_ibmn_2 = fill_missing_data(
    quarl_ibmn_2, quarl_ibmn_2_paper, circuits_ran=circuits_ran
)
quarl_ibmn_3 = fill_missing_data(
    quarl_ibmn_3, quarl_ibmn_3_paper, circuits_ran=circuits_ran
)

# %%
original = guoq_ibmn[
    ["circuit_id", "original_2q", "original_fidelity"]
].drop_duplicates()
guoq_data = pd.concat([guoq_ibmn])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
        "original_fidelity",
        "best_size_fidelity",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other2(queso_ibmn, "queso", original, "2q", "fidelity"),
        process_other2(tket_ibmn, "tket", original, "2q", "fidelity"),
        process_other2(qiskit_ibmn, "qiskit", original, "2q", "fidelity"),
        process_other2(bqskit_ibmn, "bqskit", original, "2q", "fidelity"),
        process_other2(quartz_ibmn, "quartz", original, "2q", "fidelity"),
        process_other2(quarl_ibmn_1, "quarl", original, "2q", "fidelity"),
        process_other2(quarl_ibmn_2, "quarl", original, "2q", "fidelity"),
        process_other2(quarl_ibmn_3, "quarl", original, "2q", "fidelity"),
    ]
)

all_data.replace(
    {
        "guoq BEAM TOTAL NONE ibmnew 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM TOTAL NONE ibmnew 3600 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM_MCMC FIDELITY BQSKITorSYNTHETIQ ibmnew 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM_MCMC FIDELITY BQSKIT ibmnew 3600 10.0 0.0 0.0 1 130 1 False": US,
        "queso": QUESO,
        "tket": TKET,
        "voqc": VOQC,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "quartz": QUARTZ,
        "quarl": QUARL,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [QISKIT, TKET] + [BQSKIT, QUARTZ, QUARL, QUESO],
]

print(f"Plotting RQ1 Fig 8 for {IBM_NEW}")
print("RQ1 Summary Claim:")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig8_rq1_ibmn_2q",
    IBM_NEW,
    "2q",
)
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig8_rq1_ibmn_fidelity",
    IBM_NEW,
    "fidelity",
    fidelity=True,
)

# %% [markdown]
# # RQ1 IBMO

# %%
bqskit_ibmo = import_data("fresh_results/rq1/ibm_old/bqskit")
if bqskit_ibmo.size > 0:
    add_fidelity(bqskit_ibmo, "fresh_results/rq1/ibm_old/bqskit")

# %%
qiskit_ibmo = import_data("fresh_results/rq1/ibm_old/qiskit")
if qiskit_ibmo.size > 0:
    add_fidelity(qiskit_ibmo, "fresh_results/rq1/ibm_old/qiskit")

# %%
tket_ibmo = import_data("fresh_results/rq1/ibm_old/tket")
if tket_ibmo.size > 0:
    add_fidelity(tket_ibmo, "fresh_results/rq1/ibm_old/tket")

# %%
voqc_ibmo = import_data("fresh_results/rq1/ibm_old/voqc")
if voqc_ibmo.size > 0:
    add_fidelity(voqc_ibmo, "fresh_results/rq1/ibm_old/voqc")

# %%
quartz_ibmo = import_quartz_data("fresh_results/rq1/ibm_old/quartz")

# %%
queso_ibmo = import_data(f"fresh_results/rq1/ibm_old/queso")
add_gate_set_column_us(queso_ibmo)
add_method_column_us(queso_ibmo)
add_fidelity_us(queso_ibmo, "fresh_results/rq1/ibm_old/queso", False)

# %%
guoq_ibmo = import_data(f"fresh_results/rq1/ibm_old/guoq")
add_gate_set_column_us(guoq_ibmo)
add_method_column_us(guoq_ibmo)
add_fidelity_us(guoq_ibmo, "fresh_results/rq1/ibm_old/guoq")

# %%
bqskit_ibmo_paper = pd.read_csv("paper_results/rq1_bqskit_ibmo.csv")
qiskit_ibmo_paper = pd.read_csv("paper_results/rq1_qiskit_ibmo.csv")
tket_ibmo_paper = pd.read_csv("paper_results/rq1_tket_ibmo.csv")
voqc_ibmo_paper = pd.read_csv("paper_results/rq1_voqc_ibmo.csv")
quartz_ibmo_paper = pd.read_csv("paper_results/rq1_quartz_ibmo.csv")
queso_ibmo_paper = pd.read_csv("paper_results/rq1_queso_ibmo.csv")
guoq_ibmo_paper = pd.read_csv("paper_results/rq1_guoq_ibmo.csv")

# %%
guoq_ibmo = fill_missing_data(guoq_ibmo, guoq_ibmo_paper, total_trials=10)
queso_ibmo = fill_missing_data(queso_ibmo, queso_ibmo_paper, circuits_ran=circuits_ran)
tket_ibmo = fill_missing_data(tket_ibmo, tket_ibmo_paper, circuits_ran=circuits_ran)
qiskit_ibmo = fill_missing_data(
    qiskit_ibmo, qiskit_ibmo_paper, circuits_ran=circuits_ran
)
bqskit_ibmo = fill_missing_data(
    bqskit_ibmo, bqskit_ibmo_paper, circuits_ran=circuits_ran
)
voqc_ibmo = fill_missing_data(voqc_ibmo, voqc_ibmo_paper, circuits_ran=circuits_ran)
quartz_ibmo = fill_missing_data(
    quartz_ibmo, quartz_ibmo_paper, circuits_ran=circuits_ran
)

# %%
original = guoq_ibmo[
    ["circuit_id", "original_2q", "original_fidelity"]
].drop_duplicates()
guoq_data = pd.concat([guoq_ibmo])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
        "original_fidelity",
        "best_size_fidelity",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other2(queso_ibmo, "queso", original, "2q", "fidelity"),
        process_other2(tket_ibmo, "tket", original, "2q", "fidelity"),
        process_other2(voqc_ibmo, "voqc", original, "2q", "fidelity"),
        process_other2(qiskit_ibmo, "qiskit", original, "2q", "fidelity"),
        process_other2(bqskit_ibmo, "bqskit", original, "2q", "fidelity"),
        process_other2(quartz_ibmo, "quartz", original, "2q", "fidelity"),
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC FIDELITY BQSKIT ibm 3600 10.0 0.0 0.0 1 10 1 False": US,
        "guoq BEAM_MCMC FIDELITY BQSKITorSYNTHETIQ ibm 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM TOTAL NONE ibm 3600 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM TOTAL NONE ibmnew 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "queso": QUESO,
        "tket": TKET,
        "voqc": VOQC,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "quartz": QUARTZ,
        "quarl": QUARL,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [QISKIT, TKET, VOQC] + [BQSKIT, QUARTZ, QUESO],
]

print(f"Plotting RQ1 for {IBM_OLD}")
plot_all(
    all_data,
    baseline,
    comparisons,
    "rq1_ibmo_2q",
    IBM_OLD,
    "2q",
    # suptitle=1,
)
plot_all(
    all_data,
    baseline,
    comparisons,
    "rq1_ibmo_fidelity",
    IBM_OLD,
    "fidelity",
    fidelity=True,
)

# %% [markdown]
# # RQ1 Ion

# %%
bqskit_ion = import_data("fresh_results/rq1/ion/bqskit")
if bqskit_ion.size > 0:
    add_fidelity(bqskit_ion, "fresh_results/rq1/ion/bqskit")

# %%
qiskit_ion = import_data("fresh_results/rq1/ion/qiskit")
if qiskit_ion.size > 0:
    add_fidelity(qiskit_ion, "fresh_results/rq1/ion/qiskit")

# %%
queso_ion = import_data(f"fresh_results/rq1/ion/queso")
add_gate_set_column_us(queso_ion)
add_method_column_us(queso_ion)
add_fidelity_us(queso_ion, "fresh_results/rq1/ion/queso", False)

# %%
guoq_ion = import_data(f"fresh_results/rq1/ion/guoq")
add_gate_set_column_us(guoq_ion)
add_method_column_us(guoq_ion)
add_fidelity_us(guoq_ion, "fresh_results/rq1/ion/guoq")

# %%
bqskit_ion_paper = pd.read_csv("paper_results/rq1_bqskit_ion.csv")
qiskit_ion_paper = pd.read_csv("paper_results/rq1_qiskit_ion.csv")
queso_ion_paper = pd.read_csv("paper_results/rq1_queso_ion.csv")
guoq_ion_paper = pd.read_csv("paper_results/rq1_guoq_ion.csv")

# %%
guoq_ion = fill_missing_data(guoq_ion, guoq_ion_paper, total_trials=10)
queso_ion = fill_missing_data(queso_ion, queso_ion_paper, circuits_ran=circuits_ran)
qiskit_ion = fill_missing_data(qiskit_ion, qiskit_ion_paper, circuits_ran=circuits_ran)
bqskit_ion = fill_missing_data(bqskit_ion, bqskit_ion_paper, circuits_ran=circuits_ran)

# %%
original = guoq_ion[
    ["circuit_id", "original_2q", "original_fidelity"]
].drop_duplicates()
guoq_data = pd.concat([guoq_ion])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
        "original_fidelity",
        "best_size_fidelity",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other2(queso_ion, "queso", original, "2q", "fidelity"),
        process_other2(qiskit_ion, "qiskit", original, "2q", "fidelity"),
        process_other2(bqskit_ion, "bqskit", original, "2q", "fidelity"),
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC FIDELITY BQSKIT ion 3600 10.0 0.0 0.0 1 22 1 False": US,
        "guoq BEAM_MCMC FIDELITY BQSKITorSYNTHETIQ ion 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM TOTAL NONE ion 3600 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM TOTAL NONE ion 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "queso": QUESO,
        "tket": TKET,
        "voqc": VOQC,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "quartz": QUARTZ,
        "quarl": QUARL,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [QISKIT, BQSKIT, QUESO],
]

print(f"Plotting RQ1 Fig 9 for {ION}")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig9_rq1_ion_2q",
    ION,
    "2q",
    # suptitle=0.95,
)
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig9_rq1_ion_fidelity",
    ION,
    "fidelity",
    fidelity=True,
)

# %% [markdown]
# # RQ1 Nam

# %%
bqskit_nam = import_data("fresh_results/rq1/nam/bqskit")
qiskit_nam = import_data("fresh_results/rq1/nam/qiskit")
tket_nam = import_data("fresh_results/rq1/nam/tket")
voqc_nam = import_data("fresh_results/rq1/nam/voqc")
quartz_nam = import_quartz_data("fresh_results/rq1/nam/quartz")
quarl_nam_1 = import_quarl_data("fresh_results/rq1/nam/quarl-1")
quarl_nam_2 = import_quarl_data("fresh_results/rq1/nam/quarl-2")
quarl_nam_3 = import_quarl_data("fresh_results/rq1/nam/quarl-3")

queso_nam = import_data(f"fresh_results/rq1/nam/queso")
add_gate_set_column_us(queso_nam)
add_method_column_us(queso_nam)

guoq_nam = import_data(f"fresh_results/rq1/nam/guoq")
add_gate_set_column_us(guoq_nam)
add_method_column_us(guoq_nam)

# %%
bqskit_nam_paper = pd.read_csv("paper_results/rq1_bqskit_nam.csv")
qiskit_nam_paper = pd.read_csv("paper_results/rq1_qiskit_nam.csv")
tket_nam_paper = pd.read_csv("paper_results/rq1_tket_nam.csv")
voqc_nam_paper = pd.read_csv("paper_results/rq1_voqc_nam.csv")
quartz_nam_paper = pd.read_csv("paper_results/rq1_quartz_nam.csv")
quarl_nam_1_paper = pd.read_csv("paper_results/rq1_quarl_nam_1.csv")
quarl_nam_2_paper = pd.read_csv("paper_results/rq1_quarl_nam_2.csv")
quarl_nam_3_paper = pd.read_csv("paper_results/rq1_quarl_nam_3.csv")
queso_nam_paper = pd.read_csv("paper_results/rq1_queso_nam.csv")
guoq_nam_paper = pd.read_csv("paper_results/rq1_guoq_nam.csv")

# %%
guoq_nam = fill_missing_data(guoq_nam, guoq_nam_paper, total_trials=10)
queso_nam = fill_missing_data(queso_nam, queso_nam_paper, circuits_ran=circuits_ran)
tket_nam = fill_missing_data(tket_nam, tket_nam_paper, circuits_ran=circuits_ran)
qiskit_nam = fill_missing_data(qiskit_nam, qiskit_nam_paper, circuits_ran=circuits_ran)
bqskit_nam = fill_missing_data(bqskit_nam, bqskit_nam_paper, circuits_ran=circuits_ran)
voqc_nam = fill_missing_data(voqc_nam, voqc_nam_paper, circuits_ran=circuits_ran)
quartz_nam = fill_missing_data(quartz_nam, quartz_nam_paper, circuits_ran=circuits_ran)
quarl_nam_1 = fill_missing_data(
    quarl_nam_1, quarl_nam_1_paper, circuits_ran=circuits_ran
)
quarl_nam_2 = fill_missing_data(
    quarl_nam_2, quarl_nam_2_paper, circuits_ran=circuits_ran
)
quarl_nam_3 = fill_missing_data(
    quarl_nam_3, quarl_nam_3_paper, circuits_ran=circuits_ran
)

# %%
original = guoq_nam[["circuit_id", "original_2q"]].drop_duplicates()
guoq_data = pd.concat([guoq_nam])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other(queso_nam, "queso", original, "2q"),
        process_other(tket_nam, "tket", original, "2q"),
        process_other(voqc_nam, "voqc", original, "2q"),
        process_other(qiskit_nam, "qiskit", original, "2q"),
        process_other(bqskit_nam, "bqskit", original, "2q"),
        process_other(quartz_nam, "quartz", original, "2q"),
        process_other(quarl_nam_1, "quarl", original, "2q"),
        process_other(quarl_nam_2, "quarl", original, "2q"),
        process_other(quarl_nam_3, "quarl", original, "2q"),
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC TWO_Q BQSKIT nam 3600 10.0 0.0 0.0 1 180 1 False": US,
        "guoq BEAM_MCMC TWO_Q BQSKITorSYNTHETIQ nam_rz 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM TOTAL NONE nam 3600 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM TOTAL NONE nam_rz 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "queso": QUESO,
        "tket": TKET,
        "voqc": VOQC,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "quartz": QUARTZ,
        "quarl": QUARL,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [QISKIT, TKET, VOQC] + [BQSKIT, QUESO, QUARTZ, QUARL],
]

print(f"Plotting RQ1 for {NAM}")
plot_all(
    all_data,
    baseline,
    comparisons,
    "rq1_nam_2q",
    NAM,
    "2q",
)

# %% [markdown]
# # RQ2

# %%
guoq_rewrite = import_data(f"fresh_results/rq2/guoq-rewrite")
add_gate_set_column_us(guoq_rewrite)
add_method_column_us(guoq_rewrite)

guoq_resynth = import_data(f"fresh_results/rq2/guoq-resynth")
add_gate_set_column_us(guoq_resynth)
add_method_column_us(guoq_resynth)

guoq = import_data(f"fresh_results/rq2/guoq")
add_gate_set_column_us(guoq)
add_method_column_us(guoq)

# %%
guoq_rewrite_paper = pd.read_csv("paper_results/rq2_guoq_rewrite.csv")
guoq_resynth_paper = pd.read_csv("paper_results/rq2_guoq_resynth.csv")
guoq_paper = pd.read_csv("paper_results/rq2_guoq.csv")

# %%
guoq_rewrite = fill_missing_data(guoq_rewrite, guoq_rewrite_paper, total_trials=10)
guoq_resynth = fill_missing_data(guoq_resynth, guoq_resynth_paper, total_trials=10)
guoq_resynth.replace(
    {
        "guoq BEAM_MCMC TWO_Q BQSKITorSYNTHETIQ ibm 10.0 0.0 0.0 -1 -1 1 False": US_RESYNTH
    },
    inplace=True,
)
guoq = fill_missing_data(guoq, guoq_paper, total_trials=10)

# %%
original = guoq[["circuit_id", "original_2q"]].drop_duplicates()
guoq_data = pd.concat([guoq, guoq_rewrite, guoq_resynth])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC TWO_Q BQSKIT ibm 3600 10.0 0.0 0.0 1 10 1 False": US,
        "guoq BEAM_MCMC TWO_Q BQSKITorSYNTHETIQ ibm 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM_MCMC TWO_Q BQSKIT empty 3600 0.0 0.0 0.0 1 10 1 False": US_RESYNTH,
        "guoq BEAM_MCMC TWO_Q NONE ibm 3600 0.0 0.0 0.0 1 10 1 False": US_REWRITE,
        "guoq BEAM_MCMC TWO_Q NONE ibm 10.0 0.0 0.0 -1 -1 1 False": US_REWRITE,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [US_REWRITE, US_RESYNTH],
]
print(f"Plotting RQ2 Fig 10")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig10_rq2",
    "",
    "2q",
)

# %% [markdown]
# # RQ3

# %%
guoq_beam = import_data(f"fresh_results/rq3/guoq-beam")
add_gate_set_column_us(guoq_beam)
add_method_column_us(guoq_beam)

guoq_rewrite_resynth = import_data(f"fresh_results/rq3/guoq-rewrite-resynth")
add_gate_set_column_us(guoq_rewrite_resynth)
add_method_column_us(guoq_rewrite_resynth)

guoq_resynth_rewrite = import_data(f"fresh_results/rq3/guoq-resynth-rewrite")
add_gate_set_column_us(guoq_resynth_rewrite)
add_method_column_us(guoq_resynth_rewrite)

# %%
guoq_beam_paper = pd.read_csv("paper_results/rq3_guoq_beam.csv")
guoq_rewrite_resynth_paper = pd.read_csv("paper_results/rq3_guoq_rewrite_resynth.csv")
guoq_resynth_rewrite_paper = pd.read_csv("paper_results/rq3_guoq_resynth_rewrite.csv")

# %%
guoq_beam = fill_missing_data(guoq_beam, guoq_beam_paper, total_trials=10)
guoq_rewrite_resynth = fill_missing_data(
    guoq_rewrite_resynth, guoq_rewrite_resynth_paper, total_trials=10
)
guoq_rewrite_resynth.replace(
    {
        "guoq BEAM_MCMC TWO_Q BQSKITorSYNTHETIQ ibm 10.0 0.0 0.0 -1 -1 1 False": US_RR_RESYNTH
    },
    inplace=True,
)
guoq_resynth_rewrite = fill_missing_data(
    guoq_resynth_rewrite, guoq_resynth_rewrite_paper, total_trials=10
)

# %%
original = guoq[["circuit_id", "original_2q"]].drop_duplicates()
guoq_resynth_rewrite["original_2q"] = guoq_resynth_rewrite.apply(
    lambda x: original.loc[original["circuit_id"] == x["circuit_id"]][
        "original_2q"
    ].values[0],
    axis=1,
)
guoq_rewrite_resynth["original_2q"] = guoq_rewrite_resynth.apply(
    lambda x: original.loc[original["circuit_id"] == x["circuit_id"]][
        "original_2q"
    ].values[0],
    axis=1,
)
guoq_data = pd.concat([guoq, guoq_beam, guoq_resynth_rewrite, guoq_rewrite_resynth])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC TWO_Q BQSKIT ibm 3600 10.0 0.0 0.0 1 10 1 False": US,
        "guoq BEAM_MCMC TWO_Q BQSKITorSYNTHETIQ ibm 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM_MCMC TWO_Q NONE ibm 1800 10.0 0.0 0.0 1 10 1 False": US_RESYNTH_RR,
        "guoq BEAM_MCMC TWO_Q NONE ibm 10.0 0.0 0.0 -1 -1 1 False": US_RESYNTH_RR,
        "guoq BEAM_MCMC TWO_Q BQSKIT empty 1800 10.0 0.0 0.0 1 10 1 False": US_RR_RESYNTH,
        "guoq BEAM TWO_Q BQSKIT ibm 3600 0.0 0.0 0.0 -1 -1 8000 False": US_BEAM,
        "guoq BEAM TWO_Q BQSKITorSYNTHETIQ ibm 0.0 0.0 0.0 -1 -1 8000 False": US_BEAM,
    },
    inplace=True,
)
all_data = filter(all_data)

TITLE_YAXIS_FONT = 34

baseline = US
comparisons = [[US_RR_RESYNTH, US_RESYNTH_RR, US_BEAM]]
print(f"Plotting RQ3 Fig 11")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig11_rq3",
    "",
    "2q",
)

TITLE_YAXIS_FONT = 40

# %% [markdown]
# # RQ4

# %%
pyzx = import_data(f"fresh_results/rq4/pyzx")
qiskit = import_data(f"fresh_results/rq4/qiskit")
bqskit = import_data(f"fresh_results/rq4/bqskit")
synthetiq = import_data(f"fresh_results/rq4/synthetiq")

queso = import_data(f"fresh_results/rq4/queso")
add_gate_set_column_us(queso)
add_method_column_us(queso)

guoq = import_data(f"fresh_results/rq4/guoq")
add_gate_set_column_us(guoq)
add_method_column_us(guoq)

guoq_rewrite = import_data(f"fresh_results/rq4/guoq-rewrite")
add_gate_set_column_us(guoq_rewrite)
add_method_column_us(guoq_rewrite)

guoq_resynth = import_data(f"fresh_results/rq4/guoq-resynth")
add_gate_set_column_us(guoq_resynth)
add_method_column_us(guoq_resynth)

# %%
pyzx_paper = pd.read_csv("paper_results/rq4_pyzx.csv")
qiskit_paper = pd.read_csv("paper_results/rq4_qiskit.csv")
bqskit_paper = pd.read_csv("paper_results/rq4_bqskit.csv")
synthetiq_paper = pd.read_csv("paper_results/rq4_synthetiq.csv")
queso_paper = pd.read_csv("paper_results/rq4_queso.csv")
guoq_paper = pd.read_csv("paper_results/rq4_guoq.csv")
guoq_rewrite_paper = pd.read_csv("paper_results/rq4_guoq_rewrite.csv")
guoq_resynth_paper = pd.read_csv("paper_results/rq4_guoq_resynth.csv")

# %%
pyzx = fill_missing_data(pyzx, pyzx_paper, circuits_ran=circuits_ran)
qiskit = fill_missing_data(qiskit, qiskit_paper, circuits_ran=circuits_ran)
bqskit = fill_missing_data(bqskit, bqskit_paper, circuits_ran=circuits_ran)
synthetiq = fill_missing_data(synthetiq, synthetiq_paper, circuits_ran=circuits_ran)
queso = fill_missing_data(queso, queso_paper, circuits_ran=circuits_ran)
guoq = fill_missing_data(guoq, guoq_paper, total_trials=10)
guoq_rewrite = fill_missing_data(guoq_rewrite, guoq_rewrite_paper, total_trials=10)
guoq_resynth = fill_missing_data(guoq_resynth, guoq_resynth_paper, total_trials=10)
guoq_resynth.replace(
    {
        "guoq BEAM_MCMC FT BQSKITorSYNTHETIQ nam_t_tdg 10.0 0.0 0.0 -1 -1 1 False": US_RESYNTH
    },
    inplace=True,
)

# %%
original = guoq[["circuit_id", "original_2q", "original_t"]].drop_duplicates()
guoq_data = pd.concat([guoq, guoq_rewrite, guoq_resynth])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
        "original_t",
        "best_size_t",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other2(queso, "queso", original, "2q", "t"),
        process_other2(pyzx, "pyzx", original, "2q", "t"),
        process_other2(qiskit, "qiskit", original, "2q", "t"),
        process_other2(bqskit, "bqskit", original, "2q", "t"),
        process_other2(synthetiq, "synthetiq", original, "2q", "t"),
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC FT SYNTHETIQ cliffordt 3600 10.0 0.0 0.0 1 622 1 False": US,
        "guoq BEAM_MCMC FT BQSKITorSYNTHETIQ nam_t_tdg 10.0 0.0 0.0 -1 -1 1 False": US,
        "guoq BEAM_MCMC FT SYNTHETIQ empty 3600 10.0 0.0 0.0 1 622 1 False": US_RESYNTH,
        "guoq BEAM_MCMC FT NONE cliffordt 3600 10.0 0.0 0.0 1 622 1 False": US_REWRITE,
        "guoq BEAM_MCMC FT NONE nam_t_tdg 10.0 0.0 0.0 -1 -1 1 False": US_REWRITE,
        "guoq BEAM TOTAL NONE cliffordt 3600 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "guoq BEAM TOTAL NONE nam_t_tdg 0.0 0.0 0.0 -1 -1 8000 False": QUESO,
        "queso": QUESO,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "pyzx": PYZX,
        "synthetiq": SYNTHETIQ,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [
    [QISKIT, BQSKIT, SYNTHETIQ, QUESO, PYZX],
]
print(f"Plotting RQ4 Fig 12")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig12_rq4_t",
    "Clifford + T",
    "t",
    ylabel="T",
)
plot_all(
    all_data, baseline, comparisons, "fig12_rq4_2q", "Clifford + T", "2q", legend=False
)

baseline = US
comparisons = [
    [US_REWRITE, US_RESYNTH],
]
print(f"Plotting RQ4 Fig 13")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig13_rq4_rq2_t",
    "Clifford + T",
    "t",
    ylabel="T",
)

# %%
guoq_after_pyzx = import_data(f"fresh_results/rq4/guoq-after-pyzx")
add_gate_set_column_us(guoq_after_pyzx)
add_method_column_us(guoq_after_pyzx)

guoq_after_pyzx_paper = pd.read_csv("paper_results/rq4_guoq_after_pyzx.csv")

guoq_after_pyzx = fill_missing_data(
    guoq_after_pyzx, guoq_after_pyzx_paper, total_trials=10
)

original = guoq_after_pyzx[
    ["circuit_id", "original_2q", "original_t"]
].drop_duplicates()
guoq_data = pd.concat([guoq_after_pyzx])
guoq_data = guoq_data[
    [
        "method",
        "circuit_id",
        "original_2q",
        "best_size_2q",
        "original_t",
        "best_size_t",
    ]
]

all_data = pd.concat(
    [
        guoq_data,
        process_other2(pyzx, "pyzx", original, "2q", "t"),
    ]
)

all_data.replace(
    {
        "guoq BEAM_MCMC FT SYNTHETIQ cliffordt 3600 10.0 0.0 0.0 1 622 1 False": US,
        "guoq BEAM_MCMC FT BQSKITorSYNTHETIQ nam_t_tdg 10.0 0.0 0.0 -1 -1 1 False": US,
        "queso": QUESO,
        "qiskit": QISKIT,
        "bqskit": BQSKIT,
        "pyzx": PYZX,
        "synthetiq": SYNTHETIQ,
    },
    inplace=True,
)
all_data = filter(all_data)

baseline = US
comparisons = [[PYZX]]
print(f"Plotting RQ4 Fig 14")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig14_rq4_after_pyzx_t",
    "Clifford + T",
    "t",
    ylabel="T",
)
print("RQ4 Summary Claim:")
plot_all(
    all_data,
    baseline,
    comparisons,
    "fig14_rq4_after_pyzx_2q",
    "Clifford + T",
    "2q",
    legend=False,
)

# %% [markdown]
# # Overview Figures

# %%
sns.set_context("notebook", font_scale=1.4)
print(f"Plotting Fig 1")
# better = [233, 217, 218, 215, 240, 237, 198]
# match = [6, 7, 5, 10, 7, 9, 12]
better = [
    qiskit_ibmo_better,
    tket_ibmo_better,
    voqc_ibmo_better,
    bqskit_ibmo_better,
    queso_ibmo_better,
    quartz_ibmo_better,
    quarl_ibmn_better,
]
match = [
    qiskit_ibmo_match,
    tket_ibmo_match,
    voqc_ibmo_match,
    bqskit_ibmo_match,
    queso_ibmo_match,
    quartz_ibmo_match,
    quarl_ibmn_match,
]
bar_data = {
    "category": [QISKIT, TKET, VOQC, BQSKIT, QUESO, QUARTZ, QUARL + "*"],
    "count": [x / 247 * 100 for x in better],
}
b1 = sns.barplot(
    data={
        "category": [QISKIT, TKET, VOQC, BQSKIT, QUESO, QUARTZ, QUARL + "*"],
        "count": [100] * 7,
    },
    y="category",
    x="count",
    palette=["#e7e7ebff"] * 7,
    hue="category",
    # y=[x / 250 * 100 for x in [233, 224, 224, 224, 234, 234, 185]],
    # color=sns.color_palette("colorblind")[1:],
)
b2 = sns.barplot(
    data={
        "category": [QISKIT, TKET, VOQC, BQSKIT, QUESO, QUARTZ, QUARL + "*"],
        "count": [x / 247 * 100 for x in [sum(x) for x in zip(better, match)]],
    },
    y="category",
    x="count",
    color=sns.color_palette("colorblind")[7],
    alpha=1.0,
    # y=[x / 250 * 100 for x in [233, 224, 224, 224, 234, 234, 185]],
    # color=sns.color_palette("colorblind")[1:],
)
b = sns.barplot(
    bar_data,
    y="category",
    x="count",
    color="#ccccffff",
    alpha=1.0,
    # color=sns.color_palette("colorblind")[7],
)
b.set_box_aspect(0.5)

for i in [b.containers[8]]:
    b.bar_label(i, label_type="edge", padding=-39, fmt="%.1f")

for p in b.patches[14:]:
    width = p.get_width()
    b.annotate(
        "GUOQ",
        xy=(0, p.get_y() + p.get_height() / 2),
        xytext=(5, 0),  # Offset the text by 5 units to the right
        textcoords="offset points",
        ha="left",
        va="center",
    )

b.yaxis.tick_right()
b.tick_params(axis="y", which="both", length=0)
b.set_title("GUOQ vs. State-of-the-Art Quantum Optimizers")
b.set_ylabel("")
b.set_xlabel("% benchmarks GUOQ better/match/worse (left to right)", fontsize=15)
sns.despine(left=True, bottom=True)
b.grid(False)
b.figure.set_size_inches(10, 3.81)
plt.savefig("graphs/fig1.pdf", bbox_inches="tight", pad_inches=0.1)
plt.close()

# %%
sns.set_context("notebook", font_scale=2)

print(f"Plotting Fig 7")

barenco_combined = "logs_for_overview/guoq_log_272536_4.out"
barenco_rewrite = "logs_for_overview/guoq_log_272576_4.out"
barenco_resynth = "logs_for_overview/guoq_log_272576_254.out"

if os.path.exists("logs_for_overview/guoq_log_barenco_10_combined.out"):
    barenco_combined = "logs_for_overview/guoq_log_barenco_10_combined.out"
if os.path.exists("logs_for_overview/guoq_log_barenco_10_rewrite.out"):
    barenco_rewrite = "logs_for_overview/guoq_log_barenco_10_rewrite.out"
if os.path.exists("logs_for_overview/guoq_log_barenco_10_resynth.out"):
    barenco_resynth = "logs_for_overview/guoq_log_barenco_10_resynth.out"


def parse_size_time_data(log_file):
    data_combined = []
    with open(log_file, "r") as f:
        lines = f.readlines()
        for line in lines:
            if "original" in line:
                data = json.loads(line)
                data["best_circuit_size"] = int(data["original_total"])
                data["best_size_2q"] = int(data["original_2q"])
                data["best_size_t"] = int(data["original_t"])
                data["seconds_elapsed"] = 0
                data_combined.append(data)
            if "time_to_best" in line:
                data = json.loads(line)
                data["best_circuit_size"] = int(data["best_circuit_size"])
                data["best_size_2q"] = int(data["best_size_2q"])
                data["best_size_t"] = int(data["best_size_t"])
                data["seconds_elapsed"] = float(data["seconds_elapsed"])
                if data["seconds_elapsed"] == 0:
                    data["seconds_elapsed"] = 0.5
                data_combined.append(data)
    last = dict(data_combined[-1])
    last["seconds_elapsed"] = 3600
    data_combined.append(last)
    return pd.DataFrame(data_combined)


combined = parse_size_time_data(barenco_combined)
rewrite_only = parse_size_time_data(barenco_rewrite)
resynth_only = parse_size_time_data(barenco_resynth)

sns.lineplot(
    combined,
    x="seconds_elapsed",
    y="best_size_2q",
    label="combined",
    linewidth=3,
)

b = sns.lineplot(
    rewrite_only,
    x="seconds_elapsed",
    y="best_size_2q",
    label="rewrite only",
    linestyle="-.",
    linewidth=3,
)
sns.lineplot(
    resynth_only,
    x="seconds_elapsed",
    y="best_size_2q",
    label="resynth only",
    linestyle="--",
    linewidth=3,
)

b.set(xlabel="time (s)", ylabel="2q gate count")

plt.legend(title="approach", title_fontsize="small", fontsize="small")
plt.title("barenco_tof_10 circuit")
plt.savefig("graphs/fig7_barenco.pdf", bbox_inches="tight", pad_inches=0.1)
# plt.show()
plt.close()


qft_combined = "logs_for_overview/guoq_log_272536_157.out"
qft_rewrite = "logs_for_overview/guoq_log_272576_157.out"
qft_resynth = "logs_for_overview/guoq_log_272576_407.out"

if os.path.exists("logs_for_overview/guoq_log_qft_20_combined.out"):
    qft_combined = "logs_for_overview/guoq_log_qft_20_combined.out"
if os.path.exists("logs_for_overview/guoq_log_qft_20_rewrite.out"):
    qft_rewrite = "logs_for_overview/guoq_log_qft_20_rewrite.out"
if os.path.exists("logs_for_overview/guoq_log_qft_20_resynth.out"):
    qft_resynth = "logs_for_overview/guoq_log_qft_20_resynth.out"

combined = parse_size_time_data(qft_combined)
rewrite_only = parse_size_time_data(qft_rewrite)
resynth_only = parse_size_time_data(qft_resynth)

sns.lineplot(
    combined,
    x="seconds_elapsed",
    y="best_size_2q",
    label="combined",
    linewidth=3,
)

b = sns.lineplot(
    rewrite_only,
    x="seconds_elapsed",
    y="best_size_2q",
    label="rewrite only",
    linestyle="-.",
    linewidth=3,
)
sns.lineplot(
    resynth_only,
    x="seconds_elapsed",
    y="best_size_2q",
    label="resynth only",
    linestyle="--",
    linewidth=3,
)

b.set(xlabel="time (s)", ylabel="2q gate count")

plt.title("qft_20 circuit")
plt.legend().remove()
plt.savefig("graphs/fig7_qft.pdf", bbox_inches="tight", pad_inches=0.1)
# plt.show()
plt.close()

# %%
sns.set_context("notebook", font_scale=2.5)

print(f"Processing data and plotting Fig 15")

for gate_set in ["ibmo", "ibmn", "ion", "nam", "cliffordt"]:
    directory = F"benchmarks/{GATE_SET_MAP[gate_set]}"
    data = []

    for filename in os.listdir(directory):
        if os.path.isfile(os.path.join(directory, filename)):
            circ = QuantumCircuit.from_qasm_file(os.path.join(directory, filename))
            data.append(
                {
                    "circuit": filename,
                    "num_qubits": circ.num_qubits,
                    "gate_count": circ.size(),
                    "depth": circ.depth(),
                }
            )

    df = pd.DataFrame(data)
    df.to_csv(f"circuit_statistics/{gate_set}_circuit_data.csv", index=False)

def plot_histogram(data, title, filename):
    g = sns.histplot(data, x="gate_count", bins=50, log_scale=True)
    g.set_xticks([1e2, 1e4, 1e6])
    plt.ylabel("benchmark count")
    plt.xlabel("total gate count")
    plt.title(title)
    plt.xlim(0, 2e6)
    plt.savefig(filename, bbox_inches="tight", pad_inches=0.1)
    plt.close()


data = pd.read_csv("circuit_statistics/nam_circuit_data.csv")
plot_histogram(data, "Nam", "graphs/fig15_nam_circuit_data.pdf")

data = pd.read_csv("circuit_statistics/ion_circuit_data.csv")
plot_histogram(data, "IONQ", "graphs/fig15_ionq_circuit_data.pdf")

data = pd.read_csv("circuit_statistics/ibmn_circuit_data.csv")
plot_histogram(data, "IBM-EAGLE", "graphs/fig15_ibm_eagle_circuit_data.pdf")

data = pd.read_csv("circuit_statistics/ibmo_circuit_data.csv")
plot_histogram(data, "IBMQ20", "graphs/fig15_ibmq20_circuit_data.pdf")

data = pd.read_csv("circuit_statistics/cliffordt_circuit_data.csv")
plot_histogram(data, "Clifford + T", "graphs/fig15_cliffordt_circuit_data.pdf")

print(f"Claim: Circuits act on {data["num_qubits"].min()} to {data["num_qubits"].max()} qubits.")


