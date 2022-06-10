package org.eclipse.dataspaceconnector.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Feature Detection object produced by a compliant decentralized Web Node.
 * See: <a href="https://identity.foundation/decentralized-web-node/spec/#feature-detection">Feature detection identity foundation documentation.</a>
 */
@JsonDeserialize(builder = FeatureDetection.Builder.class)
public class FeatureDetection {
    private static final String type = "FeatureDetection";

    private List<WebNodeInterface> interfaces;

    public String getType() {
        return type;
    }

    public List<WebNodeInterface> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    private FeatureDetection() {
        interfaces = new ArrayList<>();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final FeatureDetection featureDetection;

        public Builder() {
            featureDetection = new FeatureDetection();
        }

        public static FeatureDetection.Builder newInstance() {
            return new FeatureDetection.Builder();
        }

        public FeatureDetection.Builder interfaces(List<WebNodeInterface> interfaces) {
            featureDetection.interfaces.addAll(interfaces);
            return this;
        }

        public FeatureDetection build() {
            return featureDetection;
        }

    }
}
