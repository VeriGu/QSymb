import sympy as sp

def intertwiner_basis(L, R):
    """
    Return a list of basis matrices {S_k} spanning all solutions to L S = S R
    (no unitarity enforced here; purely the linear intertwiner space).
    Works with numeric or symbolic entries.
    """
    n = L.shape[0]
    K = sp.kronecker_product(L, sp.eye(n)) - sp.kronecker_product(sp.eye(n), R.T)
    null = K.nullspace()
    print("nullity =", null)
    basis = [sp.Matrix(n, n, v) for v in null]  # reshape vec -> n×n
    return basis, K

def unitary_solutions_from_basis(basis):
    """
    If you want to enforce unitarity S†S=I in the found subspace:
    parametrize S = sum_j (a_j) * B_j, then impose polynomial equations.
    Returns the symbolic polynomial system (you can solve or substitute numerically).
    """
    if not basis:
        return None, []
    n = basis[0].rows
    # Complex coefficients a_j = x_j + i y_j (re/im)
    xs = sp.symbols(' '.join([f'x{j}' for j in range(len(basis))]), real=True)
    ys = sp.symbols(' '.join([f'y{j}' for j in range(len(basis))]), real=True)
    coeffs = [xs[j] + sp.I*ys[j] for j in range(len(basis))]
    S = sum((coeffs[j]*basis[j] for j in range(len(basis))), sp.zeros(n))
    # Unitarity constraints: S† S = I  (n^2 scalar equations)
    raw_eqs = list((S.H * S - sp.eye(n)).reshape(n*n, 1))
    # (Optionally also S S† = I; it’s redundant if the subspace is correct, but can help)
    eqs = [sp.Eq(e, 0) for e in raw_eqs]
    return sp.simplify(S), eqs

# ---------- Examples ----------

# 1) Purely symbolic 1-qubit rotations: L=Rz(θ), R=Rz(φ)
θ, φ = sp.symbols('theta phi', real=True)

def Rz(a):
    return sp.Matrix([[sp.exp(-sp.I*a/2), 0],
                      [0, sp.exp(sp.I*a/2)]])

L = Rz(θ)
R = Rz(φ)

basis, K = intertwiner_basis(L, R)

print("Intertwiner basis for generic (θ, φ):")
print(len(basis))
for B in basis:
    sp.pprint(B)
print("Rank(K) =", K.rank(), "  det(K) factorized:")
sp.pprint(sp.factor(sp.together(sp.det(K))))

# The determinant of K is ∏_{i,j} (λ_i(L) - μ_j(R)).
# For 2×2 Rz, det(K) = (e^{-iθ/2}-e^{-iφ/2})(e^{-iθ/2}-e^{+iφ/2})
#                     (e^{+iθ/2}-e^{-iφ/2})(e^{+iθ/2}-e^{+iφ/2})
# => det(K)=0 exactly when θ ≡ ±φ (mod 4π) (strict matrix equality; mod 2π if ignoring global phase).

# If you want the actual basis on a *solution* branch (e.g., θ = φ):
branch = {φ: θ}  # enforce spectra match
basis_branch, _ = intertwiner_basis(L.subs(branch), R.subs(branch))

print("\nIntertwiner basis on branch φ = θ (all eigenvalues match):")
for B in basis_branch:
    sp.pprint(B)

# Enforce unitarity inside that subspace:
S_param, eqs = unitary_solutions_from_basis(basis_branch)
print("\nParametric S in that subspace:")
sp.pprint(S_param)
print("\nUnitarity equations S†S = I (solve for coefficients):")
for e in eqs:
    sp.pprint(e)

# You’ll see the basis are the two diagonal matrix units E11, E22,
# so S = (x1+i y1) E11 + (x2+i y2) E22, and unitarity enforces |x1+iy1|=|x2+iy2|=1.
# A convenient parametrization is S = diag(e^{iα}, e^{iβ}).

# 2) Mixed axes: L=Rz(θ), R=Rx(φ)  (symbolic still)
def Rx(a):
    c, s = sp.cos(a/2), sp.sin(a/2)
    return sp.Matrix([[c, -sp.I*s],
                      [-sp.I*s, c]])


# CX q0 q1; S; CX q0 q1 = S
def CX():
    return sp.Matrix([[1, 0, 0, 0],
                      [0, 1, 0, 0],
                      [0, 0, 0, 1],
                      [0, 0, 1, 0]])


L2 = CX()
R2 = CX()
basis2, K2 = intertwiner_basis(L2, R2)

for B in basis2:
    sp.pprint(sp.N(B))


S_param, eqs = unitary_solutions_from_basis(basis2)
sp.pprint(S_param)
for e in eqs:
    sp.pprint(e)

# L2 = Rz(θ)
# R2 = Rx(φ)
# basis2, K2 = intertwiner_basis(L2, R2)

# print("\nMixed axes Rz(θ) vs Rx(φ): det(K) factorized:")
# sp.pprint(sp.factor(sp.together(sp.det(K2))))

# # det(K2)=0 ⇔ spectra share an eigenvalue ⇔ θ ≡ ±φ (mod 4π).
# # On those branches, recompute basis and (optionally) solve unitarity as above.

# # 3) Numeric bind (e.g., θ=φ=1.2) to get an explicit unitary S
# branch = {φ: θ}  # enforce spectra match
# basis_num, _ = intertwiner_basis(L.subs(branch), R.subs(branch))
# print("\nNumeric basis at θ=φ:")
# for B in basis_num:
#     sp.pprint(sp.N(B))


# S_param, eqs = unitary_solutions_from_basis(basis_num)
# print("\nParametric S in that subspace:")
# sp.pprint(S_param)
# print("\nUnitarity equations S†S = I (solve for coefficients):")
# for e in eqs:
#     sp.pprint(e)

