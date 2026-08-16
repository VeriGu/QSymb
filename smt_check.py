#!/usr/bin/env python3
import base64
import math
import sys
from fractions import Fraction

import z3

Z3_TIMEOUT_MS = 30000
NUM_SAMPLES = 2
PHASE_EPS = 1e-6


def cmul(a, b):
    return (a[0] * b[0] - a[1] * b[1], a[0] * b[1] + a[1] * b[0])


def cadd(a, b):
    return (a[0] + b[0], a[1] + b[1])


def ang_add(a, b):
    return (a[0] * b[0] - a[1] * b[1], a[1] * b[0] + a[0] * b[1])


def ang_neg(a):
    return (a[0], -a[1])


class AngleParseError(Exception):
    pass


def _parse_term(term):
    factors = term.split('*')
    num = Fraction(1)
    sym = None
    has_pi = False
    for f in factors:
        f = f.strip()
        if '/' in f:
            top, bot = f.split('/', 1)
            top, bot = top.strip(), bot.strip()
            if top == 'pi':
                has_pi = True
            else:
                num *= _to_fraction(top)
            num /= _to_fraction(bot)
        elif f == 'pi':
            has_pi = True
        elif f.startswith('theta') or f in ('phi', 'lam', 'gamma'):
            if sym is not None:
                raise AngleParseError(f"nonlinear term: {term}")
            sym = f
        else:
            num *= _to_fraction(f)
    if has_pi and sym is not None:
        raise AngleParseError(f"pi*sym term: {term}")
    if has_pi:
        return num, None, 0
    if sym is not None:
        if num.denominator != 1:
            raise AngleParseError(f"fractional symbol coefficient: {term}")
        return Fraction(0), sym, int(num)
    val = float(num)
    snapped = Fraction(round(val / (math.pi / 8)), 8)
    if abs(val - float(snapped) * math.pi) > 1e-9:
        raise AngleParseError(f"constant {term} is not a multiple of pi/8")
    return snapped, None, 0


def _to_fraction(tok):
    tok = tok.strip()
    try:
        return Fraction(tok)
    except ValueError:
        f = float(tok)
        fr = Fraction(f).limit_denominator(64)
        if abs(float(fr) - f) > 1e-12:
            raise AngleParseError(f"non-rational literal: {tok}")
        return fr


def parse_angle(expr):
    expr = expr.replace(' ', '')
    terms = []
    cur, sign = '', 1
    for ch in expr:
        if ch in '+-' and cur:
            terms.append((sign, cur))
            cur, sign = '', (1 if ch == '+' else -1)
        elif ch in '+-' and not cur:
            sign = sign * (1 if ch == '+' else -1)
        else:
            cur += ch
    if cur:
        terms.append((sign, cur))
    pi_coeff = Fraction(0)
    syms = {}
    for sign, term in terms:
        p, s, k = _parse_term(term)
        pi_coeff += sign * p
        if s is not None:
            syms[s] = syms.get(s, 0) + sign * k
    syms = {s: k for s, k in syms.items() if k != 0}
    if pi_coeff.denominator not in (1, 2, 4, 8):
        raise AngleParseError(f"pi denominator {pi_coeff.denominator} unsupported: {expr}")
    return pi_coeff, syms


def parse_circuit(text):
    wi = text.lower().find(' when ')
    if wi >= 0:
        text = text[:wi]
    gates = []
    for stmt in text.split(';'):
        stmt = stmt.strip()
        if not stmt:
            continue
        if '(' in stmt:
            name = stmt[:stmt.index('(')].strip()
            angle = stmt[stmt.index('(') + 1:stmt.index(')')].strip()
            rest = stmt[stmt.index(')') + 1:]
        else:
            parts = stmt.split(None, 1)
            name, angle, rest = parts[0], None, (parts[1] if len(parts) > 1 else '')
        qubits = []
        for tok in rest.replace(',', ' ').split():
            tok = tok.strip()
            if tok.startswith('q'):
                qubits.append(int(tok[1:].strip('[]')))
        gates.append((name.lower(), angle, qubits))
    return gates


