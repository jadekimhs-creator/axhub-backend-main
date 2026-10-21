package io.shinhanlife.dat.report.source;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import java.util.Objects;
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
                .map(JavaAnnotationReader::expressionToString)
                .filter(Objects::nonNull)
                .orElse(fallback);
    }

    static String expressionToString(Expression expr) {
        if (expr instanceof StringLiteralExpr str) {
            return str.asString();
        }
        if (expr instanceof TextBlockLiteralExpr textBlock) {
            return textBlock.asString();
        }
        if (expr instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            String left = expressionToString(binary.getLeft());
            String right = expressionToString(binary.getRight());
            if (left != null && right != null) {
                return left + right;
            }
            if (left != null) return left;
            if (right != null) return right;
        }
        return null;
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
                        .map(JavaAnnotationReader::expressionToString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" | ")))
                .orElse("");
    }

    static int integer(AnnotationExpr annotation, String key, int fallback) {
        return value(annotation, key)
                .filter(com.github.javaparser.ast.expr.IntegerLiteralExpr.class::isInstance)
                .map(com.github.javaparser.ast.expr.IntegerLiteralExpr.class::cast)
                .map(com.github.javaparser.ast.expr.IntegerLiteralExpr::asNumber)
                .map(Number::intValue)
                .orElse(fallback);
    }

    static Optional<AnnotationExpr> nested(AnnotationExpr annotation, String key) {
        return value(annotation, key).filter(AnnotationExpr.class::isInstance).map(AnnotationExpr.class::cast);
    }
}
