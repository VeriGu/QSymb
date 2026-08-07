from __future__ import annotations
from typing import Dict, List, Tuple, Set
import math
import networkx as nx
from qiskit import QuantumCircuit
from qiskit.dagcircuit import DAGCircuit, DAGOpNode

# ---------- build op-only DAG ----------
def _op_nodes_in_topo(dag: DAGCircuit) -> List[DAGOpNode]:
    return [n for n in dag.topological_op_nodes()]

def _succ_ops(dag: DAGCircuit, n: DAGOpNode) -> List[DAGOpNode]:
    return [s for s in dag.successors(n) if isinstance(s, DAGOpNode)]

def _build_op_dag(dag: DAGCircuit) -> nx.DiGraph:
    G = nx.DiGraph()
    nodes = _op_nodes_in_topo(dag)
    G.add_nodes_from(nodes)
    for u in nodes:
        for v in _succ_ops(dag, u):
            G.add_edge(u, v)
    assert nx.is_directed_acyclic_graph(G), "Input must be a DAG."
    return G

# ---------- safe Kahn fallback (always produces a valid topo order) ----------
def _kahn_topo_order(G: nx.DiGraph) -> List[DAGOpNode]:
    indeg = {n: G.in_degree(n) for n in G.nodes()}
    ready = [n for n, d in indeg.items() if d == 0]
    order = []
    while ready:
        n = ready.pop()
        order.append(n)
        for s in G.successors(n):
            indeg[s] -= 1
            if indeg[s] == 0:
                ready.append(s)
    return order

# ---------- ILP MinLA with robust validation & fallback ----------
def _ilp_minla_topo_order(G: nx.DiGraph, time_limit_sec: int | None = 120) -> List[DAGOpNode]:
    try:
        import pulp
    except ModuleNotFoundError:
        return _kahn_topo_order(G)

    nodes = list(G.nodes())
    n = len(nodes)
    idx_of = {nodes[i]: i for i in range(n)}
    positions = list(range(1, n + 1))

    X = pulp.LpVariable.dicts(
        "X", ((i, k) for i in range(n) for k in positions),
        lowBound=0, upBound=1, cat=pulp.LpBinary
    )

    model = pulp.LpProblem("MinLA_with_precedence", pulp.LpMinimize)

    # objective: sum_{(u->v)} (sum k*k*X[v,k] - sum k*k*X[u,k])
    obj_terms = []
    for u, v in G.edges():
        iu, iv = idx_of[u], idx_of[v]
        obj_terms += [k * X[(iv, k)] for k in positions]
        obj_terms += [-k * X[(iu, k)] for k in positions]
    model += pulp.lpSum(obj_terms)

    # each node exactly one position
    for i in range(n):
        model += pulp.lpSum(X[(i, k)] for k in positions) == 1

    # each position taken by exactly one node
    for k in positions:
        model += pulp.lpSum(X[(i, k)] for i in range(n)) == 1

    # precedence: p[v] - p[u] >= 1
    for u, v in G.edges():
        iu, iv = idx_of[u], idx_of[v]
        model += (
            pulp.lpSum(k * X[(iv, k)] for k in positions) -
            pulp.lpSum(k * X[(iu, k)] for k in positions)
            >= 1
        )

    if time_limit_sec is not None:
        solver = pulp.PULP_CBC_CMD(
            msg=False,
            timeLimit=time_limit_sec,
            mip=True,
        )
    else:
        solver = pulp.PULP_CBC_CMD(msg=False, mip=True, strong=True)

    status = model.solve(solver)
    status_str = pulp.LpStatus[status]

    tol = 1e-5
    pos_of: Dict[int, int] = {}
    taken_pos: Set[int] = set()
    feasible = True

    for i in range(n):
        candidates = [(k, pulp.value(X[(i, k)]) or 0.0) for k in positions]
        k_star, v_star = max(candidates, key=lambda kv: kv[1])

        if v_star < 0.5 - 1e-6:
            feasible = False
            break

        pos_of[i] = k_star

    if feasible:
        used = set()
        for i in range(n):
            k = pos_of[i]
            if k not in used:
                used.add(k)
                continue
            alts = sorted(
                [(k2, pulp.value(X[(i, k2)]) or 0.0) for k2 in positions if k2 not in used],
                key=lambda kv: kv[1], reverse=True
            )
            if not alts:
                feasible = False
                break
            pos_of[i] = alts[0][0]
            used.add(pos_of[i])

    if feasible:
        for (u, v) in G.edges():
            iu, iv = idx_of[u], idx_of[v]
            if pos_of[iv] <= pos_of[iu]:
                feasible = False
                break

    if not feasible:
        return _kahn_topo_order(G)

    order_idx = sorted(range(n), key=lambda i: pos_of[i])
    return [nodes[i] for i in order_idx]

def linearized_circuit_from_dag(
    dag: DAGCircuit,
    ilp_time_limit_sec: int | None = 10,
) -> QuantumCircuit:
    G = _build_op_dag(dag)
    node_order = _ilp_minla_topo_order(G, time_limit_sec=ilp_time_limit_sec)

    qc = QuantumCircuit(*dag.qregs.values(), *dag.cregs.values())
    qc.global_phase = dag.global_phase
    for n in node_order:
        qc.append(n.op.copy(), n.qargs, n.cargs)
    return qc
