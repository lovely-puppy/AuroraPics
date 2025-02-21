package com.wws.imageplatformback.api.deepseek.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekResultRequest implements Serializable {

    private String searchText;  //搜索的内容

    private static final long serialVersionUID = 1L;
}
