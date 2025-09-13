import re
import csv
import sys
import statistics
import pprint
from collections import OrderedDict
import seaborn as sns
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import FormatStrFormatter
import numpy as np
import os
import math

sns.set_theme(palette="colorblind")
sns.set_style("ticks",{'font.family': 'serif', 'axes.grid' : True})

NAM = 'nam'
IBM = 'ibm'
RIGETTI = 'rigetti'
ION = 'ion'

US = 'QUESO'
US_PP = 'QUESO-PP'
QISKIT = 'Qiskit'
VOQC = 'VOQC'
TKET = 'TKET'
QUILC = 'Quilc'
NAM_TOOL = 'Nam'
QUARTZ = 'Quartz'
QUARTZ_NO_TD = 'Quartz-NoPP'
QUARTZ_NO_RO = 'Quartz-NoRO'
QUARTZ_NO_TDRO = 'Quartz-NoPP'

TYPE_ORDER = ["arithmetic", "qaoa", "toffoli"]
MARKERS = ["o", "s", "^"]

# https://ionq.com/posts/july-25-2022-ionq-aria-part-one-practical-performance
# Aria
ION_1Q_FIDELITY = 0.9995
ION_2Q_FIDELITY = 0.996

# https://www.rigetti.com/
# Aspen-11
RIGETTI_1Q_FIDELITY = 0.998
RIGETTI_2Q_FIDELITY = 0.902

# https://quantum-computing.ibm.com/services/resources?system=ibmq_toronto
# toronto
IBM_1Q_FIDELITY = 0.999606
IBM_2Q_FIDELITY = 0.98719

FIDELITY_COLOR = "#e7e7eb"
TOTAL_COLOR = "#fffbdd"

PP_NAME = "afterQuartzPP_"

benchmarks = [
  "adder_8",
  "barenco_tof_3",
  "barenco_tof_4",
  "barenco_tof_5",
  "barenco_tof_10",
  "csla_mux_3",
  "csum_mux_9",
  "gf2^4_mult",
  "gf2^5_mult",
  "gf2^6_mult",
  "gf2^7_mult",
  "gf2^8_mult",
  "gf2^9_mult",
  "gf2^10_mult",
  "mod5_4",
  "mod_mult_55",
  "mod_red_21",
  "qcla_adder_10",
  "qcla_com_7",
  "qcla_mod_7",
  "rc_adder_6",
  "tof_3",
  "tof_4",
  "tof_5",
  "tof_10",
  "vbe_adder_3",
  "qaoa_n4_p4",
  "qaoa_n6_p4",
  "qaoa_n8_p4",
  "qaoa_n10_p4",
  "qaoa_n14_p4",
  "qaoa_n20_p4",
  "qaoa_n30_p4",
]

benchmark_types = {
  "adder_8":"arithmetic",
  "barenco_tof_3":"toffoli",
  "barenco_tof_4":"toffoli",
  "barenco_tof_5":"toffoli",
  "barenco_tof_10":"toffoli",
  "csla_mux_3":"arithmetic",
  "csum_mux_9":"arithmetic",
  "gf2^4_mult":"arithmetic",
  "gf2^5_mult":"arithmetic",
  "gf2^6_mult":"arithmetic",
  "gf2^7_mult":"arithmetic",
  "gf2^8_mult":"arithmetic",
  "gf2^9_mult":"arithmetic",
  "gf2^10_mult":"arithmetic",
  "mod5_4":"arithmetic",
  "mod_mult_55":"arithmetic",
  "mod_red_21":"arithmetic",
  "qcla_adder_10":"arithmetic",
  "qcla_com_7":"arithmetic",
  "qcla_mod_7":"arithmetic",
  "rc_adder_6":"arithmetic",
  "tof_3":"toffoli",
  "tof_4":"toffoli",
  "tof_5":"toffoli",
  "tof_10":"toffoli",
  "vbe_adder_3":"arithmetic",
  "qaoa_n4_p4":"qaoa",
  "qaoa_n6_p4":"qaoa",
  "qaoa_n8_p4":"qaoa",
  "qaoa_n10_p4":"qaoa",
  "qaoa_n14_p4":"qaoa",
  "qaoa_n20_p4":"qaoa",
  "qaoa_n30_p4":"qaoa",
}

original_total = {
  "adder_8": 900,
  "barenco_tof_3": 58,
  "barenco_tof_4": 114,
  "barenco_tof_5": 170,
  "barenco_tof_10": 450,
  "csla_mux_3": 170,
  "csum_mux_9": 420,
  "gf2^4_mult": 225,
  "gf2^5_mult": 347,
  "gf2^6_mult": 495,
  "gf2^7_mult": 669,
  "gf2^8_mult": 883,
  "gf2^9_mult": 1095,
  "gf2^10_mult": 1347,
  "mod5_4": 63,
  "mod_mult_55": 119,
  "mod_red_21": 278,
  "qcla_adder_10": 521,
  "qcla_com_7": 443,
  "qcla_mod_7": 884,
  "rc_adder_6": 200,
  "tof_3": 45,
  "tof_4": 75,
  "tof_5": 105,
  "tof_10": 255,
  "vbe_adder_3": 150,
  "qaoa_n4_p4": 220,
  "qaoa_n6_p4": 330,
  "qaoa_n8_p4": 440,
  "qaoa_n10_p4": 550,
  "qaoa_n14_p4": 770,
  "qaoa_n20_p4": 1100,
  "qaoa_n30_p4": 1650,
}

original_rigetti_total = {
  "adder_8": 4412,
  "barenco_tof_3": 268,
  "barenco_tof_4": 528,
  "barenco_tof_5": 788,
  "barenco_tof_10": 2088,
  "csla_mux_3": 870,
  "csum_mux_9": 1848,
  "gf2^4_mult": 1059,
  "gf2^5_mult": 1633,
  "gf2^6_mult": 2329,
  "gf2^7_mult": 3147,
  "gf2^8_mult": 4213,
  "gf2^9_mult": 5149,
  "gf2^10_mult": 6333,
  "mod5_4": 305,
  "mod_mult_55": 545,
  "mod_red_21": 1208,
  "qcla_adder_10": 2535,
  "qcla_com_7": 2048,
  "qcla_mod_7": 4186,
  "rc_adder_6": 1010,
  "tof_3": 207,
  "tof_4": 345,
  "tof_5": 483,
  "tof_10": 1173,
  "vbe_adder_3": 740,
  "qaoa_n4_p4": 712,
  "qaoa_n6_p4": 1068,
  "qaoa_n8_p4": 1424,
  "qaoa_n10_p4": 1780,
  "qaoa_n14_p4": 2492,
  "qaoa_n20_p4": 3560,
  "qaoa_n30_p4": 5340,
}

original_ion_total = {
  "adder_8": 2616,
  "barenco_tof_3": 160,
  "barenco_tof_4": 316,
  "barenco_tof_5": 472,
  "barenco_tof_10": 1252,
  "csla_mux_3": 510,
  "csum_mux_9": 1120,
  "gf2^4_mult": 635,
  "gf2^5_mult": 981,
  "gf2^6_mult": 1401,
  "gf2^7_mult": 1895,
  "gf2^8_mult": 2533,
  "gf2^9_mult": 3105,
  "gf2^10_mult": 3821,
  "mod5_4": 181,
  "mod_mult_55": 325,
  "mod_red_21": 728,
  "qcla_adder_10": 1503,
  "qcla_com_7": 1226,
  "qcla_mod_7": 2494,
  "rc_adder_6": 594,
  "tof_3": 123,
  "tof_4": 205,
  "tof_5": 287,
  "tof_10": 697,
  "vbe_adder_3": 440,
  "qaoa_n4_p4": 448,
  "qaoa_n6_p4": 672,
  "qaoa_n8_p4": 896,
  "qaoa_n10_p4": 1120,
  "qaoa_n14_p4": 1568,
  "qaoa_n20_p4": 2240,
  "qaoa_n30_p4": 3360,
}

original_2q = {
  "adder_8": 409,
  "barenco_tof_3": 24,
  "barenco_tof_4": 48,
  "barenco_tof_5": 72,
  "barenco_tof_10": 192,
  "csla_mux_3": 80,
  "csum_mux_9": 168,
  "gf2^4_mult": 99,
  "gf2^5_mult": 154,
  "gf2^6_mult": 221,
  "gf2^7_mult": 300,
  "gf2^8_mult": 405,
  "gf2^9_mult": 494,
  "gf2^10_mult": 609,
  "mod5_4": 28,
  "mod_mult_55": 48,
  "mod_red_21": 105,
  "qcla_adder_10": 233,
  "qcla_com_7": 186,
  "qcla_mod_7": 382,
  "rc_adder_6": 93,
  "tof_3": 18,
  "tof_4": 30,
  "tof_5": 42,
  "tof_10": 102,
  "vbe_adder_3": 70,
  "qaoa_n4_p4": 48,
  "qaoa_n6_p4": 72,
  "qaoa_n8_p4": 96,
  "qaoa_n10_p4": 120,
  "qaoa_n14_p4": 168,
  "qaoa_n20_p4": 240,
  "qaoa_n30_p4": 360,
}

