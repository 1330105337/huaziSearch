package com.yupi.springbootinit.model.dto.search;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;

@Data
public class SearchRequest extends PageRequest {


    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 关键词
     */
    private String type;


}
