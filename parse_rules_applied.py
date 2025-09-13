import sys

def parse_rules(file):
  with open(file) as f:
    lines = f.readlines()
    rules = {}
    for line in lines:
      line = line[line.index("m:")+3:]
      line = line[1:len(line)-1]
      while line.count("[") > 1:
        if "symb" in line[:line.index("|")]:
          rule = line[1:line.index("}]")+2]
          if rule in rules:
            rules[rule] +=1
          else:
            rules[rule] = 1
          try:
            line = line[line.index("[", line.index("}]")):]
          except:
            break
        else:
          rule = line[1:line.rindex(";",0,line.index("]"))+1]
          if rule in rules:
            rules[rule] +=1
          else:
            rules[rule] = 1
          try:
            line = line[line.index("[", line.index("]")):]
          except:
            break
    return sorted(rules.items(), key=lambda kv: kv[1], reverse=True)

def check_connectivity(rules):
  for (rule,x) in rules:
    split_rule = rule.split("| ")
    replace = split_rule[0]
    pattern = split_rule[1]

    replace_gates = replace.split("; ")
    pattern_gates = pattern.split("; ")

    qubit_pairs_pattern = []
    qubit_pairs_replace = []
    for gate in pattern_gates:
      gate = gate.replace(";", "")
      if "cx" in gate or "cz" in gate:
        qubit1 = gate[gate.find("q")+1:gate.find(",")]
        qubit2 = gate[gate.rfind("q")+1:]
        qubit_pairs_pattern.append(sorted((int(qubit1), int(qubit2))))
    for gate in replace_gates:
      gate = gate.replace(";", "")
      if "cx" in gate or "cz" in gate:
        qubit1 = gate[gate.find("q")+1:gate.find(",")]
        qubit2 = gate[gate.rfind("q")+1:]
        qubit_pairs_replace.append(sorted((int(qubit1), int(qubit2))))

    for pair in qubit_pairs_replace:
      if pair not in qubit_pairs_pattern:
        print(pair)
        print(rule)
        print(x)

def prune_rules_not_applied(file1, file2, rules):
  pruned = []
  with open(file1) as f1:
    with open(file2) as f2:
      lines = f1.read().splitlines()
      lines.extend(f2.read().splitlines())
      for line in lines:
        if line in rules:
          pruned.append(line)
  return pruned

def prune_rules_not_applied(file1, rules):
  pruned = []
  with open(file1) as f1:
    lines = f1.read().splitlines()
    for line in lines:
      if line in rules:
        pruned.append(line)
  return pruned

if __name__ == "__main__":
  args = sys.argv[1:]
  # check_connectivity(parse_rules(args[0]))
  rules = []
  for (rule,_) in parse_rules(args[0]):
    rules.append(rule)
  for rule in prune_rules_not_applied(args[1], rules):
    print(rule)