nam_results_total = {
  "adder_8": 606,
  "barenco_tof_3": 40,
  "barenco_tof_4": 72,
  "barenco_tof_5": 104,
  "barenco_tof_10": 264,
  "csla_mux_3": 155,
  "csum_mux_9": 266,
  "gf2^4_mult": 187,
  "gf2^5_mult": 296,
  "gf2^6_mult": 403,
  "gf2^7_mult": 555,
  "gf2^8_mult": 712,
  "gf2^9_mult": 891,
  "gf2^10_mult": 1070,
  "mod5_4": 51,
  "mod_mult_55": 91,
  "mod_red_21": 180,
  "qcla_adder_10": 399,
  "qcla_com_7": 284,
  "qcla_mod_7": 0,
  "rc_adder_6": 140,
  "tof_3": 35,
  "tof_4": 55,
  "tof_5": 75,
  "tof_10": 175,
  "vbe_adder_3": 89,
  "qaoa_n4_p4": 0,
  "qaoa_n6_p4": 0,
  "qaoa_n8_p4": 0,
  "qaoa_n10_p4": 0,
  "qaoa_n14_p4": 0,
  "qaoa_n20_p4": 0,
  "qaoa_n30_p4": 0,
}

nam_results_2q = {
  "adder_8": 291,
  "barenco_tof_3": 18,
  "barenco_tof_4": 34,
  "barenco_tof_5": 50,
  "barenco_tof_10": 130,
  "csla_mux_3": 70,
  "csum_mux_9": 140,
  "gf2^4_mult": 99,
  "gf2^5_mult": 154,
  "gf2^6_mult": 221,
  "gf2^7_mult": 300,
  "gf2^8_mult": 405,
  "gf2^9_mult": 494,
  "gf2^10_mult": 609,
  "mod5_4": 28,
  "mod_mult_55": 40,
  "mod_red_21": 77,
  "qcla_adder_10": 183,
  "qcla_com_7": 132,
  "qcla_mod_7": 0,
  "rc_adder_6": 71,
  "tof_3": 14,
  "tof_4": 22,
  "tof_5": 30,
  "tof_10": 70,
  "vbe_adder_3": 50,
  "qaoa_n4_p4": 0,
  "qaoa_n6_p4": 0,
  "qaoa_n8_p4": 0,
  "qaoa_n10_p4": 0,
  "qaoa_n14_p4": 0,
  "qaoa_n20_p4": 0,
  "qaoa_n30_p4": 0,
}

times_dict = {
  "adder_8": (254, 23),
  "barenco_tof_3": (252, 4),
  # (337, 5),
  # (357, 8),
  "barenco_tof_10": (864, 15),
  "csla_mux_3": (31, 0.5),
  "csum_mux_9": (0.5, 0.5),
  "gf2^4_mult": (153, 7),
  # (459, 6),
  # (681, 18),
  # (2243, 35),
  # (1767, 60),
  # (2195, 53),
  "gf2^10_mult": (3183, 84),
  "mod5_4": (3, 0.5),
  "mod_mult_55": (23, 1),
  "mod_red_21": (336, 8),
  "qcla_adder_10": (52, 7),
  "qcla_com_7": (32, 4),
  "qcla_mod_7": (61, 38),
  "rc_adder_6": (89, 1),
  "tof_3": (17, 0.5),
  # (5, 0.5),
  # (2, 0.5),
  "tof_10": (8, 1),
  "vbe_adder_3": (711, 11),
  "qaoa_n4_p4": (192, 5),
  # (54, 3),
  # (12, 1),
  "qaoa_n10_p4": (2255, 10),
  # (158, 4),
  "qaoa_n20_p4": (281, 7),
  "qaoa_n30_p4": (0.5, 0.5)
}

def parse_into_csv(output_dir, gate_count_file, two_qubit_only):
  fieldnames = ['benchmark']
  rows_dict = {}
  with open(gate_count_file) as f:
    lines = f.read().splitlines()
    for line in lines:
      benchmark = parse_benchmark_name(line)
      gate_count = parse_gate_count(line) if two_qubit_only else line[line.find("Final gate count: ")+len("Final gate count: "):line.find(",", line.find("Final gate count: "))]
      header = parse_header_name(line)
      if benchmark in rows_dict:
        rows_dict[benchmark][header] = gate_count
      else:
        rows_dict[benchmark] = {'benchmark':benchmark, header:gate_count}
      if header not in fieldnames:
        fieldnames.append(header)

  filename = output_dir+'/queso_results%s.csv' % ("_2q" if two_qubit_only else "")
  fieldnames.sort(reverse=True)
  with open(filename, 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows_dict.values())

def parse_benchmark_name(line):
  return line[line.find(":")+1:line.find(".qasm")].replace("decomp0_", "")

def parse_gateset(line):
  if NAM in line:
    return NAM
  if IBM in line:
    return IBM
  if RIGETTI in line:
    return RIGETTI
  if ION in line:
    return ION

def parse_gate_count(line):
  return line[line.rfind(" ")+1:]

def parse_header_name(line):
  line = line[line.find("queso_")+1:]
  return line[line.find("queso_"):line.find(".txt")]

def find_missing_data(filename):
  with open(filename, 'r') as data:
    for line in csv.DictReader(data):
      for key in line.keys():
        if line[key] == '':
          print(key + " " + line['benchmark'])


def parse_other_into_csv(output_dir, file, gateset, two_qubit_only, adnl_csv_name=""):
  fieldnames = ['benchmark', 'original']
  rows_dict = {}
  num_q = "2q" if two_qubit_only else "1q" 
  with open(file) as f:
    lines = f.read().splitlines()
    for line in lines:
      if num_q in line:
        benchmark = line[line.rfind("/")+1:line.find(".qasm")].replace("decomp0_", "")
        gate_count = parse_gate_count(line)
        if 'original' in line:
          if benchmark in rows_dict:
            rows_dict[benchmark]['original'] = gate_count
          else:
            rows_dict[benchmark] = {'benchmark':benchmark,'original':gate_count}
        if gateset in line:
          header = line[line.find(".qasm")+6:line.rfind(gateset)-1]
          if benchmark in rows_dict:
            rows_dict[benchmark][header] = gate_count
          else:
            rows_dict[benchmark] = {'benchmark':benchmark,header:gate_count}
          if header not in fieldnames:
            fieldnames.append(header)
  filename = output_dir+'/other_tools_results%s_%s%s.csv' % ("_2q" if two_qubit_only else "", gateset, "_"+adnl_csv_name if adnl_csv_name != "" else "")
  with open(filename, 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows_dict.values())

