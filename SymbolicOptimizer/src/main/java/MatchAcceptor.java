import java.util.List;
import java.util.Map;

import ast.Expr;

/**
 * Acceptance test for a symbolic-rule match candidate.
 *
 * <p>The matcher proposes candidates with a progressively larger symbolic
 * region. An acceptor that returns {@code false} tells the matcher to keep
 * growing the region (up to max symb size) and try the next candidate, rather
 * than stopping at the first constraint-satisfying match. This lets the outer
 * equivalence check act as a second acceptance gate alongside the symbolic
 * constraint check.
 */
@FunctionalInterface
public interface MatchAcceptor {
    boolean accept(List<Node> matched,
                   Map<String, String> qubitMap,
                   Map<String, String> reverseMap,
                   Map<String, Expr> angleMap);
}
