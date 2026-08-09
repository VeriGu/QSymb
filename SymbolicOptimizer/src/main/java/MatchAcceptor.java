import java.util.List;
import java.util.Map;

import ast.Expr;

@FunctionalInterface
public interface MatchAcceptor {
    boolean accept(List<Node> matched,
                   Map<String, String> qubitMap,
                   Map<String, String> reverseMap,
                   Map<String, Expr> angleMap);
}
