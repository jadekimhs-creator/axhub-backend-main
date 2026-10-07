package io.shinhanlife.dat.lib.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @package io.shinhanlife.dat.lib.config
 * @className McpProperties
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private String namespace;
    private Manifest manifest = new Manifest();

    @Data
    public static class Manifest {
        private String bundleId;
        private String serviceId;
        private String namePrefix;
        private List<RoutingFunction> routingFunctions = List.of();
        private RoutingFunction routingContract;

        public String getServiceId() {
            return serviceId != null && !serviceId.isBlank() ? serviceId : bundleId;
        }

        public String getBundleId() {
            return bundleId != null && !bundleId.isBlank() ? bundleId : serviceId;
        }

        public List<RoutingFunction> getRoutingFunctions() {
            if (routingFunctions != null && !routingFunctions.isEmpty()) {
                return routingFunctions;
            }
            if (routingContract != null) {
                return List.of(routingContract);
            }
            return List.of();
        }
    }

    @Data
    public static class RoutingFunction {
        private String name;
        private String descriptionSerialization;
        private String serverId;
        private String serviceId;
        private String categoryKey;
        private String productBoundary;
        private List<String> businessDomain = List.of();
        private List<String> businessOutcome = List.of();
        private List<String> primaryEntities = List.of();
        private List<String> capabilities = List.of();
        private List<String> capabilityIndex = List.of();
        private List<String> selectIf = List.of();
        private List<String> rejectIf = List.of();
        private List<String> confusableServers = List.of();
        private String decisionPolicy;

        public String getServerId() {
            return serverId != null && !serverId.isBlank() ? serverId : serviceId;
        }

        public String getServiceId() {
            return serviceId != null && !serviceId.isBlank() ? serviceId : serverId;
        }

        public List<String> getCapabilityIndex() {
            if (capabilityIndex != null && !capabilityIndex.isEmpty()) {
                return capabilityIndex;
            }
            return capabilities != null ? capabilities : List.of();
        }

        public List<String> getCapabilities() {
            return getCapabilityIndex();
        }

        public void setBusinessDomain(Object value) {
            this.businessDomain = coerceToList(value);
        }

        public void setBusinessOutcome(Object value) {
            this.businessOutcome = coerceToList(value);
        }

        public void setSelectIf(Object value) {
            this.selectIf = coerceToList(value);
        }

        public void setRejectIf(Object value) {
            this.rejectIf = coerceToList(value);
        }

        public void setCapabilities(Object value) {
            this.capabilities = coerceToList(value);
            if (this.capabilityIndex.isEmpty()) {
                this.capabilityIndex = this.capabilities;
            }
        }

        public void setCapabilityIndex(Object value) {
            this.capabilityIndex = coerceToList(value);
            if (this.capabilities.isEmpty()) {
                this.capabilities = this.capabilityIndex;
            }
        }

        public void setPrimaryEntities(Object value) {
            this.primaryEntities = coerceToList(value);
        }

        public void setConfusableServers(Object value) {
            this.confusableServers = coerceToList(value);
        }

        private static List<String> coerceToList(Object value) {
            if (value == null) return List.of();
            if (value instanceof Collection<?> coll) {
                List<String> list = new ArrayList<>();
                for (Object item : coll) {
                    if (item != null && !item.toString().isBlank()) {
                        list.add(item.toString().trim());
                    }
                }
                return List.copyOf(list);
            }
            String str = value.toString().trim();
            if (str.isBlank()) return List.of();
            return List.of(str);
        }
    }

}