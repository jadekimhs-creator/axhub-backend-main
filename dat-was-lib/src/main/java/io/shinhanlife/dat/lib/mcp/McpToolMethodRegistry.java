package io.shinhanlife.dat.lib.mcp;

import io.shinhanlife.dat.lib.annotation.GrowToolHint;
import io.shinhanlife.dat.lib.config.McpProperties;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Caches executable {@link McpTool} methods once when a Tool Pod starts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolMethodRegistry {

    private final ApplicationContext applicationContext;
    private final McpProperties mcpProperties;

    private volatile Map<String, RegisteredTool> tools = Map.of();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {

        Map<String, RegisteredTool> discovered = new LinkedHashMap<>();
        for (Object bean : applicationContext.getBeansOfType(Object.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method declaredMethod : targetClass.getDeclaredMethods()) {

                /*
                 * Tool Annotation 검색
                 *
                 * 1순위 : Interface
                 * 2순위 : 구현체
                 */
                ToolAnnotationMetadata metadata = findToolAnnotationMetadata(targetClass, declaredMethod);

                if (metadata == null) {
                    continue;
                }

                McpTool annotation = metadata.mcpTool();
                GrowToolHint hint = metadata.growToolHint();

                /*
                 * Annotation은 Interface에서 가져올 수 있지만
                 * 실제 호출 Method는 구현체 Method를 사용한다.
                 */
                RegisteredTool tool = new RegisteredTool(bean, findInvocableMethod(bean, declaredMethod), annotation, hint);

                /*
                 * 원래 Tool Name 등록
                 */
                register(discovered, annotation.name(), tool);

                /*
                 * namespace alias 등록
                 */
                registerNamespaceAlias(discovered, annotation.name(), tool);

                log.info("[Tool Registry] Registered Tool: {} -> {}#{}", annotation.name(), targetClass.getSimpleName(), declaredMethod.getName());
            }
        }
        tools = Map.copyOf(discovered);
        log.info("[Tool Registry] {} executable tool names cached", tools.size());
    }

    public RegisteredTool find(String toolName) {
        return tools.get(toolName);
    }

    /**
     * @McpTool / @GrowToolHint 검색
     * <p>
     * 우선순위
     * <p>
     * 1. Interface Method
     * 2. 구현체 Method
     */
    private ToolAnnotationMetadata findToolAnnotationMetadata(Class<?> targetClass, Method declaredMethod) {

        /*
         * ==========================================
         * STEP 1.
         * Interface부터 검색
         * ==========================================
         */
        for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClassAsSet(targetClass)) {

            /*
             * 구현체 Method와 동일한 Signature를 가진
             * Interface Method를 찾는다.
             */
            Method interfaceMethod = ReflectionUtils.findMethod(interfaceClass, declaredMethod.getName(), declaredMethod.getParameterTypes());

            if (interfaceMethod == null) {
                continue;
            }

            /*
             * Interface의 @McpTool 검색
             */
            McpTool mcpTool = AnnotationUtils.findAnnotation(interfaceMethod, McpTool.class);
            if (mcpTool == null) {
                continue;
            }

            /*
             * @McpTool을 Interface에서 발견했다면
             * @GrowToolHint 역시 같은 Interface Method에서 가져온다.
             */
            GrowToolHint growToolHint = AnnotationUtils.findAnnotation(interfaceMethod, GrowToolHint.class);
            log.debug("[Tool Registry] Interface @McpTool found: " + "{}#{} -> {}", interfaceClass.getSimpleName(), interfaceMethod.getName(), mcpTool.name());
            return new ToolAnnotationMetadata(mcpTool, growToolHint);
        }

        /*
         * ==========================================
         * STEP 2.
         * Interface에서 발견되지 않은 경우
         * 구현체 Method 검색
         * ==========================================
         */
        McpTool mcpTool = AnnotationUtils.findAnnotation(declaredMethod, McpTool.class);

        if (mcpTool == null) {
            return null;
        }

        GrowToolHint growToolHint = AnnotationUtils.findAnnotation(declaredMethod, GrowToolHint.class);
        log.debug("[Tool Registry] Implementation @McpTool found: " + "{}#{} -> {}", targetClass.getSimpleName(), declaredMethod.getName(), mcpTool.name());
        return new ToolAnnotationMetadata(mcpTool, growToolHint);
    }

    /**
     * Namespace가 존재하는 경우
     * <p>
     * ex)
     * <p>
     * pct_notice_list
     * <p>
     * namespace = sys
     * <p>
     * sys_pct_notice_list
     * <p>
     * 두 이름으로 동일 Tool을 조회할 수 있도록 등록
     */
    private void registerNamespaceAlias(Map<String, RegisteredTool> discovered, String toolName, RegisteredTool tool) {

        String namespace = mcpProperties.getNamespace();

        if (StringUtils.hasText(namespace)) {

            register(discovered, namespace + "_" + toolName, tool);
        }
    }

    /**
     * Tool Registry 등록
     */
    private void register(Map<String, RegisteredTool> discovered, String toolName, RegisteredTool tool) {

        RegisteredTool existing = discovered.putIfAbsent(toolName, tool);

        if (existing != null && existing != tool) {

            throw new IllegalStateException("Duplicate @McpTool name: " + toolName);
        }
    }

    /**
     * 실제 호출 가능한 Method 확보
     * <p>
     * Annotation 검색은 Interface에서 하더라도
     * Tool 실행은 구현체 Bean을 대상으로 수행해야 한다.
     */
    private Method findInvocableMethod(Object bean, Method declaredMethod) {

        try {
            return bean.getClass().getMethod(declaredMethod.getName(), declaredMethod.getParameterTypes());
        } catch (NoSuchMethodException ignored) {
            return declaredMethod;
        }
    }

    /**
     * Annotation 검색 결과
     */
    private record ToolAnnotationMetadata(McpTool mcpTool, GrowToolHint growToolHint) {
    }

    /**
     * 실제 실행 Registry 정보
     */
    public record RegisteredTool(Object bean, Method method, McpTool annotation, GrowToolHint hint) {
    }
}