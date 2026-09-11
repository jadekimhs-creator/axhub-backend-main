package io.shinhanlife.dat.report.source;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Optional;
import java.util.stream.Collectors;

/** JavaParser AST에서 어노테이션 값을 안전하게 읽는 도우미. */
final class JavaAnnotationReader {

    private JavaAnnotationReader() {
    }

    static Optional<AnnotationExpr> find(NodeList<AnnotationExpr> annotations, String simpleName) {
        return annotations.stream().filter(a -> a.getName().getIdentifier().equals(simpleName)).findFirst();
    }

    static Optional<Expression> value(AnnotationExpr annotation, String key) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            return normal.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(key))
                    .map(MemberValuePair::getValue)
                    .findFirst();
        }
        if (annotation instanceof SingleMemberAnnotationExpr single && "value".equals(key)) {
            return Optional.of(single.getMemberValue());
        }
        return Optional.empty();
    }

    static String string(AnnotationExpr annotation, String key, String fallback) {
        return value(annotation, key)
                .filter(StringLiteralExpr.class::isInstance)
                .map(StringLiteralExpr.class::cast)
                .map(StringLiteralExpr::asString)
                .orElse(fallback);
    }

    static boolean bool(AnnotationExpr annotation, String key, boolean fallback) {
        return value(annotation, key)
                .filter(BooleanLiteralExpr.class::isInstance)
                .map(BooleanLiteralExpr.class::cast)
                .map(BooleanLiteralExpr::getValue)
                .orElse(fallback);
    }

    static String strings(AnnotationExpr annotation, String key) {
        return value(annotation, key)
                .filter(ArrayInitializerExpr.class::isInstance)
                .map(ArrayInitializerExpr.class::cast)
                .map(array -> array.getValues().stream()
                        .filter(StringLiteralExpr.class::isInstance)
                        .map(StringLiteralExpr.class::cast)
                        .map(StringLiteralExpr::asString)
                        .collect(Collectors.joining(" | ")))
                .orElse("");
    }

    static Optional<AnnotationExpr> nested(AnnotationExpr annotation, String key) {
        return value(annotation, key).filter(AnnotationExpr.class::isInstance).map(AnnotationExpr.class::cast);
    }
}
