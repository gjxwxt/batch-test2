package com.example.sdk.model;

/**
 * Payload sent to {@code POST /api/v1/license/heartbeat} to report a live
 * instance's current resource usage to the IAS Auth Center.
 */
public record HeartbeatRequest(
        String instanceId,
        String clientUuid,
        String licenseId,
        String proname,
        String productType,
        String productVersion,
        String productSpec,
        String hostname,
        String ipAddress,
        String mac,
        String machineType,
        Integer currentCpus,
        Integer currentMemory,
        String extendedAttributes,
        String signature) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String instanceId;
        private String clientUuid;
        private String licenseId;
        private String proname;
        private String productType;
        private String productVersion;
        private String productSpec;
        private String hostname;
        private String ipAddress;
        private String mac;
        private String machineType;
        private Integer currentCpus;
        private Integer currentMemory;
        private String extendedAttributes;
        private String signature;

        public Builder instanceId(String v) { this.instanceId = v; return this; }
        public Builder clientUuid(String v) { this.clientUuid = v; return this; }
        public Builder licenseId(String v) { this.licenseId = v; return this; }
        public Builder proname(String v) { this.proname = v; return this; }
        public Builder productType(String v) { this.productType = v; return this; }
        public Builder productVersion(String v) { this.productVersion = v; return this; }
        public Builder productSpec(String v) { this.productSpec = v; return this; }
        public Builder hostname(String v) { this.hostname = v; return this; }
        public Builder ipAddress(String v) { this.ipAddress = v; return this; }
        public Builder mac(String v) { this.mac = v; return this; }
        public Builder machineType(String v) { this.machineType = v; return this; }
        public Builder currentCpus(Integer v) { this.currentCpus = v; return this; }
        public Builder currentMemory(Integer v) { this.currentMemory = v; return this; }
        public Builder extendedAttributes(String v) { this.extendedAttributes = v; return this; }
        public Builder signature(String v) { this.signature = v; return this; }

        public HeartbeatRequest build() {
            return new HeartbeatRequest(instanceId, clientUuid, licenseId, proname,
                    productType, productVersion, productSpec, hostname, ipAddress, mac,
                    machineType, currentCpus, currentMemory, extendedAttributes, signature);
        }
    }
}