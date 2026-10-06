package com.example.sdk.model;

/**
 * Payload sent to {@code POST /api/v1/license/heartbeat} to report a live
 * instance's current resource usage to the IAS Auth Center.
 *
 * <p>Fields mirror the server's {@code HeartbeatRequest} DTO.</p>
 */
public record HeartbeatRequest(
        String instanceId,
        String serial,
        Integer currentCpus,
        Integer currentMemory,
        String signature) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String instanceId;
        private String serial;
        private Integer currentCpus;
        private Integer currentMemory;
        private String signature;

        public Builder instanceId(String v) { this.instanceId = v; return this; }
        public Builder serial(String v) { this.serial = v; return this; }
        public Builder currentCpus(Integer v) { this.currentCpus = v; return this; }
        public Builder currentMemory(Integer v) { this.currentMemory = v; return this; }
        public Builder signature(String v) { this.signature = v; return this; }

        public HeartbeatRequest build() {
            return new HeartbeatRequest(instanceId, serial, currentCpus, currentMemory, signature);
        }
    }
}