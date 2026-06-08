package com.nexoracommerce.common.util;

import lombok.experimental.UtilityClass;

import java.util.List;

// Chuyển đổi embedding vector sang chuỗi PostgreSQL pgvector
@UtilityClass
public class VectorUtils {

    public static String toVectorString(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            sb.append(embedding.get(i));
            if (i < embedding.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