class Ctx:

    def __init__(self, symbols, symbolic=True, values=None):
        self.symbolic = symbolic
        self.constraints = []
        self.pairs = {}
        if symbolic:
            self.sqrt2 = z3.Real('const_rt2')
            self.constraints += [self.sqrt2 * self.sqrt2 == 2, self.sqrt2 > 0]
            self.c8 = z3.Real('const_c8')
            self.s8 = z3.Real('const_s8')
            self._c8_used = False
            for s in symbols:
                c = z3.Real(f'c_{s}')
                si = z3.Real(f's_{s}')
                self.constraints.append(c * c + si * si == 1)
                self.pairs[s] = (c, si)
        else:
            self.sqrt2 = math.sqrt(2)
            self.c8 = math.cos(math.pi / 8)
            self.s8 = math.sin(math.pi / 8)
            for s in symbols:
                t = values[s]
                self.pairs[s] = (math.cos(t / 2), math.sin(t / 2))

    def const_pair(self, r):
        r = r % 2
        p = int(r * 8)
        if self.symbolic:
            H = self.sqrt2 / 2
            ZERO, ONE = z3.RealVal(0), z3.RealVal(1)
        else:
            H = self.sqrt2 / 2
            ZERO, ONE = 0.0, 1.0
        quarter_table = [
            (ONE, ZERO), (H, H), (ZERO, ONE), (-H, H),
            (-ONE, ZERO), (-H, -H), (ZERO, -ONE), (H, -H),
        ]
        out = quarter_table[(p // 2) % 8]
        if p % 2:
            if self.symbolic and not self._c8_used:
                self._c8_used = True
                self.constraints += [
                    self.c8 * self.c8 == (2 + self.sqrt2) / 4,
                    self.s8 * self.s8 == (2 - self.sqrt2) / 4,
                    self.c8 > 0, self.s8 > 0,
                ]
            out = ang_add(out, (self.c8, self.s8))
        return out

    def half_angle_pair(self, pi_coeff, syms):
        out = self.const_pair(pi_coeff / 2)
        for s, k in syms.items():
            base = self.pairs[s] if k > 0 else ang_neg(self.pairs[s])
            for _ in range(abs(k)):
                out = ang_add(out, base)
        return out

    def angle_pair(self, pi_coeff, syms):
        h = self.half_angle_pair(pi_coeff, syms)
        return ang_add(h, h)


def gate_matrix(ctx, name, angle):
    ONE, ZERO = _c(ctx, 1), _c(ctx, 0)
    I_POS, I_NEG = (ZERO[0], ONE[0]), (ZERO[0], -ONE[0])

    if name in ('i', 'id'):
        return [[ONE, ZERO], [ZERO, ONE]]
    if name == 'x':
        return [[ZERO, ONE], [ONE, ZERO]]
    if name == 'y':
        return [[ZERO, I_NEG], [I_POS, ZERO]]
    if name == 'z':
        return [[ONE, ZERO], [ZERO, _c(ctx, -1)]]
    if name == 'h':
        r = _inv_sqrt2(ctx)
        return [[(r, ZERO[0]), (r, ZERO[0])], [(r, ZERO[0]), (-r, ZERO[0])]]
    if name == 's':
        return [[ONE, ZERO], [ZERO, I_POS]]
    if name == 'sdg':
        return [[ONE, ZERO], [ZERO, I_NEG]]
    if name == 't':
        r = _inv_sqrt2(ctx)
        return [[ONE, ZERO], [ZERO, (r, r)]]
    if name == 'tdg':
        r = _inv_sqrt2(ctx)
        return [[ONE, ZERO], [ZERO, (r, -r)]]
    if name == 'sx':
        h = _half(ctx)
        return [[(h, h), (h, -h)], [(h, -h), (h, h)]]
    if name in ('cx', 'cnot'):
        return [[ONE, ZERO, ZERO, ZERO],
                [ZERO, ONE, ZERO, ZERO],
                [ZERO, ZERO, ZERO, ONE],
                [ZERO, ZERO, ONE, ZERO]]
    if name == 'cz':
        return [[ONE, ZERO, ZERO, ZERO],
                [ZERO, ONE, ZERO, ZERO],
                [ZERO, ZERO, ONE, ZERO],
                [ZERO, ZERO, ZERO, _c(ctx, -1)]]
    if name == 'swap':
        return [[ONE, ZERO, ZERO, ZERO],
                [ZERO, ZERO, ONE, ZERO],
                [ZERO, ONE, ZERO, ZERO],
                [ZERO, ZERO, ZERO, ONE]]

    pi_coeff, syms = parse_angle(angle)
    c, s = ctx.half_angle_pair(pi_coeff, syms)
    if name in ('rz', 'vz'):
        return [[(c, -s), ZERO], [ZERO, (c, s)]]
    if name == 'rx':
        return [[(c, ZERO[0]), (ZERO[0], -s)], [(ZERO[0], -s), (c, ZERO[0])]]
    if name == 'ry':
        return [[(c, ZERO[0]), (-s, ZERO[0])], [(s, ZERO[0]), (c, ZERO[0])]]
    if name == 'u1' or name == 'p':
        cf, sf = ctx.angle_pair(pi_coeff, syms)
        return [[ONE, ZERO], [ZERO, (cf, sf)]]
    if name in ('rxx', 'ms'):
        Z = ZERO
        return [[(c, Z[0]), Z, Z, (Z[0], -s)],
                [Z, (c, Z[0]), (Z[0], -s), Z],
                [Z, (Z[0], -s), (c, Z[0]), Z],
                [(Z[0], -s), Z, Z, (c, Z[0])]]
    raise AngleParseError(f"unsupported gate: {name}")


def _c(ctx, v):
    if ctx.symbolic:
        return (z3.RealVal(v), z3.RealVal(0))
    return (float(v), 0.0)


def _inv_sqrt2(ctx):
    if ctx.symbolic:
        return 1 / ctx.sqrt2
    return 1 / math.sqrt(2)


def _half(ctx):
    if ctx.symbolic:
        return z3.RealVal(1) / 2
    return 0.5


def circuit_unitary(ctx, gates, n):
    dim = 1 << n
    U = [[_c(ctx, 1 if r == c else 0) for c in range(dim)] for r in range(dim)]
    for name, angle, qubits in gates:
        G = gate_matrix(ctx, name, angle)
        U = _apply(ctx, G, qubits, U, n)
    return U


def _apply(ctx, G, targets, U, n):
    dim = 1 << n
    k = len(targets)
    weights = [1 << (n - 1 - q) for q in targets]
    result = [[None] * dim for _ in range(dim)]
    for r in range(dim):
        tr = 0
        for j, w in enumerate(weights):
            if r & w:
                tr |= 1 << (k - 1 - j)
        base = r
        for w in weights:
            base &= ~w
        for c in range(dim):
            acc = _c(ctx, 0)
            for tc in range(1 << k):
                rp = base
                for j, w in enumerate(weights):
                    if tc & (1 << (k - 1 - j)):
                        rp |= w
                acc = cadd(acc, cmul(G[tr][tc], U[rp][c]))
            result[r][c] = acc
    return result


def collect_symbols(gates):
    syms = set()
    for _, angle, _ in gates:
        if angle is not None:
            _, s = parse_angle(angle)
            syms.update(s.keys())
    return syms


def check_equivalent(lhs_text, rhs_text):
    try:
        lhs = parse_circuit(lhs_text)
        rhs = parse_circuit(rhs_text)
        n = 0
        for _, _, qs in lhs + rhs:
            for q in qs:
                n = max(n, q + 1)
        if n > 4:
            return f"UNKNOWN too many qubits ({n})"
        syms = sorted(collect_symbols(lhs) | collect_symbols(rhs))

        import random
        rng = random.Random(12345)
        samples = []
        for _ in range(NUM_SAMPLES):
            vals = {s: rng.uniform(0.1, 2 * math.pi) for s in syms}
            nctx = Ctx(syms, symbolic=False, values=vals)
            A = circuit_unitary(nctx, lhs, n)
            B = circuit_unitary(nctx, rhs, n)
            samples.append((vals, A, B))

        candidate = _find_phase(samples, syms)
        if candidate is None:
            return "INVALID"
        k, ms = candidate

        sctx = Ctx(syms, symbolic=True)
        A = circuit_unitary(sctx, lhs, n)
        B = circuit_unitary(sctx, rhs, n)
        lam = sctx.const_pair(Fraction(k, 4))
        for s in syms:
            m = ms[s]
            if m:
                base = sctx.pairs[s] if m > 0 else ang_neg(sctx.pairs[s])
                for _ in range(abs(m)):
                    lam = ang_add(lam, base)
        eqs = []
        dim = 1 << n
        for r in range(dim):
            for c in range(dim):
                lb = cmul(lam, B[r][c])
                eqs.append(A[r][c][0] == lb[0])
                eqs.append(A[r][c][1] == lb[1])
        solver = z3.Solver()
        solver.set('timeout', Z3_TIMEOUT_MS)
        solver.add(sctx.constraints)
        solver.add(z3.Not(z3.And(eqs)))
        res = solver.check()
        if res == z3.unsat:
            return "VALID"
        if res == z3.sat:
            return "INVALID"
        return "UNKNOWN z3 timeout"
    except AngleParseError as e:
        return f"UNKNOWN {e}"
    except Exception as e:
        return f"UNKNOWN {type(e).__name__}: {e}"


def _find_phase(samples, syms):
    ratios = []
    for vals, A, B in samples:
        best, bmag = None, 0.0
        dim = len(A)
        for r in range(dim):
            for c in range(dim):
                mag = B[r][c][0] ** 2 + B[r][c][1] ** 2
                if mag > bmag:
                    bmag, best = mag, (r, c)
        if best is None or bmag < 1e-12:
            return None
        r, c = best
        b = complex(*B[r][c])
        a = complex(*A[r][c])
        ratios.append((vals, a / b))

    import itertools
    for k in range(8):
        for ms in itertools.product(range(-2, 3), repeat=len(syms)):
            ok = True
            for vals, ratio in ratios:
                ang = k * math.pi / 4
                for s, m in zip(syms, ms):
                    ang += m * vals[s] / 2
                if abs(complex(math.cos(ang), math.sin(ang)) - ratio) > PHASE_EPS:
                    ok = False
                    break
            if ok:
                for vals, A, B in samples:
                    ang = k * math.pi / 4
                    for s, m in zip(syms, ms):
                        ang += m * vals[s] / 2
                    lam = complex(math.cos(ang), math.sin(ang))
                    dim = len(A)
                    for r in range(dim):
                        for c in range(dim):
                            if abs(complex(*A[r][c]) - lam * complex(*B[r][c])) > PHASE_EPS:
                                ok = False
                                break
                        if not ok:
                            break
                    if not ok:
                        break
            if ok:
                return k, dict(zip(syms, ms))
    return None


def serve():
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        parts = [base64.b64decode(p).decode('utf-8') for p in line.split('\t')]
        if parts[0] == 'SHUTDOWN':
            break
        if parts[0] == 'CHECK' and len(parts) == 3:
            verdict = check_equivalent(parts[1], parts[2])
        else:
            verdict = f"UNKNOWN bad request {parts[0]}"
        sys.stdout.write(base64.b64encode(verdict.encode('utf-8')).decode('ascii'))
        sys.stdout.write('\n')
        sys.stdout.flush()


def main():
    if '--server' in sys.argv:
        serve()
        return 0
    if '--check' in sys.argv:
        i = sys.argv.index('--check')
        print(check_equivalent(sys.argv[i + 1], sys.argv[i + 2]))
        return 0
    sys.stderr.write(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main())
