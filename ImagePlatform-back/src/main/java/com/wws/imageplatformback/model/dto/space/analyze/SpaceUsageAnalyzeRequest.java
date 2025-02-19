package com.wws.imageplatformback.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间资源使用分析
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpaceUsageAnalyzeRequest extends SpaceAnalyzeRequest implements Serializable {

}
