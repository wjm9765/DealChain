package com.dealchain.dealchain.domain.DealTracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealTrackingRequest {
    private String roomId;
    //userId는 토큰에서 가져옴
    private String role;
    private String deviceInfo;
}