def parse_quartz_csv(output_dir, filename, gateset, adnl_csv_name=""):
  fieldnames = ['benchmark']
  tracker_dict = {}
  rows_dict = {}
  two_qubit_only = "2q" in filename
  mod5_4_nam_avg = 0
  decomp0_mod5_4_nam_avg = 0
  with open(filename) as f:
    lines = f.read().splitlines()
    for line in lines:
      if two_qubit_only:
        if "."+gateset in line:
          benchmark = line[line.find("/", line.find("optimized_quartz")+1)+1:line.find(".qasm")]
          benchmark = benchmark.replace("decomp0_", "")
          gate_count = line[line.find("."+gateset)+len(gateset)+1:line.rfind(".")]
          gate_count = sys.maxsize if gate_count == '' else int(gate_count)
          gate_count_2q = parse_gate_count(line)
          header = "nopp" if "decomp0" in line else "pp"
          if benchmark in tracker_dict:
            if header in tracker_dict[benchmark]:
              if gate_count < tracker_dict[benchmark][header]:
                tracker_dict[benchmark][header] = gate_count
                rows_dict[benchmark][header] = gate_count_2q
            else:
              tracker_dict[benchmark][header] = gate_count
              rows_dict[benchmark][header] = gate_count_2q
          else:
            tracker_dict[benchmark] = {'benchmark':benchmark,header:gate_count}
            rows_dict[benchmark] = {'benchmark':benchmark,header:gate_count_2q}
          if header not in fieldnames:
            fieldnames.append(header)
      else:
        gate_count = parse_gate_count(line)
        gate_count = gate_count.replace(".00", "")
        gate_count = int(gate_count)
        if gateset.capitalize() in line or (gateset == 'ibmq' and 'IBM_' in line):
          header = "nopp" if "decomp0" in line else "pp"
          benchmark = line[line.find(":")+1:line.find(".qasm:")]
          benchmark = benchmark.replace("decomp0_", "")
          if benchmark in rows_dict:
            rows_dict[benchmark][header] = gate_count
          else:
            rows_dict[benchmark] = {'benchmark':benchmark,header:gate_count}
          if header not in fieldnames:
            fieldnames.append(header)
  filename = output_dir+'/quartz_results%s_%s%s.csv' % ("_2q" if two_qubit_only else "", gateset, "_"+adnl_csv_name if adnl_csv_name != "" else "")
  with open(filename, 'w', encoding='UTF8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows_dict.values())

def optimized_ratio(benchmark, gateset, two_qubit_only, gate_count):
  gate_count = int(gate_count)
  if two_qubit_only:
    return gate_count/original_2q[benchmark]
  else:
    if gateset == RIGETTI:
      return gate_count/original_rigetti_total[benchmark]
    else:
      return gate_count/original_total[benchmark]

def read_csv(csv_file):
  dicts = []
  with open(csv_file, 'r') as data:
    for line in csv.DictReader(data):
      dicts.append(line)
  config_to_gate_counts = {}
  for b in dicts:
    for (config, gate_count) in b.items():
      if config == 'benchmark':
        continue
      if config in config_to_gate_counts:
        config_to_gate_counts[config][b['benchmark']] = None if gate_count == "" else int(gate_count)
      else:
        config_to_gate_counts[config] = {b['benchmark']: None if gate_count == "" else int(gate_count)}
  return config_to_gate_counts

def get_geometric_mean_size(csv_file):
  config_to_gate_counts = read_csv(csv_file)
  gateset = parse_gateset(csv_file)
  two_qubit_only = "2q" in csv_file
  result = {}
  for (config, gate_counts) in config_to_gate_counts.items():
    ratios = []
    for (benchmark, gate_count) in gate_counts.items():
      ratios.append(optimized_ratio(benchmark, gateset, two_qubit_only, gate_count))
    result[config] = statistics.geometric_mean(ratios)
  return OrderedDict(sorted(result.items(), key = lambda x: x[1]))

def get_ordered_ratios(gate_counts_dict, gateset, two_qubit_only, benchmarks):
  ratios = {}
  for (benchmark, gate_count) in gate_counts_dict.items():
    ratios[benchmark] = 1 - optimized_ratio(benchmark, gateset, two_qubit_only, gate_count)
  ordered_ratios = []
  for benchmark in benchmarks:
    ordered_ratios.append(ratios[benchmark])
  return ordered_ratios

def autolabel(rects, fmt='.2f'):
  # attach some text labels
  for rect in rects:
    height = rect.get_height()
    rect.set_height(0)
    rect.axes.annotate("N/A",
                      xy=(rect.get_x()+rect.get_width()/2., 0),
                      xytext=(0, 3), textcoords='offset points',
                      ha='center', va='bottom',fontsize=9,weight='bold')

def plot_us_vs_other_tools_s_curve(all_data, gateset, two_qubit_only, ax, last_index, show_num_benchmarks=True, show_total_benchmarks=False):
  ratios_us = all_data[0][1]
  d = {}
  outperform_d = {}
  match_d = {}
  for (method, gate_counts_dict) in all_data[1:]:
    d[method] = {}
    ratios = gate_counts_dict
    outperform = 0
    match = 0
    for b in ratios.keys():
      if "qv" in b: continue
      original = original_total
      if gateset == RIGETTI:
        original = original_rigetti_total
      if gateset == ION:
        original = original_ion_total
      if ratios_us[b.replace(gateset+"_", "")] <= ratios[b]:
        if ratios_us[b.replace(gateset+"_", "")] == ratios[b]:
          match += 1
        else:
          outperform+=1
      d[method][b.replace(gateset+"_", "")] = (ratios[b]-ratios_us[b.replace(gateset+"_", "")])/(original_2q[b.replace(gateset+"_", "")] if two_qubit_only else original[b.replace(gateset+"_", "")])
    outperform_d[method] = outperform
    match_d[method] = match
  i = 0
  for tool in d.keys():
    data = pd.DataFrame({
      "program" : sorted(list(d[tool].keys()), key=d[tool].get), 
      "difference" : sorted(list(d[tool].values())),
      "circ_type": [benchmark_types[p] for p in sorted(list(d[tool].keys()), key=d[tool].get)]
      })
  
    points = sns.scatterplot(data=data,
    x = "program", y = "difference", edgecolor=None, ax=ax[i], hue="circ_type", legend=True if i == last_index else False, style="circ_type", markers=MARKERS, hue_order=TYPE_ORDER, style_order=TYPE_ORDER)
    points.grid(False, axis="x")
    points.set( xlabel = None, xticklabels=[])
    ax[i].tick_params(axis='x', bottom=False)

    x = np.arange(0, len(d[tool].keys()), 1)
    ax[i].plot(x,[0]*len(x), "black", linestyle="dashed")
    # plt.legend()
    # ax[i].tight_layout()
    ax[i].yaxis.set_major_formatter(FormatStrFormatter('%.2f'))
    ax[i].set_box_aspect(1)
    tl = ((ax[i].get_xlim()[1] - ax[i].get_xlim()[0]) + ax[i].get_xlim()[0] + 0.5,
      (ax[i].get_ylim()[1] - ax[i].get_ylim()[0])*0.90 + ax[i].get_ylim()[0])
    if not two_qubit_only:
      ax[i].set_facecolor(TOTAL_COLOR)
    if show_num_benchmarks:
      total_benchmarks = '/33' if show_total_benchmarks and i == 0 else ''
      ax[i].text(tl[0], 0, f'{outperform_d[tool]}{total_benchmarks}\n{match_d[tool]}{total_benchmarks}\n{33-match_d[tool]-outperform_d[tool]}{total_benchmarks}', weight='bold', va='center')
    points.set(title=f"{tool} ({gateset.upper() if gateset == IBM else gateset.capitalize()})")
    if i == 0:
      prefix = '2q' if two_qubit_only else 'total'
      points.set(ylabel=r"$\bf{" + prefix + "}$" + " gate reduction")
    else:
      ax[i].axes.get_yaxis().get_label().set_visible(False)
    yabs_max = abs(max(ax[i].get_ylim(), key=abs))
    ax[i].set_ylim(ymin=-yabs_max, ymax=yabs_max)
    # points.figure.savefig(f"plots/rq1_{tool}_scurve_{'2q_' if two_qubit_only else ''}{gateset}.pdf", bbox_inches='tight', pad_inches=0.01) 
    # ax[i].close()
    i += 1

def plot_us_vs_other_tool_s_curve(all_data, gateset, ax, show_total_benchmarks=False):
  ratios_us_2q = all_data[0][1]
  ratios_us = all_data[1][1]

  original = original_total
  if gateset == RIGETTI:
    original = original_rigetti_total
  if gateset == ION:
    original = original_ion_total

  d = {}
  outperform_d = {}
  match_d = {}
  for (method, gate_counts_dict) in all_data[2:]:
    two_qubit_only = "2q" in method      
    d[method] = {}
    ratios = gate_counts_dict
    outperform = 0
    match = 0
    for b in ratios.keys():
      if "qv" in b: continue
      if two_qubit_only:
        if ratios_us_2q[b.replace(gateset+"_", "")] <= ratios[b]:
          if ratios_us_2q[b.replace(gateset+"_", "")] == ratios[b]:
            match += 1
          else:
            outperform+=1
        d[method][b.replace(gateset+"_", "")] = (ratios[b]-ratios_us_2q[b.replace(gateset+"_", "")])/(original_2q[b.replace(gateset+"_", "")] if two_qubit_only else original[b.replace(gateset+"_", "")])
      else:
        if ratios_us[b.replace(gateset+"_", "")] <= ratios[b]:
          if ratios_us[b.replace(gateset+"_", "")] == ratios[b]:
            match += 1
          else:
            outperform+=1
        d[method][b.replace(gateset+"_", "")] = (ratios[b]-ratios_us[b.replace(gateset+"_", "")])/(original_2q[b.replace(gateset+"_", "")] if two_qubit_only else original[b.replace(gateset+"_", "")])
    outperform_d[method] = outperform
    match_d[method] = match
  i = 0
  for tool in d.keys():
    two_qubit_only = "2q" in tool
    data = pd.DataFrame({
      "program" : sorted(list(d[tool].keys()), key=d[tool].get), 
      "difference" : sorted(list(d[tool].values())),
      "circ_type": [benchmark_types[p] for p in sorted(list(d[tool].keys()), key=d[tool].get)]
      })
  
    points = sns.scatterplot(data=data,
    x = "program", y = "difference", edgecolor=None, ax=ax[i], hue="circ_type", legend=True if i == 0 else False, style="circ_type", markers=MARKERS, hue_order=TYPE_ORDER, style_order=TYPE_ORDER)
    points.grid(False, axis="x")
    points.set( xlabel = None, xticklabels=[])
    ax[i].tick_params(axis='x', bottom=False)

    x = np.arange(0, len(d[tool].keys()), 1)
    ax[i].plot(x,[0]*len(x), "black", linestyle="dashed")
    # plt.legend()
    # ax[i].tight_layout()
    ax[i].yaxis.set_major_formatter(FormatStrFormatter('%.2f'))
    ax[i].set_box_aspect(1)
    tl = ((ax[i].get_xlim()[1] - ax[i].get_xlim()[0]) + ax[i].get_xlim()[0] + 0.5,
      (ax[i].get_ylim()[1] - ax[i].get_ylim()[0])*0.90 + ax[i].get_ylim()[0])
    total_benchmarks = '/33' if show_total_benchmarks and i == 0 else ''
    if not two_qubit_only:
      ax[i].set_facecolor(TOTAL_COLOR)
    ax[i].text(tl[0], 0, f'{outperform_d[tool]}{total_benchmarks}\n{match_d[tool]}{total_benchmarks}\n{33-match_d[tool]-outperform_d[tool]}{total_benchmarks}', weight='bold', va='center')
    points.set(title=f"{tool.replace('2q','')} ({gateset.upper() if gateset == IBM else gateset.capitalize()})")
    prefix = '2q' if two_qubit_only else 'total'
    points.set(ylabel=r"$\bf{" + prefix + "}$" + " gate reduction")
    yabs_max = abs(max(ax[i].get_ylim(), key=abs))
    ax[i].set_ylim(ymin=-yabs_max, ymax=yabs_max)
    # points.figure.savefig(f"plots/rq1_{tool}_scurve_{'2q_' if two_qubit_only else ''}{gateset}.pdf", bbox_inches='tight', pad_inches=0.01) 
    # ax[i].close()
    i += 1

def plot_us_vs_quartz_s_curve(all_data, gateset, two_qubit_only, ax, legend, title, show_num_benchmarks=True, show_total_benchmarks=False):
  ratios_us = all_data[0][1]

  original = original_total
  if gateset == RIGETTI:
    original = original_rigetti_total
  if gateset == ION:
    original = original_ion_total

  d = {}
  outperform_d = {}
  match_d = {}
  for (method, gate_counts_dict) in all_data[1:]:
    d[method] = {}
    ratios = gate_counts_dict
    outperform = 0
    match = 0
    for b in ratios.keys():
      if ratios_us[b.replace(gateset+"_", "")] <= ratios[b]:
        if ratios_us[b.replace(gateset+"_", "")] == ratios[b]:
          match += 1
        else:
          outperform+=1
      d[method][b.replace(gateset+"_", "")] = (ratios[b]-ratios_us[b.replace(gateset+"_", "")])/(original_2q[b.replace(gateset+"_", "")] if two_qubit_only else original[b.replace(gateset+"_", "")])
    outperform_d[method] = outperform
    match_d[method] = match
  i = 0
  for tool in d.keys():
    data = pd.DataFrame({
      "program" : sorted(list(d[tool].keys()), key=d[tool].get), 
      "difference" : sorted(list(d[tool].values())),
      "circ_type": [benchmark_types[p] for p in sorted(list(d[tool].keys()), key=d[tool].get)]
      })
  
    points = sns.scatterplot(data=data,
    x = "program", y = "difference", edgecolor=None, ax=ax, hue="circ_type", style="circ_type", markers=MARKERS, legend=legend, hue_order=TYPE_ORDER, style_order=TYPE_ORDER)
    points.grid(False, axis="x")
    points.set( xlabel = None, xticklabels=[])
    ax.tick_params(axis='x', bottom=False)

    x = np.arange(0, len(d[tool].keys()), 1)
    ax.plot(x,[0]*len(x), "black", linestyle="dashed")
    # plt.legend()
    # ax[i].tight_layout()
    ax.yaxis.set_major_formatter(FormatStrFormatter('%.2f'))
    ax.set_box_aspect(1)
    tl = ((ax.get_xlim()[1] - ax.get_xlim()[0]) + ax.get_xlim()[0] + 0.5,
      (ax.get_ylim()[1] - ax.get_ylim()[0])*0.90 + ax.get_ylim()[0])
    if show_num_benchmarks:
      total_benchmarks = '/33' if show_total_benchmarks else ''
      ax.text(tl[0], 0, f'{outperform_d[tool]}{total_benchmarks}\n{match_d[tool]}{total_benchmarks}\n{33-match_d[tool]-outperform_d[tool]}{total_benchmarks}', weight='bold', va='center')
    points.set(title=f"{title} vs {tool.replace('2q','')} ({gateset.upper() if gateset == IBM else gateset.capitalize()})")
    if not two_qubit_only:
      ax.set_facecolor(TOTAL_COLOR)
    if "NoPP" in tool or "UpTo2Q" in tool:
      prefix = '2q' if two_qubit_only else 'total'
      points.set(ylabel=r"$\bf{" + prefix + "}$" + " gate reduction")
    else:
      ax.axes.get_yaxis().get_label().set_visible(False)
    yabs_max = abs(max(ax.get_ylim(), key=abs))
    ax.set_ylim(ymin=-yabs_max, ymax=yabs_max)
    # points.figure.savefig(f"plots/rq1_{tool}_scurve_{'2q_' if two_qubit_only else ''}{gateset}.pdf", bbox_inches='tight', pad_inches=0.01) 
    # ax[i].close()

def plot_fidelity(all_data, gateset, ax):
  ratios_us = all_data[0][1]

  fidelity_ratio = {}
  outperform = 0
  match = 0
  for b in all_data[1][1].keys():
    if "qv" in b: continue
    fidelity_ratio[b] = (ratios_us[b] - all_data[1][1][b])/max(ratios_us[b], all_data[1][1][b])
    if all_data[1][1][b] <= ratios_us[b]:
      if all_data[1][1][b] == ratios_us[b]:
        match += 1
      else:
        outperform+=1

  data = pd.DataFrame({
      "program" : sorted(list(fidelity_ratio.keys()), key=fidelity_ratio.get), 
      "difference" : sorted(list(fidelity_ratio.values())),
      "circ_type": [benchmark_types[p] for p in sorted(list(fidelity_ratio.keys()), key=fidelity_ratio.get)]
      })
  points = sns.scatterplot(data=data,
    x = "program", y = "difference", edgecolor=None, ax=ax, hue="circ_type", legend=False, style="circ_type", markers=MARKERS, hue_order=TYPE_ORDER, style_order=TYPE_ORDER)
  points.grid(False, axis="x")
  points.set( xlabel = None, xticklabels=[])
  ax.tick_params(axis='x', bottom=False)

  x = np.arange(0, len(fidelity_ratio.keys()), 1)
  ax.plot(x,[0]*len(x), "black", linestyle="dashed")
  ax.yaxis.set_major_formatter(FormatStrFormatter('%.2f'))
  ax.set_box_aspect(1)
  tl = ((ax.get_xlim()[1] - ax.get_xlim()[0]) + ax.get_xlim()[0] + 0.5,
    (ax.get_ylim()[1] - ax.get_ylim()[0])*0.90 + ax.get_ylim()[0])
  ax.text(tl[0], 0, f'{outperform}\n{match}\n{33-match-outperform}', weight='bold', va='center')
  ax.set_facecolor(FIDELITY_COLOR)
  yabs_max = abs(max(ax.get_ylim(), key=abs))
  ax.set_ylim(ymin=-yabs_max, ymax=yabs_max)
  points.set(title=f"{all_data[1][0]} ({gateset.upper() if gateset == IBM else gateset.capitalize()})")
  points.set(ylabel=r"$\bf{" + "fidelity" + "}$" + " difference") 

def merge_quartz_data(gate_count_dict, gateset):
  result = {}
  #onehour3, qaoa4onehr
  result['onehour'] = {}
  result['onehour_no_td'] = {}
  for (benchmark, gate_count) in gate_count_dict['onehour3'].items():
    if (benchmark not in benchmarks and benchmark.replace("decomp0_", "") not in benchmarks) or "qaoa" in benchmark: continue
    if "decomp0" in benchmark:
      result['onehour_no_td'][benchmark.replace("decomp0_", "")] = gate_count
    else:
      result['onehour'][benchmark] = gate_count
  for (benchmark, gate_count) in gate_count_dict['qaoa4onehr'].items():
    if benchmark not in benchmarks or "qaoa" not in benchmark: continue
    result['onehour'][benchmark] = gate_count
    result['onehour_no_td'][benchmark] = gate_count

  if gateset == RIGETTI:
    #qaoa4norigettionehr, norigettiopOnehr1
    result['onehour_no_ro'] = {}
    result['onehour_no_td_no_ro'] = {}
    for (benchmark, gate_count) in gate_count_dict['norigettiopOnehr1'].items():
      if (benchmark not in benchmarks and benchmark.replace("decomp0_", "") not in benchmarks) or "qaoa" in benchmark: continue
      if "decomp0" in benchmark: 
        result['onehour_no_td_no_ro'][benchmark.replace("decomp0_", "")] = gate_count
      else:
        result['onehour_no_ro'][benchmark] = gate_count
    for (benchmark, gate_count) in gate_count_dict['qaoa4norigettionehr'].items():
      if benchmark not in benchmarks or "qaoa" not in benchmark: continue
      result['onehour_no_ro'][benchmark] = gate_count
      result['onehour_no_td_no_ro'][benchmark] = gate_count
    
  return result

def get_fidelity(file):
  fidelity = 1
  with open(file) as f:
    lines = f.readlines()
    for line in lines:
      if "rigetti" in file or "quilc" in file:
        if "rx" in line or "RX" in line:
          fidelity *= RIGETTI_1Q_FIDELITY
        elif "cz" in line or "CZ" in line:
          fidelity *= RIGETTI_2Q_FIDELITY
      elif "ion" in file:
        if ("rx" in line and not ("rxx" in line)) or "ry" in line:
          fidelity *= ION_1Q_FIDELITY
        elif "rxx" in line:
          fidelity *= ION_2Q_FIDELITY
      elif "ibm" in file:
        if "u2" in line or "u3" in line:
          fidelity *= IBM_1Q_FIDELITY
        elif "cx" in line:
          fidelity *= IBM_2Q_FIDELITY
      elif "nam" in file:
        if "h" in line or "x" in line:
          fidelity *= IBM_1Q_FIDELITY
        elif "cx" in line:
          fidelity *= IBM_2Q_FIDELITY
  return fidelity

def get_fidelity_dir(dir):
  result = {}
  for file in os.listdir(dir):
    key = file.replace(".qasm", "")
    key = key.replace("optimized_", "")
    key = key.replace("ibm_", "")
    key = key.replace("rigetti_", "")
    key = key.replace("ion_", "")
    key = key.replace("decomp0_", "")
    key = key.replace(".Qiskit_ibm", "")
    key = key.replace(".Qiskit_ion", "")
    key = key.replace(".VOQC_ibm", "")
    key = key.replace(".tket_ibm", "")
    key = key.replace(".tket_rigetti", "")
    key = key.replace(".quilc", "")
    result[key] = get_fidelity(dir+file)
  return result

def filter_dict(d):
  return { k:v for (k,v) in d.items() if PP_NAME not in k }

def fill_missing_from_paper(fresh_data, paper_data):
  result = {k:v for (k,v) in fresh_data.items()}
  for k in paper_data.keys():
    if k not in fresh_data.keys() or fresh_data[k] is None:
      result[k] = paper_data[k]
  return result

def parse_times(times_file):
  result = {}
  with open(times_file) as f:
    lines = f.readlines()
    for k in times_dict.keys():
      og = -1
      prune = -1
      for line in lines:
        if k in line:
          time = int(line[line.rfind(" ")+1:])
          time = 0.5 if time == 0 else time
          if "queso_normal" in line:
            og = time
          if "queso_pruned" in line:
            prune = time
      if og != -1 and prune != -1:
        result[k] = (og, prune)
  return result

if __name__ == "__main__":
  args = sys.argv[1:]

  BENCHMARKS_FILE = args[0].replace(".txt", "")
  TIMEOUT = args[1]

  us_results = '/root/logs/queso_gate_counts.txt'
  parse_into_csv("/root/logs", us_results, True)
  parse_into_csv("/root/logs", us_results, False)

  others_nam = '/root/logs/other_tools_nam.txt'
  others_ibm = '/root/logs/other_tools_ibm.txt'
  others_rigetti = '/root/logs/other_tools_rigetti.txt'
  others_ion = '/root/logs/other_tools_ion.txt'
  parse_other_into_csv("/root/logs", others_nam, 'nam', False)
  parse_other_into_csv("/root/logs", others_nam, 'nam', True)
  parse_other_into_csv("/root/logs", others_ibm, 'ibm', False)
  parse_other_into_csv("/root/logs", others_ibm, 'ibm', True)
  parse_other_into_csv("/root/logs", others_rigetti, 'rigetti', False)
  parse_other_into_csv("/root/logs", others_rigetti, 'rigetti', True)
  parse_other_into_csv("/root/logs", others_ion, 'ion', False)
  parse_other_into_csv("/root/logs", others_ion, 'ion', True)
  
  quartz_total = '/root/logs/quartz_total_gate_counts.txt'
  quartz_2q = '/root/logs/quartz_2q_gate_counts.txt'
  parse_quartz_csv("/root/logs", quartz_total, 'nam')
  parse_quartz_csv("/root/logs", quartz_total, 'ibmq')
  parse_quartz_csv("/root/logs", quartz_total, 'rigetti')
  parse_quartz_csv("/root/logs", quartz_2q, 'nam')
  parse_quartz_csv("/root/logs", quartz_2q, 'ibmq')
  parse_quartz_csv("/root/logs", quartz_2q, 'rigetti')

  other_tools_nam = read_csv('/root/logs/other_tools_results_nam.csv')
  other_tools_ibm = read_csv('/root/logs/other_tools_results_ibm.csv')
  other_tools_rigetti = read_csv('/root/logs/other_tools_results_rigetti.csv')
  other_tools_ion = read_csv('/root/logs/other_tools_results_ion.csv')
  other_tools_nam_2q = read_csv('/root/logs/other_tools_results_2q_nam.csv')
  other_tools_ibm_2q = read_csv('/root/logs/other_tools_results_2q_ibm.csv')
  other_tools_rigetti_2q = read_csv('/root/logs/other_tools_results_2q_rigetti.csv')
  other_tools_ion_2q = read_csv('/root/logs/other_tools_results_2q_ion.csv')

  nam_normal = "SymbolicOptimizer-Base-PQ-3.jar_3600_8000_rules_q3_s6_nam_nosub.txt_rules_q3_s3_nam_nosub_symb.txt_nam_-1_0_7_10"
  ibm_normal = "SymbolicOptimizer-Base-PQ-3.jar_3600_8000_rules_q3_s4_ibm.txt_rules_q3_s3_ibm_symb.txt_ibm_-1_0_7_10"
  rigetti_normal = "SymbolicOptimizer-Base-PQ-3.jar_3600_8000_rules_q3_s5_rigetti2.txt_rules_q3_s3_rigetti2_symb.txt_rigetti_-1_0_7_10"
  ion_rz_obj = "SymbolicOptimizer-RZ-Obj-Flip-RZ.jar_3600_8000_rules_q3_s3_ion1.txt_rules_q3_s3_ion1_symb.txt_ion_-1_0_7_10"
  ion_normal = "SymbolicOptimizer-Base-PQ-3.jar_3600_8000_rules_q3_s3_ion1.txt_rules_q3_s3_ion1_symb.txt_ion_-1_0_7_10"
  ibm_reduceall = "SymbolicOptimizer-ReduceAll-PQ-3.jar_3600_8000_rules_q3_s4_ibm.txt_rules_q3_s3_ibm_symb.txt_ibm_-1_0_7_10"
  ibm_nosymb = "SymbolicOptimizer-Base-PQ-3.jar_3600_8000_empty.txt_empty.txt_ibm_-1_0_7_10"
  ibm_sizepreservesymb = "SymbolicOptimizer-SymbPreserve-PQ-3.jar_3600_8000_rules_q3_s4_ibm.txt_rules_q3_s3_ibm_symb.txt_ibm_-1_0_7_10"
  ibm_leq2 = "SymbolicOptimizer-leq2-PQ-3.jar_3600_8000_rules_q3_s4_ibm.txt_rules_q3_s3_ibm_symb.txt_ibm_-1_0_7_10"

  paper_results_us = read_csv('/root/results_from_paper/results.csv')
  paper_results_us_2q = read_csv('/root/results_from_paper/results_2q.csv')

  results_us = read_csv('/root/logs/queso_results.csv')
  results_us_2q = read_csv('/root/logs/queso_results_2q.csv')

  paper_us_nam = filter_dict(paper_results_us[nam_normal])
  paper_us_ibm = filter_dict(paper_results_us[ibm_normal])
  paper_us_rigetti = filter_dict(paper_results_us[rigetti_normal])
  paper_us_ion = filter_dict(paper_results_us[ion_rz_obj])
  paper_us_ion_normal = filter_dict(paper_results_us[ion_normal])

  paper_us_nam_2q = filter_dict(paper_results_us_2q[nam_normal])
  paper_us_ibm_2q = filter_dict(paper_results_us_2q[ibm_normal])
  paper_us_rigetti_2q = filter_dict(paper_results_us_2q[rigetti_normal])
  paper_us_ion_2q = filter_dict(paper_results_us_2q[ion_rz_obj])
  
  QUESO_NORMAL = f"queso_normal_logs_{BENCHMARKS_FILE.replace('.txt', '')}_{TIMEOUT}_"
  us_nam = fill_missing_from_paper(filter_dict(results_us[f"{QUESO_NORMAL}nam"]), paper_us_nam)
  us_ibm = fill_missing_from_paper(filter_dict(results_us[f"{QUESO_NORMAL}ibm"]), paper_us_ibm)
  us_rigetti = fill_missing_from_paper(filter_dict(results_us[f"{QUESO_NORMAL}rigetti"]), paper_us_rigetti)
  us_ion = fill_missing_from_paper(filter_dict(results_us[f"{QUESO_NORMAL}ion"]), paper_us_ion)
  us_ion_normal = fill_missing_from_paper(filter_dict(results_us[f"{QUESO_NORMAL}ion_normal"]), paper_us_ion_normal)

  us_nam_2q = fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_NORMAL}nam"]), paper_us_nam_2q)
  us_ibm_2q = fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_NORMAL}ibm"]), paper_us_ibm_2q)
  us_rigetti_2q = fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_NORMAL}rigetti"]), paper_us_rigetti_2q)
  us_ion_2q = fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_NORMAL}ion"]), paper_us_ion_2q)

  paper_us_pp_nam = { k:v for (k,v) in paper_results_us[nam_normal].items() if PP_NAME in k }
  paper_us_pp_nam = {k.replace(PP_NAME, "").replace("nam_", ""):v for (k,v) in paper_us_pp_nam.items()}
  paper_us_pp_ibm = { k:v for (k,v) in paper_results_us[ibm_normal].items() if PP_NAME in k }
  paper_us_pp_ibm = {k.replace(PP_NAME, "").replace("ibm_", ""):v for (k,v) in paper_us_pp_ibm.items()}
  paper_us_pp_rigetti = { k:v for (k,v) in paper_results_us[rigetti_normal].items() if PP_NAME in k }
  paper_us_pp_rigetti = {k.replace(PP_NAME, "").replace("rigetti_", ""):v for (k,v) in paper_us_pp_rigetti.items()}

  paper_us_pp_nam_2q = { k:v for (k,v) in paper_results_us_2q[nam_normal].items() if PP_NAME in k }
  paper_us_pp_nam_2q = {k.replace(PP_NAME, "").replace("nam_", ""):v for (k,v) in paper_us_pp_nam_2q.items()}
  paper_us_pp_ibm_2q = { k:v for (k,v) in paper_results_us_2q[ibm_normal].items() if PP_NAME in k }
  paper_us_pp_ibm_2q = {k.replace(PP_NAME, "").replace("ibm_", ""):v for (k,v) in paper_us_pp_ibm_2q.items()}
  paper_us_pp_rigetti_2q = { k:v for (k,v) in paper_results_us_2q[rigetti_normal].items() if PP_NAME in k }
  paper_us_pp_rigetti_2q = {k.replace(PP_NAME, "").replace("rigetti_", ""):v for (k,v) in paper_us_pp_rigetti_2q.items()}

  QUESO_PP = f"queso_pp_logs_{BENCHMARKS_FILE.replace('.txt', '')}_{TIMEOUT}_"
  us_pp_nam = fill_missing_from_paper({ k:v for (k,v) in results_us[f"{QUESO_PP}nam"].items() if PP_NAME in k }, paper_us_pp_nam)
  us_pp_nam = {k.replace(PP_NAME, "").replace("nam_", ""):v for (k,v) in us_pp_nam.items()}
  us_pp_ibm = fill_missing_from_paper({ k:v for (k,v) in results_us[f"{QUESO_PP}ibm"].items() if PP_NAME in k }, paper_us_pp_ibm)
  us_pp_ibm = {k.replace(PP_NAME, "").replace("ibm_", ""):v for (k,v) in us_pp_ibm.items()}
  us_pp_rigetti = fill_missing_from_paper({ k:v for (k,v) in results_us[f"{QUESO_PP}rigetti"].items() if PP_NAME in k }, paper_us_pp_rigetti)
  us_pp_rigetti = {k.replace(PP_NAME, "").replace("rigetti_", ""):v for (k,v) in us_pp_rigetti.items()}

  us_pp_nam_2q = fill_missing_from_paper({ k:v for (k,v) in results_us_2q[f"{QUESO_PP}nam"].items() if PP_NAME in k }, paper_us_pp_nam_2q)
  us_pp_nam_2q = {k.replace(PP_NAME, "").replace("nam_", ""):v for (k,v) in us_pp_nam_2q.items()}
  us_pp_ibm_2q = fill_missing_from_paper({ k:v for (k,v) in results_us_2q[f"{QUESO_PP}ibm"].items() if PP_NAME in k }, paper_us_pp_ibm_2q)
  us_pp_ibm_2q = {k.replace(PP_NAME, "").replace("ibm_", ""):v for (k,v) in us_pp_ibm_2q.items()}
  us_pp_rigetti_2q = fill_missing_from_paper({ k:v for (k,v) in results_us_2q[f"{QUESO_PP}rigetti"].items() if PP_NAME in k }, paper_us_pp_rigetti_2q)
  us_pp_rigetti_2q = {k.replace(PP_NAME, "").replace("rigetti_", ""):v for (k,v) in us_pp_rigetti_2q.items()}
  
  nam = [(US, us_nam), (VOQC, other_tools_nam['voqc']), (QISKIT, other_tools_nam['qiskit'])]
  ibm = [(US, us_ibm), (VOQC, other_tools_ibm['voqc']), (QISKIT, other_tools_ibm['qiskit']), (TKET, other_tools_ibm['tket'])]
  rigetti = [(US, us_rigetti), (TKET, other_tools_rigetti['tket']), (QUILC, other_tools_rigetti['quilc'])]
  ion = [(US, us_ion), (QISKIT, other_tools_ion['qiskit'])]
  
  nam_2q = [(US, us_nam_2q), (VOQC, other_tools_nam_2q['voqc']), (QISKIT, other_tools_nam_2q['qiskit'])]
  ibm_2q = [(US, us_ibm_2q), (VOQC, other_tools_ibm_2q['voqc']), (QISKIT, other_tools_ibm_2q['qiskit']), (TKET, other_tools_ibm_2q['tket'])]
  rigetti_2q = [(US, us_rigetti_2q), (TKET, other_tools_rigetti_2q['tket']), (QUILC, other_tools_rigetti_2q['quilc'])]
  ion_2q = [(US, us_ion_2q), (QISKIT, other_tools_ion_2q['qiskit'])]

  rigetti_tket = [(US, us_rigetti_2q), (US, us_rigetti), (TKET+"2q", other_tools_rigetti_2q['tket']), (TKET, other_tools_rigetti['tket'])]
  rigetti_quilc = [(US, us_rigetti_2q), (US, us_rigetti), (QUILC+"2q", other_tools_rigetti_2q['quilc']), (QUILC, other_tools_rigetti['quilc'])]
  ion_qiskit = [(US, us_ion_2q), (US, us_ion), (QISKIT+"2q", other_tools_ion_2q['qiskit']), (QISKIT, other_tools_ion['qiskit'])]

  paper_us_ibm_fidelity = get_fidelity_dir('/root/results_from_paper/us_ibm_optimized/')
  paper_us_ion_fidelity = get_fidelity_dir('/root/results_from_paper/us_ion_optimized_rz_flip_rz/')
  paper_us_rigetti_fidelity = get_fidelity_dir('/root/results_from_paper/us_rigetti_optimized/')

  us_ibm_fidelity = fill_missing_from_paper(get_fidelity_dir('/root/optimized_benchmarks/optimized_queso_ibm/'), paper_us_ibm_fidelity)
  us_ion_fidelity = fill_missing_from_paper(get_fidelity_dir('/root/optimized_benchmarks/optimized_queso_ion/'), paper_us_ion_fidelity)
  us_rigetti_fidelity = fill_missing_from_paper(get_fidelity_dir('/root/optimized_benchmarks/optimized_queso_rigetti/'), paper_us_rigetti_fidelity)

  quilc_rigetti_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_quilc_rigetti/')
  tket_rigetti_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_tket_rigetti/')
  qiskit_ion_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_qiskit_ion/')
  tket_ibm_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_tket_ibm/')
  qiskit_ibm_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_qiskit_ibm/')
  voqc_ibm_fidelity = get_fidelity_dir('/root/optimized_benchmarks/optimized_voqc_ibm/')

  ############################################################################################################

  fig, axs = plt.subplots(nrows=2, ncols=3)
  plot_us_vs_other_tools_s_curve(ibm_2q, IBM, True, axs[0], 0, show_total_benchmarks=True)
  plot_fidelity([(US, us_ibm_fidelity), (VOQC, voqc_ibm_fidelity)], IBM, axs[1][0])
  plot_fidelity([(US, us_ibm_fidelity), (QISKIT, qiskit_ibm_fidelity)], IBM, axs[1][1])
  plot_fidelity([(US, us_ibm_fidelity), (TKET, tket_ibm_fidelity)], IBM, axs[1][2])
  axs[1][1].axes.get_yaxis().get_label().set_visible(False)
  axs[1][2].axes.get_yaxis().get_label().set_visible(False)
  axs[0][0].legend(title="circuit type", loc='upper left', title_fontsize='small', fontsize='small')
  fig.tight_layout(pad=0.5, rect=(0,0,2,1.07), w_pad=9)
  fig.savefig(f"plots/fig9_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)


  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve(ibm, IBM, False, axs, 0, show_total_benchmarks=True)
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=6)
  axs[0].legend(title="circuit type", loc='upper left', title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig14_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve(nam_2q, NAM, True, axs, 0, show_total_benchmarks=True)
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=6)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig15a_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve(nam, NAM, False, axs, -1)
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=6)
  fig.savefig(f"plots/fig15b_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

############################################################################################################

  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tool_s_curve(rigetti_tket, RIGETTI, axs, show_total_benchmarks=True)
  plot_fidelity([(US, us_rigetti_fidelity), (TKET, tket_rigetti_fidelity)], RIGETTI, axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=2.5)
  axs[0].legend(title="circuit type", loc='upper left')
  fig.savefig(f"plots/fig16_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  sns.set_theme(palette="colorblind", font_scale=1.25)
  sns.set_style("ticks",{'font.family': 'serif', 'axes.grid' : True})
  fig, axs = plt.subplots(nrows=1, ncols=3)
  # plot_us_vs_other_tool_s_curve(rigetti_quilc, RIGETTI, axs, show_total_benchmarks=True)
  plot_us_vs_other_tools_s_curve([(US, us_rigetti_2q), (QUILC, other_tools_rigetti_2q['quilc'])], RIGETTI, True, [axs[0]], 0, show_total_benchmarks=True)
  plot_fidelity([(US, us_rigetti_fidelity), (QUILC, quilc_rigetti_fidelity)], RIGETTI, axs[1])
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=1)
  axs[0].legend(title="circuit type", loc='upper left', title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig10a_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  fig, axs = plt.subplots(nrows=1, ncols=3)
  # plot_us_vs_other_tool_s_curve(ion_qiskit, ION, axs)
  plot_us_vs_other_tools_s_curve([(US, us_ion_2q), (QISKIT, other_tools_ion_2q['qiskit'])], ION, True, [axs[0]], 0)
  plot_fidelity([(US, us_ion_fidelity), (QISKIT, qiskit_ion_fidelity)], ION, axs[1])
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=2.1)
  axs[0].get_legend().remove()
  fig.savefig(f"plots/fig10b_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  sns.set_theme(palette="colorblind")
  sns.set_style("ticks",{'font.family': 'serif', 'axes.grid' : True})
  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve([(US, us_rigetti), (QUILC, other_tools_rigetti['quilc'])], RIGETTI, False, [axs[0]], 0, show_total_benchmarks=True)
  plot_us_vs_other_tools_s_curve([(US, us_ion), (QISKIT, other_tools_ion['qiskit'])], ION, False, [axs[1]], 1)
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=4)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig17_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve([(US, us_ion_normal), (QISKIT, other_tools_ion['qiskit'])], ION, False, [axs[0]], 0, show_total_benchmarks=True)
  fig.delaxes(axs[1])
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=4)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig18_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  ############################################################################################################

  paper_quartz_nam_2q = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_2q_nam.csv'), NAM)
  paper_quartz_ibm_2q = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_2q_ibmq.csv'), IBM)
  paper_quartz_rigetti_2q = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_2q_rigetti.csv'), RIGETTI)
  paper_quartz_nam_2q["pp"] = paper_quartz_nam_2q["onehour"]
  paper_quartz_nam_2q["nopp"] = paper_quartz_nam_2q["onehour_no_td"]
  paper_quartz_ibm_2q["pp"] = paper_quartz_ibm_2q["onehour"]
  paper_quartz_ibm_2q["nopp"] = paper_quartz_ibm_2q["onehour_no_td"]
  paper_quartz_rigetti_2q["pp"] = paper_quartz_rigetti_2q["onehour"]
  paper_quartz_rigetti_2q["nopp"] = paper_quartz_rigetti_2q["onehour_no_td_no_ro"]


  quartz_nam_2q = read_csv('/root/logs/quartz_results_2q_nam.csv')
  quartz_ibm_2q = read_csv('/root/logs/quartz_results_2q_ibmq.csv')
  quartz_rigetti_2q = read_csv('/root/logs/quartz_results_2q_rigetti.csv')

  quartz_nam_2q_pp = fill_missing_from_paper(quartz_nam_2q["pp"], paper_quartz_nam_2q["pp"])
  quartz_ibm_2q_pp = fill_missing_from_paper(quartz_ibm_2q["pp"], paper_quartz_ibm_2q["pp"])
  quartz_rigetti_2q_pp = fill_missing_from_paper(quartz_rigetti_2q["pp"], paper_quartz_rigetti_2q["pp"])
  quartz_nam_2q_nopp = fill_missing_from_paper(quartz_nam_2q["nopp"], paper_quartz_nam_2q["nopp"])
  quartz_ibm_2q_nopp = fill_missing_from_paper(quartz_ibm_2q["nopp"], paper_quartz_ibm_2q["nopp"])
  quartz_rigetti_2q_nopp = fill_missing_from_paper(quartz_rigetti_2q["nopp"], paper_quartz_rigetti_2q["nopp"])

  fig, axs = plt.subplots(nrows=3, ncols=3)
  plot_us_vs_quartz_s_curve([(US, us_ibm_2q), (QUARTZ_NO_TD, quartz_ibm_2q_nopp)], IBM, True, axs[0][0], True, US, show_total_benchmarks=True)
  plot_us_vs_quartz_s_curve([(US, us_ibm_2q), (QUARTZ, quartz_ibm_2q_pp)], IBM, True, axs[0][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_ibm_2q), (QUARTZ, quartz_ibm_2q_pp)], IBM, True, axs[0][2], False, US_PP)

  plot_us_vs_quartz_s_curve([(US, us_nam_2q), (QUARTZ_NO_TD, quartz_nam_2q_nopp)], NAM, True, axs[1][0], False, US)
  plot_us_vs_quartz_s_curve([(US, us_nam_2q), (QUARTZ, quartz_nam_2q_pp)], NAM, True, axs[1][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_nam_2q), (QUARTZ, quartz_nam_2q_pp)], NAM, True, axs[1][2], False, US_PP)
  
  plot_us_vs_quartz_s_curve([(US, us_rigetti_2q), (QUARTZ_NO_TDRO, quartz_rigetti_2q_nopp)], RIGETTI, True, axs[2][0], False, US)
  plot_us_vs_quartz_s_curve([(US, us_rigetti_2q), (QUARTZ, quartz_rigetti_2q_pp)], RIGETTI, True, axs[2][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_rigetti_2q), (QUARTZ, quartz_rigetti_2q_pp)], RIGETTI, True, axs[2][2], False, US_PP)

  fig.tight_layout(pad=0.5, rect=(0,0,2,1.5), w_pad=7)
  axs[0][0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig11_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)


  paper_quartz_nam = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_nam.csv'), NAM)
  paper_quartz_ibm = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_ibmq.csv'), IBM)
  paper_quartz_rigetti = merge_quartz_data(read_csv('/root/results_from_paper/quartz_results_rigetti.csv'), RIGETTI)
  paper_quartz_nam["pp"] = paper_quartz_nam["onehour"]
  paper_quartz_nam["nopp"] = paper_quartz_nam["onehour_no_td"]
  paper_quartz_ibm["pp"] = paper_quartz_ibm["onehour"]
  paper_quartz_ibm["nopp"] = paper_quartz_ibm["onehour_no_td"]
  paper_quartz_rigetti["pp"] = paper_quartz_rigetti["onehour"]
  paper_quartz_rigetti["nopp"] = paper_quartz_rigetti["onehour_no_td_no_ro"]

  quartz_nam = read_csv('/root/logs/quartz_results_nam.csv')
  quartz_ibm = read_csv('/root/logs/quartz_results_ibmq.csv')
  quartz_rigetti = read_csv('/root/logs/quartz_results_rigetti.csv')

  quartz_nam_pp = fill_missing_from_paper(quartz_nam["pp"], paper_quartz_nam["pp"])
  quartz_ibm_pp = fill_missing_from_paper(quartz_ibm["pp"], paper_quartz_ibm["pp"])
  quartz_rigetti_pp = fill_missing_from_paper(quartz_rigetti["pp"], paper_quartz_rigetti["pp"])
  quartz_nam_nopp = fill_missing_from_paper(quartz_nam["nopp"], paper_quartz_nam["nopp"])
  quartz_ibm_nopp = fill_missing_from_paper(quartz_ibm["nopp"], paper_quartz_ibm["nopp"])
  quartz_rigetti_nopp = fill_missing_from_paper(quartz_rigetti["nopp"], paper_quartz_rigetti["nopp"])

  fig, axs = plt.subplots(nrows=3, ncols=3)
  plot_us_vs_quartz_s_curve([(US, us_ibm), (QUARTZ_NO_TD, quartz_ibm_nopp)], IBM, False, axs[0][0], True, US, show_total_benchmarks=True)
  plot_us_vs_quartz_s_curve([(US, us_ibm), (QUARTZ, quartz_ibm_pp)], IBM, False, axs[0][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_ibm), (QUARTZ, quartz_ibm_pp)], IBM, False, axs[0][2], False, US_PP)

  plot_us_vs_quartz_s_curve([(US, us_nam), (QUARTZ_NO_TD, quartz_nam_nopp)], NAM, False, axs[1][0], False, US)
  plot_us_vs_quartz_s_curve([(US, us_nam), (QUARTZ, quartz_nam_pp)], NAM, False, axs[1][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_nam), (QUARTZ, quartz_nam_pp)], NAM, False, axs[1][2], False, US_PP)
  
  plot_us_vs_quartz_s_curve([(US, us_rigetti), (QUARTZ_NO_TDRO, quartz_rigetti_nopp)], RIGETTI, False, axs[2][0], False, US)
  plot_us_vs_quartz_s_curve([(US, us_rigetti), (QUARTZ, quartz_rigetti_pp)], RIGETTI, False, axs[2][1], False, US)
  plot_us_vs_quartz_s_curve([(US_PP, us_pp_rigetti), (QUARTZ, quartz_rigetti_pp)], RIGETTI, False, axs[2][2], False, US_PP)

  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=1)
  axs[0][0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig19_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  ############################################################################################################

  QUESO_TOGGLES = f"queso_toggles_logs_{BENCHMARKS_FILE.replace('.txt', '')}_{TIMEOUT}_ibm_"

  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve([
    ("Baseline", us_ibm_2q), 
    ("Effect of Removing Symbolic Rules", fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_TOGGLES}removeSymbolicRules"]), filter_dict(paper_results_us_2q[ibm_nosymb]))), 
    ("Effect of Removing Size-Preserving Rules", fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_TOGGLES}removeSizePreserveRules"]), filter_dict(paper_results_us_2q[ibm_reduceall]))), 
    ("Effect of Removing Rules with 3 Qubits", fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_TOGGLES}remove3QRules"]), filter_dict(paper_results_us_2q[ibm_leq2])))
    ], IBM, True, axs, 0, False)
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=12)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig13_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)


  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_other_tools_s_curve([
    ("Baseline", us_ibm), 
    ("Effect of Removing Symbolic Rules", fill_missing_from_paper(filter_dict(results_us[f"{QUESO_TOGGLES}removeSymbolicRules"]), filter_dict(paper_results_us[ibm_nosymb]))), 
    ("Effect of Removing Size-Preserving Rules", fill_missing_from_paper(filter_dict(results_us[f"{QUESO_TOGGLES}removeSizePreserveRules"]), filter_dict(paper_results_us[ibm_reduceall]))), 
    ("Effect of Removing Rules with 3 Qubits", fill_missing_from_paper(filter_dict(results_us[f"{QUESO_TOGGLES}remove3QRules"]), filter_dict(paper_results_us[ibm_leq2])))
    ], IBM, False, axs, 0, False)
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=7)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig20_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)


  fig, axs = plt.subplots(nrows=1, ncols=3)
  plot_us_vs_quartz_s_curve([("Baseline", us_ibm_2q), (US+"-UpTo2Q", fill_missing_from_paper(filter_dict(results_us_2q[f"{QUESO_TOGGLES}addSizePreserveSymbRules"]), filter_dict(paper_results_us_2q[ibm_sizepreservesymb])))], IBM, True, axs[0], True, US, False)
  plot_us_vs_quartz_s_curve([("Baseline", us_ibm), (US+"-UpTo2Q", fill_missing_from_paper(filter_dict(results_us[f"{QUESO_TOGGLES}addSizePreserveSymbRules"]), filter_dict(paper_results_us[ibm_sizepreservesymb])))], IBM, False, axs[1], False, US, False)
  fig.delaxes(axs[2])
  fig.tight_layout(pad=0.5, rect=(0,0,2,2), w_pad=7)
  axs[0].set_title("")
  axs[1].set_title("")
  fig.suptitle("Effect of Adding Size-Preserving Symbolic Rules (IBM)", x=0.7, y=1.4)
  axs[0].legend(title="circuit type", title_fontsize='small', fontsize='small')
  fig.savefig(f"plots/fig21_{BENCHMARKS_FILE}_{TIMEOUT}.pdf", bbox_inches='tight', pad_inches=0.1)

  ############################################################################################################

  sns.set_theme(palette="colorblind", font_scale=3)
  sns.set_style("ticks",{'font.family': 'serif', 'axes.grid' : True})

  fresh_times_dict = parse_times("/root/logs/queso_ibm_time_to_best.txt")
  fresh_times_dict = fill_missing_from_paper(fresh_times_dict, times_dict)

  times = [fresh_times_dict[b] for b in benchmarks if b in times_dict.keys()]

  original, applied = zip(*times)
  bmks = benchmarks
  bmks.remove("barenco_tof_4")
  bmks.remove("barenco_tof_5")
  bmks.remove("gf2^5_mult")
  bmks.remove("gf2^6_mult")
  bmks.remove("gf2^7_mult")
  bmks.remove("gf2^8_mult")
  bmks.remove("gf2^9_mult")
  bmks.remove("tof_4")
  bmks.remove("tof_5")
  bmks.remove("qaoa_n6_p4")
  bmks.remove("qaoa_n8_p4")
  # bmks.remove("qaoa_n10_p4")
  bmks.remove("qaoa_n14_p4")

  data = pd.DataFrame({
      "circuit": bmks*2, 
      "method": len(bmks)*["original"] + len(bmks)*["pruned"],
      "time (s)": original + applied
    })

  barchart = sns.catplot(
      data=data,
      x = "circuit",
      y = "time (s)",
      hue = "method",
      kind = "bar", 
      aspect=10/1
  )

  def autolabel(rects, fmt='d'):
    # attach some text labels
    for rect in rects:
        height = rect.get_height()
        if height == 0.5:
          rect.set_color('white')
          rect.axes.annotate(f'{{:{fmt}}}'.format(int(height)),
                           xy=(rect.get_x()+rect.get_width()/2., 0.35),
                           xytext=(0, 3), textcoords='offset points',
                           ha='center', va='bottom')
        else:
          rect.axes.annotate(f'{{:{fmt}}}'.format(int(height)),
                            xy=(rect.get_x()+rect.get_width()/2., height),
                            xytext=(0, 3), textcoords='offset points',
                            ha='center', va='bottom')
  autolabel(barchart.ax.patches)

  barchart.set_xticklabels(rotation=45, horizontalalignment='right')
  plt.yscale('log')
  barchart.savefig(f"plots/fig12_{BENCHMARKS_FILE}_{TIMEOUT}.pdf")
  plt.close()
  plt.clf()
